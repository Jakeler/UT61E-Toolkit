package jk.ut61eTool

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.github.mikephil.charting.charts.BarChart
import com.jake.UT61e_decoder
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.File
import java.util.*


class ViewLogActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_log)

        var graphUI = GraphUI(this, findViewById<BarChart>(R.id.view_graph), findViewById(R.id.view_dataInfo), R.color.logPrimary)
        graphUI.viewSize = Int.MAX_VALUE

        val file = intent.extras["filename"] as File

        Log.d("FILE", file.length().toString())



        var csvFormat = CSVFormat.RFC4180.withDelimiter(';').withCommentMarker('#')
        val parser = CSVParser.parse(file, charset("UTF-8"), csvFormat)

        val record_list = parser.records
        val time_list = record_list.filter { it.hasComment() }

        val time_strings = ArrayList<String>(time_list.size)
        for (csvRecord in time_list) {
            time_strings.add(csvRecord.comment)
        }


        val spinner = findViewById<Spinner>(R.id.log_spinner)
        spinner.adapter = ArrayAdapter<String>(this, R.layout.spinneritem_time, R.id.time_textView, time_strings)

        fun loadGraph() {
            graphUI.graph.barData.removeDataSet(0)
            graphUI.newDataSet()

            var start_pos = time_list[spinner.selectedItemPosition].recordNumber.toInt()
            for (pos in start_pos until record_list.size) {
                val csvRecord = record_list[pos]
                if (csvRecord.hasComment()) break
                var data = UT61e_decoder()
                data.value = csvRecord[0].toDoubleOrNull()?: continue
                data.unit_str = csvRecord[1]
                graphUI.displayData(data)
            }
            graphUI.updateDataInfo()
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Log.d("SELECTED", time_list[position].comment)
                loadGraph()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }


    }
}
