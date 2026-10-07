package ghasemi.abbas.autoclicker.ui

import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import ghasemi.abbas.autoclicker.R
import ghasemi.abbas.autoclicker.utils.AndroidUtils

/** One window treatment for activity and overlay dialogs. Content determines the height. */
object AppDialogUi {
    fun show(dialog: AlertDialog, windowType: Int? = null): AlertDialog {
        if (windowType != null) dialog.window?.setType(windowType)
        dialog.window?.setBackgroundDrawableResource(R.drawable.app_alert_background)
        dialog.show()
        val window = dialog.window ?: return dialog
        val metrics = dialog.context.resources.displayMetrics
        val width = (metrics.widthPixels - AndroidUtils.dp(32f)).coerceAtLeast(AndroidUtils.dp(240f))
        val maxHeight = (metrics.heightPixels - AndroidUtils.dp(48f)).coerceAtLeast(AndroidUtils.dp(240f))
        window.setBackgroundDrawableResource(R.drawable.app_alert_background)
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        val primary = ContextCompat.getColor(dialog.context, R.color.color_primary)
        for (which in intArrayOf(AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE,
            AlertDialog.BUTTON_NEUTRAL)) {
            dialog.getButton(which)?.apply {
                setTextColor(primary)
                typeface = ResourcesCompat.getFont(context, R.font.sans_bold)
                isAllCaps = false
                minHeight = AndroidUtils.dp(44f)
            }
        }
        window.decorView.post {
            if (dialog.isShowing && window.decorView.height > maxHeight) {
                window.setLayout(width, maxHeight)
            }
        }
        return dialog
    }
}
