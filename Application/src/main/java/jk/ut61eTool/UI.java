package jk.ut61eTool;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.jake.UT61e_decoder;

public class UI {

    TextView mDataField;
    TextView neg, ol, acdc, freqDuty;

    public UI(Activity a) {
        mDataField = a.findViewById(R.id.data_value);
        neg = a.findViewById(R.id.Neg);
        ol = a.findViewById(R.id.OL);
        acdc = a.findViewById(R.id.ACDC);
        freqDuty = a.findViewById(R.id.FreqDuty);
    }

    public void update(UT61e_decoder ut61e) {
        mDataField.setText(ut61e.toString());

        enableTextView(neg, ut61e.getValue() < 0);
        enableTextView(ol, ut61e.isOL());
        if (ut61e.isFreq() || ut61e.isDuty()) {
            enableTextView(freqDuty, true);
            enableTextView(acdc, false);
            if (ut61e.isDuty()) freqDuty.setText("Duty");
            else if (ut61e.isFreq()) freqDuty.setText("Freq.");
        } else {
            enableTextView(freqDuty, false);
            enableTextView(acdc, true);
            if (ut61e.isDC()) {
                acdc.setText("DC");
            } else if (ut61e.isAC()) {
                acdc.setText("AC");
            } else {
                enableTextView(acdc, false);
            }
        }
    }

    private void enableTextView(View v, boolean enabled) {
        if (enabled) {
            v.setAlpha(1.0f);
        } else {
            v.setAlpha(0.2f);
        }
    }
}
