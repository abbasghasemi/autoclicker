package ghasemi.abbas.autoclicker.ui.components

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import androidx.core.util.Consumer
import ghasemi.abbas.autoclicker.R
import ghasemi.abbas.autoclicker.utils.AndroidUtils
import java.util.Locale

class DurationTimeView(context: Context) : LinearLayout(context) {
    lateinit var consumer: Consumer<Long>
    private val hours = createPicker(context, 23)
    private val minutes = createPicker(context, 59)
    private val seconds = createPicker(context, 59)
    private val summary = TextView(context)
    private var updating = false

    init {
        orientation = VERTICAL
        layoutDirection = LAYOUT_DIRECTION_LTR

        val labels = LinearLayout(context)
        addView(labels, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        listOf("h", "m", "s").forEach { label ->
            labels.addView(TextView(context).apply {
                text = label
                gravity = Gravity.CENTER
                textSize = 13f
                setTextColor(Color.BLACK)
            }, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }

        val pickers = LinearLayout(context)
        addView(pickers, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtils.dp(150f)))
        listOf(hours, minutes, seconds).forEach { picker ->
            pickers.addView(picker, LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
            picker.setOnValueChangedListener { _, _, _ -> updateSelection() }
        }

        addView(summary.apply {
            gravity = Gravity.CENTER
            textSize = 16f
            setTextColor(Color.BLACK)
        }, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtils.dp(30f)))
    }

    private fun createPicker(context: Context, max: Int) = NumberPicker(context).apply {
        minValue = 0
        maxValue = max
        wrapSelectorWheel = true
        descendantFocusability = FOCUS_BLOCK_DESCENDANTS
        setFormatter { String.format(Locale.US, "%02d", it) }
        setOnTouchListener { view, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                view.parent?.requestDisallowInterceptTouchEvent(true)
            }
            false
        }
    }

    fun updateDurationTime(durationTime: Long) {
        updating = true
        val safeDuration = durationTime.coerceIn(0L, 23L * 3600 + 59 * 60 + 59)
        hours.value = (safeDuration / 3600).toInt()
        minutes.value = ((safeDuration % 3600) / 60).toInt()
        seconds.value = (safeDuration % 60).toInt()
        updating = false
        updateSelection()
    }

    private fun updateSelection() {
        if (updating) return
        val duration = hours.value * 3600L + minutes.value * 60L + seconds.value
        summary.text = if (duration == 0L) context.getString(R.string.infinity)
        else String.format(Locale.US, "%02d:%02d:%02d", hours.value, minutes.value, seconds.value)
        if (::consumer.isInitialized) consumer.accept(duration)
    }
}
