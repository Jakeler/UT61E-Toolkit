package jk.ut61eTool

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Utils

/**
 * Created by jan on 21.10.17.
 */
class ValueMarker(context: Context?) : MarkerView(context, R.layout.value_marker) {
    private val tvContent = findViewById<TextView>(R.id.tvContent)

    // callbacks everytime the MarkerView is redrawn, can be used to update the
    // content (user-interface)
    override fun refreshContent(e: Entry, highlight: Highlight) {
        tvContent.text = "${Utils.formatNumber(e.y, 4, true)} ${e.data}"
        Log.d("ValueMarker", "refreshContent: $e")
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF((-(width / 2)).toFloat(), (-height).toFloat())
    }
}