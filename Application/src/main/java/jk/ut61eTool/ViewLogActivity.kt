package jk.ut61eTool

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.documentfile.provider.DocumentFile
import com.jake.UT61e_decoder
import jk.ut61eTool.databinding.ActivityViewLogBinding
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.util.*


class ViewLogActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bndg: ActivityViewLogBinding =
                DataBindingUtil.setContentView(this, R.layout.activity_view_log)

        val graphUI = GraphUI(this, bndg.viewGraph, bndg.viewDataInfo, R.color.logPrimary)
        graphUI.viewSize = Int.MAX_VALUE

        val fileUri = intent.extras?.get("filename") as Uri
        val file = DocumentFile.fromSingleUri(this, fileUri) ?: return
        val stream = contentResolver.openInputStream(fileUri)


        Log.d("FILE", file.length().toString())

        actionBar?.title = getString(R.string.logview_actionBar_title, file.name)

        val csvFormat = CSVFormat.RFC4180.withDelimiter(';').withCommentMarker('#')
        val parser = CSVParser.parse(stream, charset("UTF-8"), csvFormat)

        val record_list = parser.records
        val time_list = record_list.filter { it.hasComment() }

        val time_strings = ArrayList<String>(time_list.size)
        for (csvRecord in time_list) {
            time_strings.add(csvRecord.comment)
        }
        if (time_strings.size < 1) {
            Toast.makeText(this, R.string.parse_error_toast, Toast.LENGTH_SHORT).show()
            finish()
        }


        bndg.logSpinner.adapter = ArrayAdapter<String>(this, R.layout.spinneritem_time, R.id.time_textView, time_strings)

        fun loadGraph() {
            graphUI.graph.barData.removeDataSet(0)
            graphUI.newDataSet()

            val start_pos = time_list[bndg.logSpinner.selectedItemPosition].recordNumber.toInt()
            for (pos in start_pos until record_list.size) {
                val csvRecord = record_list[pos]
                if (csvRecord.hasComment()) break
                val data = UT61e_decoder()
                data.value = csvRecord[0].toDoubleOrNull()?: continue
                data.unit_str = csvRecord[1]
                graphUI.displayData(data)
            }
            graphUI.updateDataInfo()
        }

        bndg.logSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
