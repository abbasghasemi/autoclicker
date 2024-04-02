package ghasemi.abbas.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.content.Intent
import android.graphics.Path
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import ghasemi.abbas.autoclicker.NotificationCenter.NotificationCenterDelegate

open class AppAccessibilityService : AccessibilityService(), NotificationCenterDelegate {
    private val TAG = "AppAccessibilityService"
    private val gestureDescriptionList = ArrayList<GestureDescription>()
    private val gestureResultCallbackList = ArrayList<GestureResultCallback?>()
    private var serviceHelper: AppServiceHelper? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent) {}
    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceHelper =
            AppServiceHelper(this, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
        NotificationCenter.instance().addObserver(this, NotificationCenter.accessibilityClick)
        NotificationCenter.instance().addObserver(this, NotificationCenter.accessibilitySwipe)
        NotificationCenter.instance().addObserver(this, NotificationCenter.accessibilityClear)
        NotificationCenter.instance().addObserver(this, NotificationCenter.appServiceStart)
        NotificationCenter.instance().addObserver(this, NotificationCenter.appServiceStop)
        NotificationCenter.instance().postNotificationName(NotificationCenter.accessibilityConnected)
    }

    override fun onInterrupt() {}
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onUnbind(intent: Intent): Boolean {
//        destroy()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        destroy()
        super.onDestroy()
    }

    private fun destroy() {
        NotificationCenter.instance().removeObserver(this, NotificationCenter.accessibilityClick)
        NotificationCenter.instance().removeObserver(this, NotificationCenter.accessibilitySwipe)
        NotificationCenter.instance().removeObserver(this, NotificationCenter.accessibilityClear)
        NotificationCenter.instance().removeObserver(this, NotificationCenter.appServiceStart)
        NotificationCenter.instance().removeObserver(this, NotificationCenter.appServiceStop)
        serviceHelper?.onDestroy()
    }

    private fun click(x: Int, y: Int, longTime: Long) {
        val path = Path()
        path.moveTo(if (x < 0) 0f else x.toFloat(), if (y < 0) 0f else y.toFloat())
        val build =
            GestureDescription.Builder().addStroke(StrokeDescription(path, 0, longTime)).build()
        val callback = GestureResultCallback()
        dispatchGesture(build, callback, null)
        gestureDescriptionList.add(build)
        gestureResultCallbackList.add(callback)
    }

    private fun swipe(fromX: Float, fromY: Float, toX: Float, toY: Float, duration: Long) {
        val path = Path()
        path.moveTo(if (fromX < 0.0f) 0.0f else fromX, if (fromY < 0.0f) 0.0f else fromY)
        path.lineTo(if (toX < 0.0f) 0.0f else toX, if (toY < 0.0f) 0.0f else toY)
        val build =
            GestureDescription.Builder().addStroke(StrokeDescription(path, 0, duration)).build()
        val callback = GestureResultCallback()
        dispatchGesture(build, callback, null)
        gestureDescriptionList.add(build)
        gestureResultCallbackList.add(callback)
    }

    private fun clear() {
        try {
            if (gestureResultCallbackList.size > 0) {
                val size = gestureDescriptionList.size - 1
                if (size >= 0) {
                    var i = 0
                    while (true) {
                        if (gestureResultCallbackList[i] != null) {
                            gestureResultCallbackList[i]!!.onCancelled(gestureDescriptionList[i])
                            gestureResultCallbackList[i]!!.onCompleted(gestureDescriptionList[i])
                        }
                        if (i == size) {
                            break
                        }
                        i++
                    }
                }
                gestureDescriptionList.clear()
                gestureResultCallbackList.clear()
            }
        } catch (e: Exception) {
            //
        }
    }

    override fun didReceivedNotification(event: Int, vararg args: Any) {
        when (event) {
            NotificationCenter.accessibilityClick -> {
                click(args[0] as Int, args[1] as Int, args[2] as Long)
            }

            NotificationCenter.accessibilitySwipe -> {
                swipe(
                    args[0] as Float,
                    args[1] as Float,
                    args[2] as Float,
                    args[3] as Float,
                    args[4] as Long
                )
            }

            NotificationCenter.accessibilityClear -> {
                clear()
            }

            NotificationCenter.appServiceStart -> {
                if (args.isEmpty()) {
                    serviceHelper?.showSettings()
                } else  {
                    serviceHelper?.showSettings(args[0] as AppServiceHelper.ScriptsConfig)
                }
            }

            NotificationCenter.appServiceStop -> {
                serviceHelper?.dismissSettings()
            }
        }
    }

    inner class GestureResultCallback : AccessibilityService.GestureResultCallback() {

        override fun onCompleted(gestureDescription: GestureDescription) {
            super.onCompleted(gestureDescription)
            Log.i(TAG, "onCompleted: ")
            val index = gestureResultCallbackList.indexOf(this)
            if (index > 0) {
                gestureResultCallbackList.removeAt(index)
                gestureDescriptionList.removeAt(index)
            }
        }

        override fun onCancelled(gestureDescription: GestureDescription) {
            super.onCancelled(gestureDescription)
            Log.i(TAG, "onCancelled: ")
            val index = gestureResultCallbackList.indexOf(this)
            if (index > 0) {
                gestureResultCallbackList.removeAt(index)
                gestureDescriptionList.removeAt(index)
            }
            gestureDescription.getStroke(0).path.close()
        }
    }
}