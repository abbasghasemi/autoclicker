package ghasemi.abbas.autoclicker.utils

import android.os.Build
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class BackController33 : OnBackInvokedCallback {

    private lateinit var onInvoke: BackController.OnInvoke

    fun registerOnBackInvokedCallback(activity: AppCompatActivity, onInvoke: BackController.OnInvoke) {
        this.onInvoke = onInvoke
        activity.onBackInvokedDispatcher.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            this
        )
    }

    fun unregisterOnBackInvokedCallback(activity: AppCompatActivity) {
        activity.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(this)
    }

    override fun onBackInvoked() {
        onInvoke.onBackInvoked()
    }

}