package ghasemi.abbas.autoclicker

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import ghasemi.abbas.autoclicker.utils.AndroidUtils


class AppBroadcast : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (AndroidUtils.isAccessibilityServiceEnabled()) {
            NotificationCenter.instance().postNotificationName(
                if (AppServiceHelper.isEnabled) NotificationCenter.appServiceStop else NotificationCenter.appServiceStart
            )
            if (intent == null) {
                return
            }
            val appWidgetManager = AppWidgetManager.getInstance(context)
//            AndroidUtils.runOnUIThread({
//                AppWidget.initWidget(
//                    appWidgetManager,
//                    intent.getIntExtra(
//                        AppWidgetManager.EXTRA_APPWIDGET_ID,
//                        AppWidgetManager.INVALID_APPWIDGET_ID
//                    )
//                )
//            }, 50)
        } else {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }
}