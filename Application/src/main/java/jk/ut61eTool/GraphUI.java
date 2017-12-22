package jk.ut61eTool;

import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.jake.UT61e_decoder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jan on 23.12.17.
 */

public class GraphUI implements OnChartGestureListener {
    LogActivity activity;

    TextView mDataField, dataInfo;
    TextView neg, ol, acdc, freqDuty;
    BarChart graph;
    int points = 1, viewSize;

    public GraphUI(LogActivity a) {

        activity = a;

        mDataField = (TextView) a.findViewById(R.id.data_value);
        dataInfo = (TextView) a.findViewById(R.id.dataInfo);

        neg = (TextView) a.findViewById(R.id.Neg);
        ol = (TextView) a.findViewById(R.id.OL);
        acdc = (TextView) a.findViewById(R.id.ACDC);
        freqDuty = (TextView) a.findViewById(R.id.FreqDuty);

        graph = (BarChart) a.findViewById(R.id.graph);
        setupGraph();
    }

    private void setupGraph() {
        // enable description text
        graph.getDescription().setEnabled(false);

        // enable touch gestures
        graph.setTouchEnabled(true);

        // enable scaling and dragging
        graph.setDragEnabled(true);
        graph.setScaleEnabled(true);
        graph.setDrawGridBackground(false);

        // if disabled, scaling can be done on x- and y-axis separately
        graph.setPinchZoom(false);

        graph.getAxisLeft().setEnabled(false);

        graph.getLegend().setEnabled(false);

        ValueMarker marker = new ValueMarker(activity);
        marker.setChartView(graph);
        graph.setMarker(marker);

        List<BarEntry> list = new ArrayList<>();
        list.add(new BarEntry(0,0, ""));
        BarDataSet dataSet = new BarDataSet(list, "values");
        dataSet.setDrawValues(false);
        dataSet.setColor(Color.BLUE);
        BarData data = new BarData(dataSet);
        graph.setData(data);
        graph.setOnChartGestureListener(this);
    }

    public void displayData(UT61e_decoder ut61e) {
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

        graph.getBarData().getDataSetByIndex(0).addEntry(new BarEntry(points, (float) ut61e.getValue(), ut61e.unit_str));
        while (graph.getBarData().getDataSetByIndex(0).getEntryCount() > viewSize) {
            graph.getBarData().getDataSetByIndex(0).removeFirst();
        }
        graph.getBarData().notifyDataChanged();
        graph.notifyDataSetChanged();
        graph.invalidate();

        updateDataInfo();
        points++;
    }

    private void updateDataInfo() {
        int lowX = (int)(graph.getLowestVisibleX()+0.5);
        int highX = (int)(graph.getHighestVisibleX()+0.5);
        List<BarEntry> viewData = new ArrayList<>();
        for (int i = lowX; i < highX; i++) {
            viewData.add(graph.getBarData().getDataSetByIndex(0).getEntriesForXValue(i).get(0));
        }

        double sum = 0, min = viewData.get(0).getY(), max = min;
        for (BarEntry e : viewData) {
            float value = e.getY();
            sum += value;
            min = value < min? value : min;
            max = value > max? value : max;
        }
        double avg = sum / viewData.size();

        sum = 0;
        for (BarEntry e : viewData) {
            sum += Math.abs(e.getY() - avg);
        }
        double stdDev = sum / viewData.size();

        dataInfo.setText("Max: " + double2String(max) + " | Min: " + double2String(min)
                + " | Avg: " + double2String(avg) + " | Std.dev: " + double2String(stdDev));
    }

    private String double2String(double d) {
        return String.format("%5.3f", d);
    }

    private void enableTextView(View v, boolean enabled) {
        if (enabled) {
            v.setAlpha(1.0f);
        } else {
            v.setAlpha(0.2f);
        }
    }

    @Override
    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
        updateDataInfo();
    }

    @Override
    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {}

    @Override
    public void onChartLongPressed(MotionEvent me) {}

    @Override
    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {}

    @Override
    public void onChartDoubleTapped(MotionEvent me) {}

    @Override
    public void onChartSingleTapped(MotionEvent me) {}

    @Override
    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {}

    @Override
    public void onChartTranslate(MotionEvent me, float dX, float dY){}
}
