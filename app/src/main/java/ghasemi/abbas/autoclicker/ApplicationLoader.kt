package ghasemi.abbas.autoclicker

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

class ApplicationLoader : Application() {
    override fun onCreate() {
        super.onCreate()
        context = applicationCreateConfigurationContext(this)
        if (AppConfig.instance().isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        var context: Context? = null
        val applicationHandler = Handler(Looper.getMainLooper())
        fun applicationCreateConfigurationContext(context: Context?): Context {
            if (ApplicationLoader.context == null) {
                ApplicationLoader.context = context
            }
            val locale = Locale("fa")
            val configuration = Configuration(
                context!!.resources.configuration
            )
            configuration.setLocale(locale)
            return context.createConfigurationContext(configuration)
        }
    }
}