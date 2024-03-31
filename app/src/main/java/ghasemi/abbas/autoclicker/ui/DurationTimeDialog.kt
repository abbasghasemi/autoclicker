package ghasemi.abbas.autoclicker.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.util.Consumer
import ghasemi.abbas.autoclicker.AppConfig
import ghasemi.abbas.autoclicker.R
import ghasemi.abbas.autoclicker.ui.components.DurationTimeView
import ghasemi.abbas.autoclicker.utils.LayoutHelper

class DurationTimeDialog(context: Context, consumer: Consumer<Long>) : BaseDialog(context) {

    init {
        relativeLayout.apply {
            val titleId = View.generateViewId()
            addView(
                TextView(context).apply {
                    id = titleId
                    text = context.resources.getString(R.string.maximum_duration_time)
                    setTextColor(Color.BLACK)
                    gravity = Gravity.CENTER_HORIZONTAL
                    textSize = 20f
                    typeface = ResourcesCompat.getFont(context, R.font.sans_bold)
                }, LayoutHelper.createRelative(
                    LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                    0f, 20f, 0f, 0f, RelativeLayout.CENTER_HORIZONTAL
                )
            )
            addView(
                DurationTimeView(context).apply {
                    this.consumer = consumer
                    updateDurationTime(AppConfig.instance().durationTime)
                },
                LayoutHelper.createRelative(
                    LayoutHelper.MATCH_PARENT,
                    LayoutHelper.WRAP_CONTENT,
                    0f, 10f, 0f, 20f,
                    RelativeLayout.BELOW,
                    titleId,
                )
            )
        }
    }

}