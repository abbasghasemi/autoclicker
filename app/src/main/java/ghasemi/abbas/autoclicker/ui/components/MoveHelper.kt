package ghasemi.abbas.autoclicker.ui.components

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import ghasemi.abbas.autoclicker.utils.AndroidUtils
import kotlin.math.abs

open class MoveHelper(context: Context) : View(context) {
    private var clickListener: OnClickListener? = null
    private var longClickListener: OnLongClickListener? = null
    private var initPx: Int = 0
    private var initPy: Int = 0
    private var initX: Float = 0f
    private var initY: Float = 0f
    private var initZ: Int = 0
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    var pointViewMoveListener: PointViewMoveListener? = null
    private var startTouchTime = 0L
    private var canReportClick = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (parent != null && event.action == MotionEvent.ACTION_DOWN) {
            parent.requestDisallowInterceptTouchEvent(true)
        }
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (pointViewMoveListener == null) return false
            startTouchTime = System.currentTimeMillis()
            val onTouchEvent = super.onTouchEvent(event)
            val params = pointViewMoveListener!!.findLayoutParams(this) ?: return onTouchEvent
            initPx = params.x
            initPy = params.y
            initX = event.rawX
            initY = event.rawY
            initZ = width.coerceAtLeast(height) / -2
            if (false && parent is ViewGroup) {
                screenWidth = (parent as ViewGroup).width
                screenHeight = (parent as ViewGroup).height
            } else {
                screenWidth = resources.displayMetrics.widthPixels
                screenHeight = resources.displayMetrics.heightPixels
            }
            canReportClick = true
            return true
        } else if (event.action == MotionEvent.ACTION_MOVE) {
            if (canReportClick && (abs(event.rawX - initX) > AndroidUtils.dpf(1f) ||
                abs(event.rawY - initY) > AndroidUtils.dpf(1f))) {
                canReportClick = false
            }
            if (canReportClick) {
                if (System.currentTimeMillis() - startTouchTime > 500) {
                    canReportClick = false
                    if (longClickListener != null && longClickListener!!.onLongClick(this)) {
                        AndroidUtils.longVibrator()
                    }
                    return false
                }
            }
            pointViewMoveListener?.onMove(
                this,
                initPx + event.rawX.toInt() - initX.toInt(),
                initPy + event.rawY.toInt() - initY.toInt(),
                initZ,
                screenWidth,
                screenHeight
            )
            return true
        } else if (event.action == MotionEvent.ACTION_CANCEL || event.action == MotionEvent.ACTION_UP) {
            if (canReportClick) {
                if (System.currentTimeMillis() - startTouchTime in 21..499) {
                    canReportClick = false
                    clickListener?.onClick(this)
                    return false
                }
            }
        }
        return false
    }


    override fun setOnLongClickListener(l: OnLongClickListener?) {
        longClickListener = l
    }

    override fun setOnClickListener(l: OnClickListener?) {
        clickListener = l
    }

    interface PointViewMoveListener {
        fun findLayoutParams(view: View): WindowManager.LayoutParams?
        fun onMove(view: View, x: Int, y: Int, z: Int, screenWidth: Int, screenHeight: Int)
    }
}