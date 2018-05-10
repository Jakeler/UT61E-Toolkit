package jk.ut61eTool

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.github.mikephil.charting.charts.BarChart
import com.jake.UT61e_decoder
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.File


class ViewLogActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_log)

        var graphUI = GraphUI(this, findViewById<BarChart>(R.id.view_graph), findViewById(R.id.view_dataInfo))
        graphUI.viewSize = Int.MAX_VALUE

        val file = intent.extras["filename"] as File

        Log.d("FILE", file.length().toString())

        var csvFormat = CSVFormat.RFC4180.withDelimiter(';').withCommentMarker('#')
        val parser = CSVParser.parse(file, charset("UTF-8"), csvFormat)
        parser.recordNumber
        for (csvRecord in parser) {
            if (csvRecord.hasComment()) continue
            var data = UT61e_decoder()
            data.value = csvRecord[0].toDoubleOrNull()?: continue
            data.unit_str = csvRecord[1]
            graphUI.displayData(data)
        }



    }
}
