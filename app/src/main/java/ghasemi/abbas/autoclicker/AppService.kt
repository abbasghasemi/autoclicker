package ghasemi.abbas.autoclicker

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.view.WindowManager

class AppService : Service() {

    private var serviceHelper: AppServiceHelper? = null

    override fun onCreate() {
        super.onCreate()
        serviceHelper = AppServiceHelper(this, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE)
        serviceHelper!!.createSettings()
    }

    override fun onDestroy() {
        serviceHelper?.onDestroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_STICKY
    }

}