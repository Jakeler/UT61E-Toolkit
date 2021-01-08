package jk.ut61eTool;

import android.app.Activity;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

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
    Activity activity;

    TextView dataInfo;
    BarChart graph;

    int points, viewSize, color;

    public GraphUI(Activity a, BarChart pGraph, TextView pDataInfo, int pcolor) {

        activity = a;

        //dataInfo = (TextView) a.findViewById(R.id.dataInfo);
        dataInfo = pDataInfo;
        //graph = (BarChart) a.findViewById(R.id.graph);
        graph = pGraph;

        this.color = pcolor;

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

        newDataSet();
    }

    public void newDataSet() {
        List<BarEntry> list = new ArrayList<>();
        list.add(new BarEntry(0,0, ""));
        BarDataSet dataSet = new BarDataSet(list, "values");
        dataSet.setDrawValues(false);
        dataSet.setColor(ContextCompat.getColor(activity, color));
        BarData data = new BarData(dataSet);
        graph.setData(data);
        graph.setOnChartGestureListener(this);

        points = 1;
    }

    public void displayData(UT61e_decoder ut61e) {

        graph.getBarData().getDataSetByIndex(0).addEntry(new BarEntry(points, (float) ut61e.getValue(), ut61e.unit_str));
        while (graph.getBarData().getDataSetByIndex(0).getEntryCount() > viewSize) {
            graph.getBarData().getDataSetByIndex(0).removeFirst();
        }
        graph.getBarData().notifyDataChanged();
        graph.notifyDataSetChanged();
        graph.invalidate();

        points++;
    }

    public void updateDataInfo() {
        int lowX = (int)(graph.getLowestVisibleX()+0.5);
        int highX = (int)(graph.getHighestVisibleX()+0.5);
        List<BarEntry> viewData = new ArrayList<>();

        for (int i = lowX; i < highX; i++) {
            viewData.add(graph.getBarData().getDataSetByIndex(0).getEntriesForXValue(i).get(0));
        }
        if (viewData.size() == 0) return;

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

        dataInfo.setText(activity.getString(R.string.graphdata_info, max, min, avg, stdDev));
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
