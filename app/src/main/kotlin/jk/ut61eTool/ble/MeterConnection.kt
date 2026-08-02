package jk.ut61eTool.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

/** What the GATT link reports while a meter connection is alive. */
sealed interface MeterConnectionEvent {
    /** connectGatt has been issued. */
    data object Connecting : MeterConnectionEvent

    /** GATT link is up; service discovery is running. */
    data object Connected : MeterConnectionEvent

    /** Notifications on the serial characteristic are enabled; data will follow. */
    data object ServicesReady : MeterConnectionEvent

    /** One notification payload, parity bits still intact. */
    class Data(val bytes: ByteArray) : MeterConnectionEvent

    /** Terminal event; the flow completes right after it. */
    data class Disconnected(val reason: String? = null) : MeterConnectionEvent
}

/**
 * Modern replacement for the 2013-era `BluetoothLeService`: one cold flow per
 * connection attempt, cancelled by cancelling the collection.
 *
 * The caller guarantees BLUETOOTH_CONNECT (API 31+).
 */
class MeterConnection(context: Context) {

    private val appContext = context.applicationContext

    @SuppressLint("MissingPermission")
    fun connect(address: String, characteristicUuid: UUID): Flow<MeterConnectionEvent> =
        callbackFlow {
            val adapter = appContext.getSystemService(BluetoothManager::class.java)?.adapter
            if (adapter == null || !adapter.isEnabled) {
                close(IllegalStateException("Bluetooth is not available or not enabled"))
                return@callbackFlow
            }

            val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull()
            if (device == null) {
                close(IllegalArgumentException("Invalid device address $address"))
                return@callbackFlow
            }

            val callback = object : BluetoothGattCallback() {

                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            trySend(MeterConnectionEvent.Connected)
                            gatt.discoverServices()
                        }

                        BluetoothProfile.STATE_DISCONNECTED -> {
                            trySend(MeterConnectionEvent.Disconnected(statusReason(status)))
                            close()
                        }
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        trySend(MeterConnectionEvent.Disconnected("Service discovery failed ($status)"))
                        close()
                        return
                    }
                    val characteristic = findCharacteristic(gatt, characteristicUuid)
                    if (characteristic == null) {
                        trySend(
                            MeterConnectionEvent.Disconnected(
                                "Characteristic $characteristicUuid not found",
                            ),
                        )
                        close()
                        return
                    }
                    if (enableNotifications(gatt, characteristic)) {
                        trySend(MeterConnectionEvent.ServicesReady)
                    } else {
                        trySend(MeterConnectionEvent.Disconnected("Could not enable notifications"))
                        close()
                    }
                }

                // API 33+ delivers the payload directly. The platform base class routes the
                // new callback to the deprecated one by default; overriding both without
                // calling super means each notification is reported exactly once.
                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    value: ByteArray,
                ) {
                    trySend(MeterConnectionEvent.Data(value))
                }

                @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
                    val value = characteristic.value ?: return
                    trySend(MeterConnectionEvent.Data(value))
                }
            }

            trySend(MeterConnectionEvent.Connecting)
            val gatt = device.connectGatt(
                appContext,
                /* autoConnect = */ false,
                callback,
                BluetoothDevice.TRANSPORT_LE,
            )
            if (gatt == null) {
                close(IllegalStateException("connectGatt returned null"))
                return@callbackFlow
            }

            awaitClose {
                runCatching {
                    gatt.disconnect()
                    gatt.close()
                }
            }
        }

    /**
     * The HM-11 exposes its serial characteristic (0000ffe1-…) inside a vendor
     * service, and the UUID is user-configurable, so every service is searched
     * instead of hard-coding a service UUID — same as the legacy app did.
     */
    private fun findCharacteristic(gatt: BluetoothGatt, uuid: UUID): BluetoothGattCharacteristic? =
        gatt.services?.firstNotNullOfOrNull { it.getCharacteristic(uuid) }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
    ): Boolean {
        if (!gatt.setCharacteristicNotification(characteristic, true)) return false

        // Local notification routing is not enough: the CCCD has to be written
        // for the peer to actually start sending.
        val descriptor = characteristic.getDescriptor(CCCD_UUID)
        if (descriptor == null) {
            Log.w(TAG, "Characteristic ${characteristic.uuid} has no CCCD")
            return false
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ==
                BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            run {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
        }
    }

    private fun statusReason(status: Int): String? =
        if (status == BluetoothGatt.GATT_SUCCESS) null else "GATT status $status"

    private companion object {
        const val TAG = "MeterConnection"

        /** Client Characteristic Configuration Descriptor, the standard notification switch. */
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
