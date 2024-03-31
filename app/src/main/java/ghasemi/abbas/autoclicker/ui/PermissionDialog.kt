package ghasemi.abbas.autoclicker.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import com.google.android.material.button.MaterialButton
import ghasemi.abbas.autoclicker.R
import ghasemi.abbas.autoclicker.utils.AndroidUtils
import ghasemi.abbas.autoclicker.utils.LayoutHelper

class PermissionDialog constructor(
    activity: Context,
    title: Int,
    content: Int,
    logo: Int = View.NO_ID,
    click: View.OnClickListener,
) : BaseDialog(activity) {
    init {
        relativeLayout.apply {
            val topId = View.generateViewId()
            val centerId = View.generateViewId()
            addView(LinearLayout(activity).apply {
                id = topId
                orientation = LinearLayout.VERTICAL
                setPadding(
                    AndroidUtils.dp(15f),
                    AndroidUtils.dp(15f),
                    AndroidUtils.dp(15f),
                    AndroidUtils.dp(15f)
                )
                background = GradientDrawable(
                    GradientDrawable.Orientation.BL_TR, intArrayOf(
                        0xff4caf50.toInt(), Color.CYAN
                    )
                )
                addView(
                    AppCompatImageView(activity).apply {
                        setImageResource(logo)
                    }, LayoutHelper.createLinear(60f, 60f, Gravity.CENTER_HORIZONTAL)
                )
                addView(
                    TextView(activity).apply {
                        setText(title)
                        textSize = 18f
                        setTextColor(Color.WHITE)
                        typeface = ResourcesCompat.getFont(activity, R.font.sans_bold)
                    }, LayoutHelper.createLinear(
                        LayoutHelper.WRAP_CONTENT,
                        LayoutHelper.WRAP_CONTENT,
                        Gravity.CENTER_HORIZONTAL,
                        0f,
                        10f,
                        0f,
                        0f
                    )
                )
            }, LayoutHelper.createRelative(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
            addView(
                TextView(activity).apply {
                    id = centerId
                    setText(content)
                    typeface = ResourcesCompat.getFont(activity, R.font.sans)
                    textSize = 15f
                    setTextColor(Color.BLACK)
                    gravity = Gravity.CENTER
                }, LayoutHelper.createRelative(
                    LayoutHelper.WRAP_CONTENT,
                    LayoutHelper.WRAP_CONTENT,
                    15f,
                    15f,
                    15f,
                    15f,
                    RelativeLayout.CENTER_HORIZONTAL,
                    RelativeLayout.BELOW,
                    topId
                )
            )
            addView(
                LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(
                        MaterialButton(activity).apply {
                            insetBottom = 0
                            insetTop = 0
                            typeface = ResourcesCompat.getFont(activity, R.font.sans_bold)
                            setText(R.string.permission_confirmation)
                            setOnClickListener {
                                click.onClick(it)
                                dismiss()
                            }
                        },
                        LayoutHelper.createLinear(0f, 48f, 1.2f)
                    )
                    addView(
                        MaterialButton(activity).apply {
                            insetBottom = 0
                            insetTop = 0
                            setText(R.string.later)
                            setOnClickListener {
                                dismiss()
                            }
                            typeface = ResourcesCompat.getFont(activity, R.font.sans_bold)
                            strokeWidth = AndroidUtils.dp(1f)
                            setBackgroundColor(Color.WHITE)
                            strokeColor = ColorStateList.valueOf(
                                ResourcesCompat.getColor(
                                    activity.resources,
                                    R.color.color_primary,
                                    null
                                )
                            )
                            rippleColor = ColorStateList.valueOf(
                                ColorUtils.setAlphaComponent(
                                    ResourcesCompat.getColor(
                                        activity.resources,
                                        R.color.color_primary,
                                        null
                                    ), 20
                                )
                            )
                            setTextColor(
                                ResourcesCompat.getColor(
                                    activity.resources,
                                    R.color.color_primary,
                                    null
                                )
                            )
                        },
                        LayoutHelper.createLinearRelatively(
                            0f,
                            48f,
                            Gravity.NO_GRAVITY,
                            0.5f,
                             15f,
                            0f,
                            0f,
                            0f
                        )
                    )
                },
                LayoutHelper.createRelative(
                    LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                    15f, 0f, 15f, 15f,
                    RelativeLayout.BELOW,
                    centerId
                )
            )
        }
    }
}