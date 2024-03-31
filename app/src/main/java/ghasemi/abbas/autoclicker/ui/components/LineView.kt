package ghasemi.abbas.autoclicker.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import android.view.WindowManager
import ghasemi.abbas.autoclicker.utils.AndroidUtils

class LineView(context: Context) : View(context) {

    private val location1 = IntArray(2)
    private val location2 = IntArray(2)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = Rect()

    var view1: PointView? = null
    var view2: PointView? = null
    var params1: WindowManager.LayoutParams? = null
    var params2: WindowManager.LayoutParams? = null
    var xFromRequest = 0f
    var xToRequest = 0f
    var yFromRequest = 0f
    var yToRequest = 0f

    init {
        paint.style = Paint.Style.STROKE
        paint.color = Color.argb(150, 0, 255, 0)
        paint.alpha = 150
        paint.strokeWidth = AndroidUtils.dpf(10f)
    }

    fun onAddView(
        view1: PointView, params1: WindowManager.LayoutParams,
        view2: PointView, params2: WindowManager.LayoutParams?
    ) {
        this.view1 = view1
        this.params1 = params1
        this.view2 = view2
        this.params2 = params2
       invalidate()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        invalidate()
    }

    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (view1 != null && view2 != null) {
            view1!!.getLocationOnScreen(location1)
            view2!!.getLocationOnScreen(location2)
            view1!!.getWindowVisibleDisplayFrame(rect)

            val widthV1 = view1!!.width / 2f
            val heightV1 = view1!!.height / 2f
            val widthV2 = view2!!.width / 2f
            val heightV2 = view2!!.height / 2f

            xFromRequest = location1[0] + widthV1
            yFromRequest = location1[1] + heightV1
            xToRequest = location2[0] + widthV2
            yToRequest = location2[1] + heightV2

            val width = location1[0] + widthV1 - rect.left
            val height = location1[1] + heightV1 - rect.top
            val width2 = location2[0] + widthV2 - rect.left

            canvas.drawLine(width, height, width2, location2[1] + heightV2 - rect.top, paint)
        }
    }
}