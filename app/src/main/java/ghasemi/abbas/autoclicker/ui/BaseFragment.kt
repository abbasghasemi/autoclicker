package ghasemi.abbas.autoclicker.ui

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.CallSuper

open class BaseFragment() {

    lateinit var root: FrameLayout
    var context: Context? = null
        private set
    var isFinish: Boolean = false
        private set
    var parentLayout:LauncherActivity? = null

    @CallSuper
    open fun onCreateView(context: Context) {
        if (!::root.isInitialized) {
            root = FrameLayout(context)
            root.setBackgroundColor(Color.WHITE)
            root.isFocusable = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                root.focusable = View.FOCUSABLE
            }
        }
        this.context = context
    }

    @CallSuper
    open fun onDestroyView() {
        if (isFinish || parentLayout == null) {
            return
        }
        isFinish = true
        context = null
    }

    open fun onPause() {

    }

    open fun onBackInvoked(): Boolean {
        return true
    }

    open fun finishFragment() {
        if (isFinish || parentLayout == null) {
            return
        }
        onPause()
        onDestroyView()
        parentLayout?.closeLastFragment()
    }

}