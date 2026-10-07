package ghasemi.abbas.autoclicker.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.button.MaterialButton
import ghasemi.abbas.autoclicker.R
import ghasemi.abbas.autoclicker.utils.AndroidUtils

class RateDialog(context: Context, private val onSubmit: () -> Unit) : BaseDialog(context) {
    init {
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(AndroidUtils.dp(20f), AndroidUtils.dp(24f),
                AndroidUtils.dp(20f), AndroidUtils.dp(20f))
        }
        val stars = LinearLayout(context).apply {
            gravity = Gravity.CENTER
            repeat(5) {
                addView(AppCompatImageView(context).apply {
                    setImageResource(R.drawable.round_star_24)
                    imageTintList = ColorStateList.valueOf(0xffffb300.toInt())
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    contentDescription = null
                }, LinearLayout.LayoutParams(AndroidUtils.dp(40f), AndroidUtils.dp(40f)).apply {
                    marginStart = AndroidUtils.dp(2f)
                    marginEnd = AndroidUtils.dp(2f)
                })
            }
        }
        content.addView(stars, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        content.addView(TextView(context).apply {
            setText(R.string.rate_prompt)
            gravity = Gravity.CENTER
            textSize = 16f
            setTextColor(Color.BLACK)
            typeface = ResourcesCompat.getFont(context, R.font.sans)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = AndroidUtils.dp(14f)
            bottomMargin = AndroidUtils.dp(22f)
        })
        content.addView(MaterialButton(context).apply {
            setText(R.string.submit_rating)
            typeface = ResourcesCompat.getFont(context, R.font.sans_bold)
            insetTop = 0
            insetBottom = 0
            setOnClickListener {
                dismiss()
                onSubmit()
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtils.dp(48f)))
        relativeLayout.addView(content, RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

}
