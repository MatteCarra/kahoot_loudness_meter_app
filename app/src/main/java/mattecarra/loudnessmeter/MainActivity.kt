package mattecarra.loudnessmeter

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import mattecarra.loudnessmeter.protocol.LoudnessClient

import java.lang.reflect.Field;
import java.lang.reflect.Method;

class MainActivity : AppCompatActivity(), OnChartGestureListener {
    private val VISIBLE_RANGE = 600f

    private lateinit var chart: LineChart
    private var loudnessClient: LoudnessClient? = null
    private var dialog: MaterialDialog? = null
    private var scroll = true
    private lateinit var fab: FloatingActionButton
    private var ip: String? = null

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
                    showReconnectionWindow()
                }
            }
        }
    }

    private fun initCharts() {
        chart = findViewById(R.id.chart1)
        chart.setDrawGridBackground(false)
        chart.description.isEnabled = false
        chart.setNoDataText("No chart data available yet")
        chart.isAutoScaleMinMaxEnabled = true
        chart.onChartGestureListener = this
    }

    private fun createSet(): LineDataSet {
        val set = LineDataSet(null, "Loudness meter")
        set.lineWidth = 2.5f
        set.setDrawCircles(false)
        set.setDrawValues(false)
        set.color = Color.rgb(240, 99, 99)
        set.setCircleColor(Color.rgb(240, 99, 99))
        set.highLightColor = Color.rgb(190, 190, 190)
        set.axisDependency = YAxis.AxisDependency.LEFT
        set.valueTextSize = 8f
        return set
    }

    fun scrollToEnd(data: LineData) {
        val touchListener = chart.onTouchListener
        try {
            val method = touchListener.javaClass.getDeclaredMethod("stopDeceleration")
            method.invoke(touchListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        chart.moveViewToX(data.entryCount - VISIBLE_RANGE - 1)
    }

    private fun onNewEntry(value: Int) {
        val data =
            chart.data ?: {
                val newData = LineData()
                chart.data = newData
                newData.addDataSet(createSet())
                newData
            }()

        val set = data.getDataSetByIndex(0)
        data.addEntry(Entry(set.entryCount.toFloat(), value.toFloat()), 0)
        data.notifyDataChanged()

        chart.notifyDataSetChanged()

        chart.setVisibleXRangeMaximum(VISIBLE_RANGE) //~ 1 minute

        if(scroll)
            scrollToEnd(data)
    }

    private fun showReconnectionWindow() {
        if(ip == null) {
            showConnectionWindow()
        } else {
            dialog = MaterialDialog(this).show {
                title(R.string.disconnected)
                message(R.string.reconnect_dialog_message)
                cancelOnTouchOutside(false)
                positiveButton(R.string.reconnect) {
                    initCharts()
                    loudnessClient = LoudnessClient(ip.toString(), handler)
                    loudnessClient?.connect()
                }
                negativeButton {
                    finish()
                }
            }

            dialog?.setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    finish()
                }
                false
            }
        }
    }

    private fun showConnectionWindow() {
        dialog = MaterialDialog(this).show {
            title(R.string.server_ip)
            input { _, ip ->
                this@MainActivity.ip = ip.toString()

                initCharts()
                loudnessClient = LoudnessClient(ip.toString(), handler)
                loudnessClient?.connect()
            }
            cancelOnTouchOutside(false)
            positiveButton(R.string.connect)
            negativeButton {
                finish()
            }
        }

        dialog?.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                finish()
            }
            false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        showConnectionWindow()

        fab = findViewById(R.id.scroll_fab)
        fab.hide()
        fab.setOnClickListener {
            scrollToEnd(chart.data)
            scroll = true
            fab.hide()
        }
    }

    override fun onDestroy() {
        dialog?.let {
            if(it.isShowing)
                it.dismiss()
        }

        if(loudnessClient?.connected == true)
            loudnessClient?.stop()

        super.onDestroy()
    }

    override fun onChartGestureEnd(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {
        if(!scroll) {
            val entryCount = chart.data.getDataSetByIndex(0).entryCount
            val highestVisibleX = chart.highestVisibleX
            if(entryCount - highestVisibleX < 10) {
                scrollToEnd(chart.data)
                scroll = true
                fab.hide()
            }
        }
    }

    override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {
        scroll = false
        fab.show()
    }

    override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, velocityX: Float, velocityY: Float) {}

    override fun onChartSingleTapped(me: MotionEvent?) {}

    override fun onChartGestureStart(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}

    override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {}

    override fun onChartLongPressed(me: MotionEvent?) {}

    override fun onChartDoubleTapped(me: MotionEvent?) {}
}
