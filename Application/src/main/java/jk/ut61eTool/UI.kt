package jk.ut61eTool

import android.app.Activity
import android.view.View
import android.widget.TextView
import com.jake.UT61e_decoder

class UI(a: Activity) {
    var mDataField: TextView
    var neg: TextView
    var ol: TextView
    var acdc: TextView
    var freqDuty: TextView


    fun update(ut61e: UT61e_decoder) {
        mDataField.text = ut61e.toString()
        enableTextView(neg, ut61e.getValue() < 0)
        enableTextView(ol, ut61e.isOL)
        if (ut61e.isFreq || ut61e.isDuty) {
            enableTextView(freqDuty, true)
            enableTextView(acdc, false)
            if (ut61e.isDuty) freqDuty.text = "Duty" else if (ut61e.isFreq) freqDuty.text = "Freq."
        } else {
            enableTextView(freqDuty, false)
            enableTextView(acdc, true)
            if (ut61e.isDC) {
                acdc.text = "DC"
            } else if (ut61e.isAC) {
                acdc.text = "AC"
            } else {
                enableTextView(acdc, false)
            }
        }
    }

    private fun enableTextView(v: View, enabled: Boolean) {
        if (enabled) {
            v.alpha = 1.0f
        } else {
            v.alpha = 0.2f
        }
    }

    init {
        mDataField = a.findViewById(R.id.data_value)
        neg = a.findViewById(R.id.Neg)
        ol = a.findViewById(R.id.OL)
        acdc = a.findViewById(R.id.ACDC)
        freqDuty = a.findViewById(R.id.FreqDuty)
    }
}