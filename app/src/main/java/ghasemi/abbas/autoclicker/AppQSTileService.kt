package ghasemi.abbas.autoclicker

import android.content.Context
import android.content.Intent
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
                if (AppServiceHelper.isEnabled) NotificationCenter.appServiceDismiss else NotificationCenter.appServiceStart
            )
            AndroidUtils.runOnUIThread({
                updateTile()
            }, 50)
        } else {
            setTheme(R.style.Theme_AutoClicker)
            showDialog(
                PermissionDialog(
                    this,
                    R.string.accessibility_permission,
                    R.string.accessibility_permission_description,
                    R.drawable.round_settings_accessibility_24
                ) {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
            )
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startID: Int): Int {
        return START_STICKY
    }
}