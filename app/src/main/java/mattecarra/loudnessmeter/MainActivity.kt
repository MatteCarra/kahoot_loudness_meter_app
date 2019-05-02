package mattecarra.loudnessmeter

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

class MainActivity : AppCompatActivity() {
    private lateinit var chart: LineChart
    private var loudnessDataSocket: LoudnessDataSocket? = null
    private var dialog: MaterialDialog? = null

    companion object {
        const val DATA_PACKET = 0
        const val DISCONNECT_PACKET = 1
    }

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when(msg.what) {
                DATA_PACKET -> {
                    onNewEntry(msg.obj as Int)
                }
                DISCONNECT_PACKET -> {
                    chart.clear()
                    showDialog()
                }
            }
        }
    }

    private fun showDialog() {
        dialog = MaterialDialog(this).show {
            title(R.string.server_ip)
            input { _, ip ->
                initCharts()
                loudnessDataSocket = LoudnessDataSocket(ip.toString(), handler).connect()
            }
            cancelOnTouchOutside(false)
            positiveButton(R.string.connect)
        }

        dialog?.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                finish()
            }
            false
        }
    }

    private fun initCharts() {
        chart = findViewById(R.id.chart1);
        chart.setDrawGridBackground(false);
        chart.description.isEnabled = false;
        chart.setNoDataText("No chart data available yet");
        chart.isAutoScaleMinMaxEnabled = true
    }

    private fun createSet(): LineDataSet {
        val set = LineDataSet(null, "Loudness meter");
        set.lineWidth = 2.5f;
        set.setDrawCircles(false)
        set.color = Color.rgb(240, 99, 99);
        set.setCircleColor(Color.rgb(240, 99, 99));
        set.highLightColor = Color.rgb(190, 190, 190);
        set.axisDependency = YAxis.AxisDependency.LEFT;
        set.valueTextSize = 8f;
        return set;
    }

    private fun onNewEntry(value: Int) {
        val data =
            chart.data ?: {
                val newData = LineData();
                chart.data = newData;
                newData.addDataSet(createSet())
                newData
            }()

        val set = data.getDataSetByIndex(0);
        data.addEntry(Entry(set.entryCount.toFloat(), value.toFloat()), 0);
        data.notifyDataChanged();

        chart.notifyDataSetChanged();

        chart.setVisibleXRangeMaximum(600f) //~ 1 minute

        chart.moveViewTo(data.entryCount - 51f, (chart.yMax - chart.yMin)/2, YAxis.AxisDependency.LEFT);
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        showDialog()
    }

    override fun onDestroy() {
        dialog?.let {
            if(it.isShowing)
                it.dismiss()
        }

        super.onDestroy()
    }
}
