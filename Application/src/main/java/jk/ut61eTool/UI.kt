package jk.ut61eTool

import android.view.View
import com.jake.UT61e_decoder
import jk.ut61eTool.databinding.LogActivityBinding

class UI(val binding: LogActivityBinding) {

    fun update(ut61e: UT61e_decoder) {
        binding.dataValue.text = ut61e.toString()
        enableTextView(binding.Neg, ut61e.getValue() < 0)
        enableTextView(binding.OL, ut61e.isOL)

        if (ut61e.isFreq || ut61e.isDuty) {
            enableTextView(binding.FreqDuty, true)
            enableTextView(binding.ACDC, false)
            binding.FreqDuty.text = when {
                ut61e.isDuty -> "Duty"
                ut61e.isFreq -> "Freq."
                else -> ""
            }
        } else {
            enableTextView(binding.FreqDuty, false)
            enableTextView(binding.ACDC, true)

            binding.ACDC.text = when {
                ut61e.isDC -> "DC"
                ut61e.isAC -> "AC"
                else -> {
                    enableTextView(binding.ACDC, false)
                    ""
                }
            }
        }
    }

    private fun enableTextView(v: View, enabled: Boolean) {
        v.alpha = if (enabled) 1.0f else 0.2f
    }
}