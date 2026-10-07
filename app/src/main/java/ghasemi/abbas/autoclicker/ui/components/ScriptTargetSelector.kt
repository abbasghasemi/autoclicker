package ghasemi.abbas.autoclicker.ui.components

import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import ghasemi.abbas.autoclicker.R
import ghasemi.abbas.autoclicker.ui.AppDialogUi
import ghasemi.abbas.autoclicker.utils.AndroidUtils
import ghasemi.abbas.autoclicker.utils.circularRippleBackground

class ScriptTargetSelector(context: Context, initialPackage: String, initialAction: Int,
    private val dialogWindowType: Int? = null) : LinearLayout(context) {
    private val packageManager = context.packageManager
    private val icon = ImageView(context)
    private val title = TextView(context)
    private val packageText = TextView(context)
    private val clear = ImageView(context)
    private val leaveTitle = TextView(context)
    private val action = AppChoiceButton(context,
        context.resources.getStringArray(R.array.on_leave_actions), initialAction,
        context.getString(R.string.on_leave_title), dialogWindowType)
    var targetPackage = initialPackage
        private set
    val onLeaveAction: Int get() = if (targetPackage.isBlank()) 0 else action.selectedIndex

    init {
        orientation = VERTICAL
        val card = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(AndroidUtils.dp(10f), AndroidUtils.dp(5f), AndroidUtils.dp(6f), AndroidUtils.dp(5f))
            background = RippleDrawable(ColorStateList.valueOf(0x22000000),
                rounded(0xfffafafa.toInt(), 0xffd9e5dd.toInt()), rounded(Color.WHITE, null))
            isClickable = true
            setOnClickListener { chooseApp() }
        }
        addView(card, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtils.dp(58f)))
        icon.scaleType = ImageView.ScaleType.FIT_CENTER
        card.addView(icon, LayoutParams(AndroidUtils.dp(36f), AndroidUtils.dp(36f)))
        val details = LinearLayout(context).apply { orientation = VERTICAL }
        card.addView(details, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = AndroidUtils.dp(9f)
        })
        title.apply {
            textSize = 14f
            setTextColor(Color.BLACK)
            typeface = ResourcesCompat.getFont(context, R.font.sans_bold)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        details.addView(title)
        packageText.apply {
            textSize = 11f
            setTextColor(Color.DKGRAY)
            typeface = ResourcesCompat.getFont(context, R.font.sans)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.MIDDLE
        }
        details.addView(packageText)
        clear.apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            contentDescription = context.getString(R.string.clear_target_app)
            background = circularRippleBackground()
            setOnClickListener { targetPackage = ""; updateApp() }
        }
        card.addView(clear, LayoutParams(AndroidUtils.dp(34f), AndroidUtils.dp(34f)))

        addView(leaveTitle.apply {
            setText(R.string.on_leave_title)
            textSize = 13f
            setTextColor(Color.BLACK)
            typeface = ResourcesCompat.getFont(context, R.font.sans_bold)
            includeFontPadding = false
            gravity = Gravity.RIGHT
        }, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = AndroidUtils.dp(8f)
        })
        addView(action, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtils.dp(44f)))
        updateApp()
    }

    private fun rounded(color: Int, stroke: Int?) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = AndroidUtils.dp(10f).toFloat()
        if (stroke != null) setStroke(AndroidUtils.dp(1f), stroke)
    }

    private fun updateApp() {
        val selected = targetPackage.isNotBlank()
        title.text = if (selected) runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(targetPackage, 0))
        }.getOrDefault(targetPackage) else context.getString(R.string.no_target_app)
        packageText.text = targetPackage
        val isPersianLayout = title.text.toString().firstOrNull { it.isLetter() }
            ?.let { it.lowercaseChar() !in 'a'..'z' } ?: false
        title.gravity = if (isPersianLayout) Gravity.RIGHT else Gravity.LEFT
        packageText.gravity = if (isPersianLayout) Gravity.RIGHT else Gravity.LEFT
        packageText.textDirection = View.TEXT_DIRECTION_LTR
        packageText.visibility = if (selected) VISIBLE else GONE
        icon.setImageDrawable(if (selected) runCatching {
            packageManager.getApplicationIcon(targetPackage)
        }.getOrNull() else null)
        if (!selected) icon.setImageResource(android.R.drawable.sym_def_app_icon)
        clear.visibility = if (selected) VISIBLE else GONE
        leaveTitle.alpha = if (selected) 1f else 0.42f
        action.isEnabled = selected
        if (!selected) action.setSelection(0)
    }

    private fun chooseApp() {
        val apps = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        ).filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(packageManager).toString() }
        AlertDialog.Builder(context, R.style.AppAlertDialog)
            .setTitle(R.string.choose_target_app_title)
            .setAdapter(object : BaseAdapter() {
                override fun getCount() = apps.size
                override fun getItem(position: Int) = apps[position]
                override fun getItemId(position: Int) = position.toLong()
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val app = apps[position]
                    val label = app.loadLabel(packageManager).toString()
                    val firstLetter = label.firstOrNull { it.isLetter() }
                    val isPersianLayout = firstLetter != null && firstLetter.lowercaseChar() !in 'a'..'z'
                    return LinearLayout(context).apply {
                        gravity = Gravity.CENTER_VERTICAL
                        layoutDirection = if (isPersianLayout) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
                        setPadding(AndroidUtils.dp(16f), AndroidUtils.dp(7f), AndroidUtils.dp(16f), AndroidUtils.dp(7f))
                        addView(ImageView(context).apply {
                            setImageDrawable(app.loadIcon(packageManager))
                        }, LayoutParams(AndroidUtils.dp(36f), AndroidUtils.dp(36f)))
                        addView(LinearLayout(context).apply {
                            orientation = VERTICAL
                            addView(TextView(context).apply {
                                text = label
                                setTextColor(Color.BLACK)
                                textSize = 14f
                                typeface = ResourcesCompat.getFont(context, R.font.sans_bold)
                                textDirection = if (isPersianLayout) View.TEXT_DIRECTION_RTL else View.TEXT_DIRECTION_LTR
                                gravity = Gravity.START
                            })
                            addView(TextView(context).apply {
                                text = app.activityInfo.packageName
                                setTextColor(Color.DKGRAY)
                                textSize = 11f
                                typeface = ResourcesCompat.getFont(context, R.font.sans)
                                textDirection = View.TEXT_DIRECTION_LTR
                                gravity = if (isPersianLayout) Gravity.RIGHT else Gravity.LEFT
                            })
                        }, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                            marginStart = AndroidUtils.dp(10f)
                        })
                    }
                }
            }) { _, index ->
                targetPackage = apps[index].activityInfo.packageName
                updateApp()
            }
            .setNegativeButton(R.string.close, null)
            .create().let { AppDialogUi.show(it, dialogWindowType) }
    }
}
