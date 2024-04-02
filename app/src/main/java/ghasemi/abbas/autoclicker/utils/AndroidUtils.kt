package ghasemi.abbas.autoclicker.utils

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.service.quicksettings.TileService
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.text.TextUtilsCompat
import com.google.android.material.snackbar.Snackbar
import ghasemi.abbas.autoclicker.AppAccessibilityService
import ghasemi.abbas.autoclicker.AppQSTileService
import ghasemi.abbas.autoclicker.AppWidget
import ghasemi.abbas.autoclicker.ApplicationLoader
import kotlin.math.roundToInt


object AndroidUtils {
    private var density = -1f
    private var vibrator: Vibrator? = null

    init {
        if (ApplicationLoader.context != null) {
            density = ApplicationLoader.context!!.resources.displayMetrics.density
            vibrator =
                ApplicationLoader.context!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        }
    }

    fun toast(message: String) {
        Toast.makeText(
            ApplicationLoader.context,
            message,
            if (message.length > 40) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        ).show()
    }

    fun dp(value: Float): Int {
        return dpf(value).toInt()
    }

    fun dpf(value: Float): Float {
        return density * value
    }

    fun dpr(value: Float): Int {
        return if (value == 0f) {
            0
        } else dpf(value).roundToInt()
    }

    fun showMessage(view: View, message: String, anchor: View? = null) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).setAnchorView(anchor).show()
    }

    fun showDialog(
        context: Context,
        title: String,
        content: String,
        btnName: String?,
        bunRun: Runnable?,
        cancelRun: Runnable? = null
    ) {
        AlertDialog.Builder(context)
            .setCancelable(false)
            .setTitle(title)
            .setMessage(content)
            .setPositiveButton(
                btnName ?: "بستن",
                if (bunRun == null) null else DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> bunRun.run() }
            ).apply {
                if (bunRun != null && btnName != null) {
                    setNegativeButton(
                        "بستن",
                        if (cancelRun == null) null else DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> cancelRun.run() }
                    )
                }
                show()
            }
    }

    fun hideKeyboard(activity: Activity) {
        val view = activity.currentFocus
        hideKeyboard(view ?: activity.window.decorView)
    }

    fun hideKeyboard(view: View?) {
        if (view == null) {
            return
        }
        try {
            val imm =
                view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (!imm.isActive) {
                return
            }
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        } catch (e: Exception) {
            //
        }
    }

    fun showKeyboard(view: View?): Boolean {
        if (view == null) {
            return false
        }
        try {
            val inputManager =
                view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            return inputManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        } catch (e: Exception) {
            //
        }
        return false
    }

    fun copy(caption: CharSequence?, toast: String? = "کپی شد.") {
        if (ApplicationLoader.context == null) return
        val manager =
            ApplicationLoader.context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText("caption", caption))
        if (toast != null) {
            toast(toast)
        }
    }

    val isRTL: Boolean
        get() = TextUtilsCompat.getLayoutDirectionFromLocale(ApplicationLoader.context!!.resources.configuration.locales[0]) == View.LAYOUT_DIRECTION_RTL

    fun runOnUIThread(runnable: Runnable?, delay: Long = 0) {
        if (delay == 0L) {
            ApplicationLoader.applicationHandler.post(runnable!!)
        } else {
            ApplicationLoader.applicationHandler.postDelayed(runnable!!, delay)
        }
    }

    fun cancelRunOnUIThread(runnable: Runnable?) {
        ApplicationLoader.applicationHandler.removeCallbacks(runnable!!)
    }

    fun isAccessibilityServiceEnabled(): Boolean {
        if (ApplicationLoader.context == null) return false
        val am =
            ApplicationLoader.context!!.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices =
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (enabledService in enabledServices) {
            val enabledServiceInfo: ServiceInfo = enabledService.resolveInfo.serviceInfo
            if (enabledServiceInfo.packageName.equals(ApplicationLoader.context!!.packageName) && enabledServiceInfo.name.equals(
                    AppAccessibilityService::class.java.name
                )
            ) return true
        }
        return false
    }

    fun longVibrator() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(80L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator?.vibrate(longArrayOf(80L), -1)
        }
    }

     fun updateTileAndWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(ApplicationLoader.context)
        for (i in appWidgetManager.getAppWidgetIds(ComponentName(ApplicationLoader.context!!, AppWidget::class.java))) {
            AppWidget.initWidget(appWidgetManager, i)
        }
        TileService.requestListeningState(
            ApplicationLoader.context,
            ComponentName(ApplicationLoader.context!!, AppQSTileService::class.java)
        )
    }
}