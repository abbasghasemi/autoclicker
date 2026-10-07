package ghasemi.abbas.autoclicker.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import ghasemi.abbas.autoclicker.R
import ghasemi.abbas.autoclicker.RecommendedApps
import ghasemi.abbas.autoclicker.utils.AndroidUtils
import androidx.core.graphics.toColorInt

class RecommendedAppsView(context: Context) : LinearLayout(context) {
    private val adapter = AppsAdapter()
    private val list = RecyclerView(context)
    private val toggle = ImageView(context)
    private val rowHeight = AndroidUtils.dp(56f)
    private var expanded = true
    private var expansionAnimator: ValueAnimator? = null

    init {
        orientation = VERTICAL
        val windowBackground = context.obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))
        background = windowBackground.getDrawable(0)
        windowBackground.recycle()
        setPadding(AndroidUtils.dp(10f), AndroidUtils.dp(2f), AndroidUtils.dp(10f), AndroidUtils.dp(2f))
        visibility = GONE
        addView(FrameLayout(context).apply {
            addView(View(context).apply {
                setBackgroundColor(0xffd9e5dd.toInt())
            }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtils.dp(1f), Gravity.CENTER_VERTICAL))
            addView(toggle.apply {
                setImageResource(R.drawable.round_arrow_drop_down_24)
                setColorFilter(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, Color.BLACK))
                scaleType = ImageView.ScaleType.CENTER
                rotation = 180f
                contentDescription = context.getString(R.string.hide_recommended_apps)
                background = RippleDrawable(
                    ColorStateList.valueOf(0x22000000),
                    roundedBackground(MaterialColors.getColor(this, android.R.attr.colorBackground, Color.WHITE),
                        0xffd9e5dd.toInt(), 16f),
                    roundedBackground(Color.WHITE, null, 16f)
                )
                isClickable = true
                setOnClickListener { toggleList() }
            }, FrameLayout.LayoutParams(AndroidUtils.dp(34f), AndroidUtils.dp(30f), Gravity.CENTER))
        }, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtils.dp(32f)))
        list.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = this@RecommendedAppsView.adapter
            overScrollMode = OVER_SCROLL_NEVER
            addOnLayoutChangeListener { _, left, _, right, _, _, _, _, _ ->
                if (right > left) post { this@RecommendedAppsView.adapter.updateViewport(width) }
            }
        }
        addView(list, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowHeight))
        RecommendedApps.load(context) { apps ->
            adapter.submit(apps)
            visibility = if (apps.isEmpty()) GONE else VISIBLE
        }
    }

    private fun toggleList() {
        expansionAnimator?.cancel()
        expanded = !expanded
        if (expanded) list.visibility = VISIBLE
        val targetHeight = if (expanded) rowHeight else 0
        val animation = ValueAnimator.ofInt(list.height, targetHeight)
        animation.duration = 220
        animation.interpolator = DecelerateInterpolator()
        animation.addUpdateListener {
            val height = it.animatedValue as Int
            list.layoutParams = list.layoutParams.apply { this.height = height }
            list.alpha = height.toFloat() / rowHeight
        }
        animation.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animator: Animator) {
                if (expansionAnimator === animation) {
                    if (!expanded) list.visibility = GONE
                    expansionAnimator = null
                }
            }
        })
        expansionAnimator = animation
        animation.start()
        toggle.animate().rotation(if (expanded) 180f else 0f).setDuration(220).start()
        toggle.contentDescription = context.getString(
            if (expanded) R.string.hide_recommended_apps else R.string.show_recommended_apps
        )
    }

    private fun roundedBackground(color: Int, stroke: Int?, radius: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = AndroidUtils.dp(radius).toFloat()
        if (stroke != null) setStroke(AndroidUtils.dp(1f), stroke)
    }

    private inner class AppsAdapter : RecyclerView.Adapter<AppsAdapter.Holder>() {
        private var apps: List<RecommendedApps.App> = emptyList()
        private var viewport = 0
        private val gap = AndroidUtils.dp(6f)

        fun submit(items: List<RecommendedApps.App>) {
            apps = items
            notifyDataSetChanged()
        }

        fun updateViewport(width: Int) {
            if (width > 0 && width != viewport) {
                viewport = width
                notifyDataSetChanged()
            }
        }

        override fun getItemCount() = apps.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val card = LinearLayout(parent.context).apply {
                gravity = Gravity.CENTER_VERTICAL
                orientation = HORIZONTAL
                setPadding(AndroidUtils.dp(7f), AndroidUtils.dp(3f), AndroidUtils.dp(7f), AndroidUtils.dp(3f))
                background = RippleDrawable(
                    ColorStateList.valueOf(0x22000000),
                    roundedBackground(Color.TRANSPARENT, 0xffd9e5dd.toInt(), 9f),
                    roundedBackground(Color.WHITE, null, 9f)
                )
                layoutParams = RecyclerView.LayoutParams(AndroidUtils.dp(160f), ViewGroup.LayoutParams.MATCH_PARENT)
                isClickable = true
                setOnTouchListener { view, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> view.animate().scaleX(0.98f).scaleY(0.98f).setDuration(90).start()
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                            view.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                    }
                    false
                }
            }
            val icon = ImageView(parent.context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageResource(android.R.drawable.sym_def_app_icon)
            }
            card.addView(icon, LayoutParams(AndroidUtils.dp(34f), AndroidUtils.dp(34f)))
            val details = LinearLayout(parent.context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER_VERTICAL
            }
            card.addView(details, LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                marginStart = AndroidUtils.dp(8f)
            })
            val title = TextView(parent.context).apply {
                textSize = 13f
                setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, Color.BLACK))
                setSingleLine(true)
                ellipsize = TextUtils.TruncateAt.MARQUEE
                marqueeRepeatLimit = -1
                isSelected = true
                gravity = Gravity.CENTER_VERTICAL
            }
            details.addView(title, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            val badge = TextView(parent.context).apply {
                textSize = 10f
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setPadding(AndroidUtils.dp(6f), 0, AndroidUtils.dp(6f), 0)
            }
            details.addView(badge, LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = AndroidUtils.dp(1f)
            })
            return Holder(card, icon, title, badge)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val app = apps[position]
            val width = when (apps.size) {
                1 -> viewport
                2 -> (viewport - gap) / 2
                else -> ((viewport - 2 * gap) / 2.15f).toInt()
            }.coerceAtLeast(AndroidUtils.dp(100f))
            holder.itemView.layoutParams = (holder.itemView.layoutParams as RecyclerView.LayoutParams).apply {
                this.width = width
                marginEnd = if (position == apps.lastIndex) 0 else gap
            }
            holder.title.text = app.title
            val badge = app.badge
            holder.badge.visibility = if (badge.isNullOrBlank()) View.GONE else View.VISIBLE
            if (!badge.isNullOrBlank()) {
                holder.badge.text = badge
                val badgeColor = app.badgeColor?.takeIf { it.matches(Regex("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")) }
                    ?.let { runCatching { it.toColorInt() }.getOrNull() }
                    ?: 0xff0cdc73.toInt()
                holder.badge.background = roundedBackground(badgeColor, null, 5f)
                val visibleColor = ColorUtils.compositeColors(badgeColor, Color.WHITE)
                holder.badge.setTextColor(
                    if (ColorUtils.calculateContrast(Color.BLACK, visibleColor) >=
                        ColorUtils.calculateContrast(Color.WHITE, visibleColor)) Color.BLACK else Color.WHITE
                )
            }
            holder.icon.setImageResource(android.R.drawable.sym_def_app_icon)
            holder.icon.tag = app.icon
            RecommendedApps.loadIcon(context, app.icon) { bitmap ->
                if (bitmap != null && holder.icon.tag == app.icon) holder.icon.setImageBitmap(bitmap)
            }
            holder.itemView.setOnClickListener {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(app.url)))
                } catch (_: ActivityNotFoundException) {
                    // No browser is installed.
                }
            }
        }

        inner class Holder(row: LinearLayout, val icon: ImageView, val title: TextView, val badge: TextView) :
            RecyclerView.ViewHolder(row)
    }
}
