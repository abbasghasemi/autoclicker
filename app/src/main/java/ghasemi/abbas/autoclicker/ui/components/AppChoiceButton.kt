package ghasemi.abbas.autoclicker.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.button.MaterialButton
import ghasemi.abbas.autoclicker.R
import ghasemi.abbas.autoclicker.utils.AndroidUtils
import android.content.res.ColorStateList
import android.view.WindowManager

class AppChoiceButton(
    context: Context,
    private val entries: Array<String>,
    initialIndex: Int,
    private val title: String,
    private val windowType: Int? = null,
    private val onSelection: (Int) -> Unit = {}
) : MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle) {
    var selectedIndex = initialIndex.coerceIn(entries.indices)
        private set

    init {
        isAllCaps = false
        typeface = ResourcesCompat.getFont(context, R.font.sans)
        setIconResource(R.drawable.round_arrow_drop_down_24)
        iconGravity = ICON_GRAVITY_TEXT_END
        iconSize = AndroidUtils.dp(20f)
        insetTop = 0
        insetBottom = 0
        cornerRadius = AndroidUtils.dp(8f)
        gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
        textAlignment = View.TEXT_ALIGNMENT_GRAVITY
        val states = arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf())
        strokeColor = ColorStateList(states,
            intArrayOf(0xffdedede.toInt(), 0xff0a9a58.toInt()))
        setTextColor(ColorStateList(states,
            intArrayOf(0xffb8b8b8.toInt(), 0xff087344.toInt())))
        iconTint = ColorStateList(states,
            intArrayOf(0xffb8b8b8.toInt(), 0xff087344.toInt()))
        setSelection(selectedIndex)
        setOnClickListener { showChoices() }
    }

    private fun showChoices() {
        showMenu(this, entries, windowType) { index ->
            setSelection(index)
            onSelection(index)
        }
    }

    companion object {
    fun showMenu(anchorView: View, entries: Array<String>, windowType: Int? = null,
                 onSelection: (Int) -> Unit) {
        val context = anchorView.context
        val radius = AndroidUtils.dp(10f)
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = radius.toFloat()
            }
            clipToOutline = true
        }
        var dismissChoices: () -> Unit = {}
        entries.forEachIndexed { index, entry ->
            content.addView(TextView(context).apply {
                text = entry
                textSize = 14f
                setTextColor(Color.BLACK)
                typeface = ResourcesCompat.getFont(context, R.font.sans)
                gravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
                setPadding(AndroidUtils.dp(14f), 0, AndroidUtils.dp(14f), 0)
                background = RippleDrawable(ColorStateList.valueOf(0x220a9a58), null,
                    GradientDrawable().apply {
                        setColor(Color.WHITE)
                        cornerRadius = AndroidUtils.dp(6f).toFloat()
                    })
                isClickable = true
                setOnClickListener {
                    onSelection(index)
                    dismissChoices()
                }
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtils.dp(48f)))
        }
        val menuWidth = anchorView.width.coerceAtLeast(AndroidUtils.dp(150f))
        if (windowType == null) {
            val popup = PopupWindow(content, menuWidth, ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
                isOutsideTouchable = true
                setBackgroundDrawable(GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = radius.toFloat()
                })
                elevation = AndroidUtils.dpf(5f)
                inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
            }
            dismissChoices = { popup.dismiss() }
            popup.showAsDropDown(anchorView)
        } else {
            val dialog = AlertDialog.Builder(context, R.style.AppAlertDialog)
                .setView(content).create()
            val window = dialog.window ?: return
            window.setType(windowType)
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            val anchor = IntArray(2)
            anchorView.getLocationOnScreen(anchor)
            val screenHeight = anchorView.resources.displayMetrics.heightPixels
            val menuHeight = entries.size * AndroidUtils.dp(48f)
            val below = screenHeight - anchor[1] - anchorView.height
            val top = if (below >= menuHeight) anchor[1] + anchorView.height
                else (anchor[1] - menuHeight).coerceAtLeast(0)
            dismissChoices = { dialog.dismiss() }
            try {
                dialog.show()
                window.setBackgroundDrawableResource(R.drawable.app_alert_background)
                window.setLayout(menuWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
                window.attributes = window.attributes.apply {
                    gravity = Gravity.TOP or Gravity.LEFT
                    x = anchor[0]
                    y = top
                    dimAmount = 0f
                }
            } catch (_: WindowManager.BadTokenException) {
                dialog.dismiss()
            }
        }
    }
    }

    fun setSelection(index: Int) {
        selectedIndex = index.coerceIn(entries.indices)
        text = entries[selectedIndex]
        contentDescription = "$title: ${entries[selectedIndex]}"
    }
}
