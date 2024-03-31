package ghasemi.abbas.autoclicker.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.ResourcesCompat
import ghasemi.abbas.autoclicker.AppServiceHelper
import ghasemi.abbas.autoclicker.R
import ghasemi.abbas.autoclicker.ui.components.TouchesView
import ghasemi.abbas.autoclicker.utils.AndroidUtils
import ghasemi.abbas.autoclicker.utils.AnimationUtils
import ghasemi.abbas.autoclicker.utils.LayoutHelper
import ghasemi.abbas.autoclicker.utils.rippleBackground

class TestAutoClickerActivity : BaseFragment() {
    override fun onCreateView(context: Context) {
        super.onCreateView(context)
        root.addView(
            TouchesView(context),
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT)
        )
        val textTitle = TextView(context)
        val finishButton = AppCompatImageView(context)
        root.addView(
            FrameLayout(context).apply {
                addView(
                    textTitle.apply {
                        setText(R.string.testing)
                        setBackgroundColor(
                            ResourcesCompat.getColor(
                                resources,
                                R.color.color_primary,
                                null
                            )
                        )
                        setTextColor(Color.WHITE)
                        textSize = 18f
                        gravity = Gravity.CENTER
                        setTypeface(
                            ResourcesCompat.getFont(context, R.font.sans_bold),
                            Typeface.BOLD
                        )
                    },
                    LayoutHelper.createFrame(
                        LayoutHelper.MATCH_PARENT,
                        LayoutHelper.MATCH_PARENT,
                    )
                )
                addView(
                    finishButton.apply {
                        setImageResource(R.drawable.round_arrow_back_24)
                        background = rippleBackground(mask = ColorDrawable(Color.WHITE))
                        setOnClickListener {
                            if (!AppServiceHelper.isRunning) {
                                finishFragment()
                            }
                        }
                    },
                    LayoutHelper.createFrame(
                        32f,
                        LayoutHelper.MATCH_PARENT,
                        Gravity.LEFT,
                        10f, 0f, 0f, 0f
                    )
                )

                addView(
                    AppCompatImageView(context).apply {
                        setImageResource(R.drawable.round_zoom_in_map_24)
                        val r = AndroidUtils.dpf(16f)
                        background = rippleBackground(
                            ShapeDrawable(
                                RoundRectShape(
                                    floatArrayOf(r, r, r, r, r, r, r, r),
                                    null,
                                    null
                                )
                            ).apply {
                                paint.color = context.resources.getColor(R.color.color_primary)
                            }
                        )
                        setOnClickListener {
                            if (finishButton.visibility == View.VISIBLE) {
                                finishButton.visibility = View.GONE
                                textTitle.visibility = View.GONE
                                setImageResource(R.drawable.round_zoom_out_map_24)
                            } else {
                                finishButton.visibility = View.VISIBLE
                                textTitle.visibility = View.VISIBLE
                                setImageResource(R.drawable.round_zoom_in_map_24)
                            }
                            AnimationUtils.changeBounds(textTitle.parent as ViewGroup)
                        }
                    },
                    LayoutHelper.createFrame(
                        32f,
                        32f,
                        Gravity.RIGHT or Gravity.CENTER_VERTICAL,
                        0f, 0f, 10f, 0f
                    )
                )

            },
            LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT,
                64f,
                Gravity.NO_GRAVITY
            )
        )
    }

    override fun onBackInvoked(): Boolean {
        return !AppServiceHelper.isRunning
    }

}