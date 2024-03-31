package ghasemi.abbas.autoclicker.utils

import android.os.Build
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class BackController {

    private lateinit var onInvoke: OnInvoke
    private var backController33: BackController33? = null

    fun registerOnBackInvokedCallback(activity: AppCompatActivity, onInvoke: OnInvoke) {
        this.onInvoke = onInvoke
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backController33 = BackController33()
            backController33!!.registerOnBackInvokedCallback(activity, onInvoke)
        } else {
            activity.onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    onInvoke.onBackInvoked()
                }
            })
        }
    }

    fun unregisterOnBackInvokedCallback(activity: AppCompatActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backController33!!.unregisterOnBackInvokedCallback(activity)
        } else {
            activity.onBackPressedDispatcher.dispatchOnBackCancelled()
        }
    }

    interface OnInvoke {

        fun onBackInvoked()

    }

}