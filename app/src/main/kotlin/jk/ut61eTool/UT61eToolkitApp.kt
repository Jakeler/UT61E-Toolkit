package jk.ut61eTool

import android.app.Application
import android.content.Context
import jk.ut61eTool.data.CsvLogger
import jk.ut61eTool.data.LogFileRepository
import jk.ut61eTool.data.MeterRepository
import jk.ut61eTool.data.SettingsRepository
import jk.ut61eTool.domain.AlarmEvaluator
import jk.ut61eTool.domain.ColdJunctionProvider
import jk.ut61eTool.domain.Converter
import jk.ut61eTool.domain.ThermalZoneReader
import jk.ut61eTool.service.MeterServiceCommander

/** Manual dependency container — the app is small enough to not need a DI framework. */
class AppContainer(context: Context) {
    val settingsRepository = SettingsRepository.create(context)
    val csvLogger = CsvLogger(context)
    val logFileRepository = LogFileRepository(context)
    val thermalZoneReader = ThermalZoneReader()
    val converter = Converter(ColdJunctionProvider(thermalZoneReader::getTempById))
    val alarmEvaluator = AlarmEvaluator()
    val meterRepository = MeterRepository().apply {
        commander = MeterServiceCommander(context)
    }
}

class UT61eToolkitApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as UT61eToolkitApp).container
