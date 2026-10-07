package ghasemi.abbas.autoclicker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile.STATE_ACTIVE
import android.service.quicksettings.Tile.STATE_INACTIVE
import android.service.quicksettings.TileService
import ghasemi.abbas.autoclicker.ui.PermissionDialog
import ghasemi.abbas.autoclicker.utils.AndroidUtils

class AppQSTileService : TileService(){

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(ApplicationLoader.applicationCreateConfigurationContext(newBase))
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTile()
    }

    private fun updateTile() {
        if (qsTile == null) {
            return
        }
        qsTile.state = if (AppServiceHelper.isEnabled) STATE_ACTIVE else STATE_INACTIVE
        qsTile.label = getString(if (AppServiceHelper.isEnabled) R.string.end_activity else R.string.start_activity)
        qsTile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        if (AndroidUtils.isAccessibilityServiceEnabled()) {
            NotificationCenter.instance().postNotificationName(
                if (AppServiceHelper.isEnabled) NotificationCenter.appServiceStop else NotificationCenter.appServiceStart
            )
//            AndroidUtils.runOnUIThread({
//                updateTile()
//            }, 50)
        } else {
            setTheme(R.style.Theme_AutoClicker)
            showDialog(
                PermissionDialog(
                    this,
                    R.string.accessibility_permission,
                    R.string.accessibility_permission_description,
                    R.drawable.round_settings_accessibility_24,
                    { launchSettings(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), 1) },
                    { launchSettings(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:$packageName")), 2) }
                )
            )
        }
    }

    private fun launchSettings(intent: Intent, requestCode: Int) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            val pending = PendingIntent.getActivity(this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startID: Int): Int {
        return START_STICKY
    }
}
