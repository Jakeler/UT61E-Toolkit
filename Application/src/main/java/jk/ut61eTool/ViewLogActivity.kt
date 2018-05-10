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

        var graphUI = GraphUI(this, findViewById<BarChart>(R.id.view_graph), findViewById(R.id.view_dataInfo))
        graphUI.viewSize = Int.MAX_VALUE

        val file = intent.extras["filename"] as File

        Log.d("FILE", file.length().toString())

        var list = ArrayList<String>(10)

        var csvFormat = CSVFormat.RFC4180.withDelimiter(';').withCommentMarker('#')
        val parser = CSVParser.parse(file, charset("UTF-8"), csvFormat)
        for (csvRecord in parser) {
            if (csvRecord.hasComment()) {
                list.add(csvRecord.comment)
                continue
            }
            var data = UT61e_decoder()
            data.value = csvRecord[0].toDoubleOrNull()?: continue
            data.unit_str = csvRecord[1]
            graphUI.displayData(data)
        }

        val spinner = findViewById<Spinner>(R.id.log_spinner)
        spinner.adapter = ArrayAdapter<String>(this, R.layout.spinneritem_time, R.id.time_textView, list)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Log.d("SELECTED", list[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }


    }
}
