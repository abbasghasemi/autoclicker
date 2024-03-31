package ghasemi.abbas.autoclicker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class AppWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (i in appWidgetIds.indices) {
            initWidget(appWidgetManager, appWidgetIds[i])
        }

    }
    companion object {

        fun initWidget(
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val pendingIntent = PendingIntent.getBroadcast(
                ApplicationLoader.context,
                0,
                Intent(ApplicationLoader.context, AppBroadcast::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val views = RemoteViews(ApplicationLoader.context!!.packageName, R.layout.appwidget_layout)
            views.setOnClickPendingIntent(R.id.widget_frame, pendingIntent)
            views.setTextViewText(
                R.id.widget_name,
                ApplicationLoader.context!!.resources.getString(if (AppServiceHelper.isEnabled) R.string.end_activity else R.string.start_activity)
            )
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

}