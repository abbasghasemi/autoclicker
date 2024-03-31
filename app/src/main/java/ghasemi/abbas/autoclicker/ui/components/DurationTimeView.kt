package ghasemi.abbas.autoclicker.ui.components

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.util.Consumer
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.ag.recyclerview.easyadapter.ItemAdapter
import ghasemi.abbas.autoclicker.R
import ghasemi.abbas.autoclicker.utils.AndroidUtils
import ghasemi.abbas.autoclicker.utils.LayoutHelper
import kotlin.math.abs


class DurationTimeView(context: Context) : FrameLayout(context),
    ItemAdapter.ViewBinding<TextView, Int> {
    lateinit var consumer: Consumer<Long>
    private var hours: ViewPager2
    private var minutes: ViewPager2
    private var seconds: ViewPager2
    private val time: TextView
    init {
        layoutDirection = LAYOUT_DIRECTION_LTR
        addView(
            View(context).apply {
                setBackgroundColor(ColorUtils.setAlphaComponent(Color.GRAY, 100))
            },
            LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT,
                1f,
                Gravity.NO_GRAVITY,
                0f, 40f, 0f, 0f,
            )
        )
        addView(
            View(context).apply {
                setBackgroundColor(ColorUtils.setAlphaComponent(Color.GRAY, 100))
            },
            LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT,
                1f,
                Gravity.NO_GRAVITY,
                0f, 80f, 0f, 0f,
            )
        )
        addView(LinearLayout(context).apply {
            hours = ViewPager2(context).apply {
                orientation = ViewPager2.ORIENTATION_VERTICAL
                setPageTransformer(AlphaAndScalePageTransformer())
                adapter = ItemAdapter((0..23).toMutableList(), this@DurationTimeView)
            }
            minutes = ViewPager2(context).apply {
                orientation = ViewPager2.ORIENTATION_VERTICAL
                setPageTransformer(AlphaAndScalePageTransformer())
                adapter = ItemAdapter((0..59).toMutableList(), this@DurationTimeView)
            }
            seconds = ViewPager2(context).apply {
                orientation = ViewPager2.ORIENTATION_VERTICAL
                setPageTransformer(AlphaAndScalePageTransformer())
                adapter = ItemAdapter((0..59).toMutableList(), this@DurationTimeView)
            }

            addView(hours, LayoutHelper.createLinear(0f, LayoutHelper.MATCH_PARENT, 1f))
            addView(minutes, LayoutHelper.createLinear(0f, LayoutHelper.MATCH_PARENT, 1f))
            addView(seconds, LayoutHelper.createLinear(0f, LayoutHelper.MATCH_PARENT, 1f))
        }, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 120f))

        time = TextView(context)
        addView(
            time.apply {
                gravity = Gravity.CENTER_HORIZONTAL
                typeface = ResourcesCompat.getFont(context, R.font.sans_bold)
                textSize = 18f
                setTextColor(Color.BLACK)
            },
            LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT,
                Gravity.NO_GRAVITY,
                0f, 110f, 0f, 0f,
            )
        )
    }

    fun updateDurationTime(durationTime:Long) {
        val h = durationTime / 60 / 60
        val m = (durationTime - h * 60 * 60) / 60
        val s = durationTime % 60
        hours.setCurrentItem(h.toInt(), false)
        minutes.setCurrentItem(m.toInt(), false)
        seconds.setCurrentItem(s.toInt(), false)
        onPageChangeCallback.onPageSelected(0)
    }

    override fun createItem(
        inflater: LayoutInflater,
        parent: ViewGroup,
        itemType: Int
    ): ItemAdapter.Binding<TextView> {
        val itemView = TextView(context).apply {
            setTextColor(Color.BLACK)
            textSize = 25f
            gravity = Gravity.CENTER
            typeface = ResourcesCompat.getFont(context, R.font.sans_bold)
            layoutParams =
                LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT)
        }
        return ItemAdapter.Binding(itemView, itemView)
    }

    override fun bindItem(view: TextView, item: Int, position: Int, itemType: Int): Boolean {
        view.text = position.toString()
        return true
    }

    class AlphaAndScalePageTransformer : ViewPager2.PageTransformer {
        private val scaleMax = 0.8f
        private val alphaMax = 0.4f
        private val pageMarginPx = AndroidUtils.dp(40f)
        override fun transformPage(page: View, position: Float) {
//            val viewPager = requireViewPager(page)
            val offset = position * -(2 * pageMarginPx)
//            val totalMargin = offset + pageMarginPx
//            if (viewPager.orientation == ViewPager2.ORIENTATION_HORIZONTAL) {
//                page.translationX = if (AndroidUtils.isRTL) -offset else offset
//            } else {
//                page.updateLayoutParams<MarginLayoutParams> {
//                    topMargin = totalMargin.toInt()
//                    bottomMargin = totalMargin.toInt()
//                }
            page.translationY = offset
//            }

            val scale =
                if (position < 0) (1 - scaleMax) * position + 1 else (scaleMax - 1) * position + 1
            val alpha =
                if (position < 0) (1 - alphaMax) * position + 1 else (alphaMax - 1) * position + 1
//            if (position < 0) {
//                page.pivotX = page.width.toFloat()
//                page.pivotY = (page.height / 2).toFloat()
//            } else {
//                page.pivotX = 0f
//                page.pivotY = (page.height / 2).toFloat()
//            }
            page.scaleX = scale
            page.scaleY = scale
            page.alpha = abs(alpha)
        }

        private fun requireViewPager(page: View): ViewPager2 {
            val parent = page.parent
            val parentParent = parent.parent
            if (parent is RecyclerView && parentParent is ViewPager2) {
                return parentParent
            }
            throw IllegalStateException(
                "Expected the page view to be managed by a ViewPager2 instance."
            )
        }
    }

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            time.text = "${hours.currentItem}h ${minutes.currentItem}m ${seconds.currentItem}s"
            consumer.accept(hours.currentItem * 60L * 60 + minutes.currentItem * 60 + seconds.currentItem)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        hours.registerOnPageChangeCallback(onPageChangeCallback)
        minutes.registerOnPageChangeCallback(onPageChangeCallback)
        seconds.registerOnPageChangeCallback(onPageChangeCallback)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        hours.unregisterOnPageChangeCallback(onPageChangeCallback)
        minutes.unregisterOnPageChangeCallback(onPageChangeCallback)
        seconds.unregisterOnPageChangeCallback(onPageChangeCallback)
    }
}