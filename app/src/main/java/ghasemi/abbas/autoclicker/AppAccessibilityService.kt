package ghasemi.abbas.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.content.Intent
import android.graphics.Path
import android.util.Log
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import ghasemi.abbas.autoclicker.NotificationCenter.NotificationCenterDelegate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

open class AppAccessibilityService : AccessibilityService(), NotificationCenterDelegate {
    companion object {
        @Volatile private var activeService: AppAccessibilityService? = null
        @Volatile var foregroundPackage: String = ""
            private set
        fun isConnected() = activeService != null

        fun hasVisibleText(text: String): Boolean {
            val service = activeService ?: return false
            var visible = false
            val query = Runnable {
                visible = runCatching {
                    val roots = service.windows.mapNotNull { it.root }
                        .ifEmpty { listOfNotNull(service.rootInActiveWindow) }
                    roots.any { root ->
                        root.packageName?.toString() != service.packageName &&
                            root.findAccessibilityNodeInfosByText(text).any { node ->
                                node.isVisibleToUser &&
                                    (node.text?.contains(text, ignoreCase = true) == true ||
                                     node.contentDescription?.contains(text, ignoreCase = true) == true)
                            }
                    }
                }.getOrDefault(false)
            }
            if (Looper.myLooper() == Looper.getMainLooper()) query.run()
            else {
                val done = CountDownLatch(1)
                Handler(Looper.getMainLooper()).post { query.run(); done.countDown() }
                done.await(500, TimeUnit.MILLISECONDS)
            }
            return visible
        }
    }
    private val TAG = "AppAccessibilityService"
    private val gestureDescriptionList = ArrayList<GestureDescription>()
    private val gestureResultCallbackList = ArrayList<GestureResultCallback?>()
    private var serviceHelper: AppServiceHelper? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val currentPackage = event.packageName?.toString().orEmpty()
        if (currentPackage.isNotBlank() && currentPackage != packageName) {
            foregroundPackage = currentPackage
        }
    }
    override fun onServiceConnected() {
        super.onServiceConnected()
        activeService = this
        foregroundPackage = rootInActiveWindow?.packageName?.toString().orEmpty()
        serviceHelper =
            AppServiceHelper(this, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
        NotificationCenter.instance().addObserver(this, NotificationCenter.accessibilityClick)
        NotificationCenter.instance().addObserver(this, NotificationCenter.accessibilitySwipe)
        NotificationCenter.instance().addObserver(this, NotificationCenter.accessibilityClear)
        NotificationCenter.instance().addObserver(this, NotificationCenter.appServiceStart)
        NotificationCenter.instance().addObserver(this, NotificationCenter.appServiceStop)
        NotificationCenter.instance().addObserver(this, NotificationCenter.previewStep)
        NotificationCenter.instance().postNotificationName(NotificationCenter.accessibilityConnected)
    }

    override fun onInterrupt() {}
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
        if (activeService === this) activeService = null
        foregroundPackage = ""
        NotificationCenter.instance().removeObserver(this, NotificationCenter.accessibilityClick)
        NotificationCenter.instance().removeObserver(this, NotificationCenter.accessibilitySwipe)
        NotificationCenter.instance().removeObserver(this, NotificationCenter.accessibilityClear)
        NotificationCenter.instance().removeObserver(this, NotificationCenter.appServiceStart)
        NotificationCenter.instance().removeObserver(this, NotificationCenter.appServiceStop)
        NotificationCenter.instance().removeObserver(this, NotificationCenter.previewStep)
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

    private fun swipe(fromX: Float, fromY: Float, toX: Float, toY: Float,
                      duration: Long, mode: Int, curvePercent: Int, waypoints: String) {
        val path = Path()
        path.moveTo(if (fromX < 0.0f) 0.0f else fromX, if (fromY < 0.0f) 0.0f else fromY)
        if (mode == 4) {
            val (screenWidth, screenHeight) = ScreenSize.of(this)
            GestureWaypoints.parse(waypoints, screenWidth.toFloat(),
                screenHeight.toFloat()).forEach { (x, y) ->
                path.lineTo(x, y)
            }
            path.lineTo(toX.coerceAtLeast(0f), toY.coerceAtLeast(0f))
        } else if (mode == 1) {
            val dx = toX - fromX
            val dy = toY - fromY
            val bend = curvePercent / 200f
            path.quadTo((fromX + toX) / 2 - dy * bend,
                (fromY + toY) / 2 + dx * bend, toX, toY)
        } else {
            path.lineTo(toX.coerceAtLeast(0f), toY.coerceAtLeast(0f))
        }
        val builder = GestureDescription.Builder()
            .addStroke(StrokeDescription(path, 0, duration))
        if (mode == 2 || mode == 3) {
            val second = Path()
            if (mode == 2) {
                second.moveTo((2 * toX - fromX).coerceAtLeast(0f),
                    (2 * toY - fromY).coerceAtLeast(0f))
                second.lineTo(toX.coerceAtLeast(0f), toY.coerceAtLeast(0f))
            } else {
                second.moveTo(fromX.coerceAtLeast(0f), fromY.coerceAtLeast(0f))
                second.lineTo((2 * fromX - toX).coerceAtLeast(0f),
                    (2 * fromY - toY).coerceAtLeast(0f))
            }
            builder.addStroke(StrokeDescription(second, 0, duration))
        }
        val build = builder.build()
        val callback = GestureResultCallback()
        dispatchGesture(build, callback, null)
        gestureDescriptionList.add(build)
        gestureResultCallbackList.add(callback)
    }

    private fun clear() {
        gestureDescriptionList.clear()
        gestureResultCallbackList.clear()
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
                    args[4] as Long,
                    args.getOrNull(5) as? Int ?: 0,
                    args.getOrNull(6) as? Int ?: 0,
                    args.getOrNull(7) as? String ?: ""
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
            NotificationCenter.previewStep -> {
                val step = args[0] as AppServiceHelper.Widget
                val script = args[1] as AppServiceHelper.ScriptsConfig
                Handler(Looper.getMainLooper()).postDelayed({
                    if (foregroundPackage != script.targetPackage || script.targetPackage.isBlank()) return@postDelayed
                    val (screenWidth, screenHeight) = ScreenSize.of(this)
                    val scaleX = if (script.relativeCoordinates && script.referenceWidth > 0)
                        screenWidth.toFloat() / script.referenceWidth else 1f
                    val scaleY = if (script.relativeCoordinates && script.referenceHeight > 0)
                        screenHeight.toFloat() / script.referenceHeight else 1f
                    val center = ghasemi.abbas.autoclicker.utils.AndroidUtils.dp(20f)
                    val x1 = step.x1 * scaleX + center
                    val y1 = step.y1 * scaleY + center
                    if (step.type == 0) click(x1.toInt(), y1.toInt(), step.longDuration.toLong().coerceAtLeast(1))
                    else swipe(x1, y1, step.x2 * scaleX + center, step.y2 * scaleY + center,
                        step.swipeDuration.toLong().coerceAtLeast(1), step.gestureMode,
                        step.curvePercent, step.waypoints)
                }, 3000)
            }
        }
    }

    inner class GestureResultCallback : AccessibilityService.GestureResultCallback() {

        override fun onCompleted(gestureDescription: GestureDescription) {
            super.onCompleted(gestureDescription)
            Log.i(TAG, "onCompleted: ")
            val index = gestureResultCallbackList.indexOf(this)
            if (index >= 0) {
                gestureResultCallbackList.removeAt(index)
                gestureDescriptionList.removeAt(index)
            }
        }

        override fun onCancelled(gestureDescription: GestureDescription) {
            super.onCancelled(gestureDescription)
            Log.i(TAG, "onCancelled: ")
            val index = gestureResultCallbackList.indexOf(this)
            if (index >= 0) {
                gestureResultCallbackList.removeAt(index)
                gestureDescriptionList.removeAt(index)
            }
            gestureDescription.getStroke(0).path.close()
        }
    }
}
