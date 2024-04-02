package ghasemi.abbas.autoclicker

import android.util.SparseArray
import androidx.annotation.IntDef
import androidx.annotation.UiThread
import ghasemi.abbas.autoclicker.utils.AndroidUtils.runOnUIThread

class NotificationCenter {
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(accessibilityConnected,accessibilityClick, accessibilitySwipe, accessibilityClear, appServiceStart, appServiceStop,appServiceToggle,scriptsSaved)
    annotation class EventID

    private val observers = SparseArray<ArrayList<NotificationCenterDelegate>>()

    fun postNotificationName(@EventID event: Int, vararg args: Any) {
        runOnUIThread({ postNotificationNameInterval(event, *args) })
    }

    @UiThread
    fun postNotificationNameInterval(@EventID event: Int, vararg args: Any) {
        runOnUIThread({
            val objects = observers[event]
            if (objects != null && objects.isNotEmpty()) {
                for (a in objects.indices) {
                    val obj = objects[a]
                    obj.didReceivedNotification(event, *args)
                }
            }
        })
    }

    fun addObserver(observer: NotificationCenterDelegate, @EventID event: Int) {
        var objects = observers[event]
        if (objects == null) {
            observers.put(event, ArrayList<NotificationCenterDelegate>().also { objects = it })
        }
        if (objects!!.contains(observer)) {
            return
        }
        objects!!.add(observer)
    }

    fun removeObserver(observer: NotificationCenterDelegate, @EventID event: Int) {
        val objects = observers[event]
        objects?.remove(observer)
    }

    interface NotificationCenterDelegate {
        fun didReceivedNotification(@EventID event: Int, vararg args: Any)
    }

    companion object {
        const val accessibilityConnected = 0
        const val accessibilityClick = 1
        const val accessibilitySwipe = 2
        const val accessibilityClear = 3
        const val appServiceStart = 4
        const val appServiceStop = 5
        const val appServiceToggle = 6
        const val scriptsSaved = 7
        private var notificationCenter: NotificationCenter? = null

        fun instance(): NotificationCenter {
            if (notificationCenter == null) {
                notificationCenter = NotificationCenter()
            }
            return notificationCenter!!
        }
    }
}