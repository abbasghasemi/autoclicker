package ghasemi.abbas.autoclicker

import android.content.Context
import android.content.SharedPreferences
import ghasemi.abbas.autoclicker.ApplicationLoader.Companion.context

class AppConfig private constructor(context: Context?) {
    private val sharedPreferences: SharedPreferences

    init {
        sharedPreferences = context!!.getSharedPreferences("appConfig", Context.MODE_PRIVATE)
    }

    var isDarkMode: Boolean
        get() = sharedPreferences.getBoolean("isDarkMode", false)
        set(isDark) {
            sharedPreferences.edit().putBoolean("isDarkMode", isDark).apply()
        }

    var lastPermissionConfirmed: Boolean
        get() = sharedPreferences.getBoolean("lastPermissionConfirmed", false)
        set(confirmed) {
            sharedPreferences.edit().putBoolean("lastPermissionConfirmed", confirmed).apply()
        }

    var scriptsConfigList: String
        get() = sharedPreferences.getString("scriptsConfigList", "")!!
        set(configList) {
            sharedPreferences.edit().putString("scriptsConfigList", configList).apply()
        }

    var synchronousExecution: Boolean
        get() = sharedPreferences.getBoolean("synchronousExecution", false)
        set(sync) {
            sharedPreferences.edit().putBoolean("synchronousExecution", sync).apply()
        }
    var hiddenWidgetsExecution: Boolean
        get() = sharedPreferences.getBoolean("hiddenWidgetsExecution", false)
        set(hidden) {
            sharedPreferences.edit().putBoolean("hiddenWidgetsExecution", hidden).apply()
        }

    var clickTime: Int
        get() = sharedPreferences.getInt("clickTime", 100)
        set(time) {
            sharedPreferences.edit().putInt("clickTime", time).apply()
        }

    var clickTimeType: Int
        get() = sharedPreferences.getInt("clickTimeType", 0)
        set(type) {
            sharedPreferences.edit().putInt("clickTimeType", type).apply()
        }

    var longClickTime: Int
        get() = sharedPreferences.getInt("longClickTime", 1)
        set(time) {
            sharedPreferences.edit().putInt("longClickTime", time).apply()
        }

    var swipeTime: Int
        get() = sharedPreferences.getInt("swipeTime", 500)
        set(time) {
            sharedPreferences.edit().putInt("swipeTime", time).apply()
        }
    var repeatCount: Int
        get() = sharedPreferences.getInt("repeatCount", 0)
        set(count) {
            sharedPreferences.edit().putInt("repeatCount", count).apply()
        }

    // scondes
    var durationTime: Long
        get() = sharedPreferences.getLong("durationTime", 0)
        set(time) {
            sharedPreferences.edit().putLong("durationTime", time).apply()
        }

    companion object {
        private var appConfig: AppConfig? = null
        fun instance(): AppConfig {
            if (appConfig == null) {
                appConfig = AppConfig(context)
            }
            return appConfig!!
        }
    }
}