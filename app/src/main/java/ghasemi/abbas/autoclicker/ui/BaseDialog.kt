package ghasemi.abbas.autoclicker.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.view.View
import android.view.ViewOutlineProvider
import android.view.Window
import android.widget.FrameLayout
import android.widget.RelativeLayout
import ghasemi.abbas.autoclicker.utils.AndroidUtils
import ghasemi.abbas.autoclicker.utils.LayoutHelper

open class BaseDialog(context: Context) : Dialog(context) {

    val relativeLayout: RelativeLayout

    init {
        relativeLayout = RelativeLayout(context).apply {
            layoutParams =
                LayoutHelper.createRelative(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                    5f,5f,5f,10f)
            val r = AndroidUtils.dpf(15f)
            background = ShapeDrawable(
                RoundRectShape(
                    floatArrayOf(r, r, r, r, r, r, r, r), null, null
                )
            ).apply {
                paint.color = Color.WHITE
            }
            elevation = AndroidUtils.dpf(4f)
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(null)
        setContentView(FrameLayout(context).apply {
            addView(relativeLayout)
        })
    }

    final override fun setContentView(view: View) {
        super.setContentView(view)
    }
}