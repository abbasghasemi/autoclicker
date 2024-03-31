package ghasemi.abbas.autoclicker.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import kotlin.math.round


class PointView constructor(context: Context) :
    MoveHelper(context) {
    var number: Int = 0
    var isEnd: Boolean? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()
    private var rect = Rect()

    init {
//        paint.typeface = Typeface.DEFAULT_BOLD
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padding: Float = width * if (isEnd == true) 0.1f else 0.09f

        rectF.apply {
            left = padding
            top = padding
            right = width.toFloat() - padding
            bottom = height.toFloat() - padding
        }
        paint.apply {
            strokeWidth = padding
            textSize = width / 1.5f
            color = Color.argb(150, 0, 255, 0)
            style = Paint.Style.STROKE
        }
        canvas.drawArc(rectF, 0f, 360f, false, paint)
        paint.style = Paint.Style.FILL
        if (isEnd == true) {
            val length = round(height * 0.3f).toInt()
            drawTriangle(
                canvas, width / 2 - length / 2, height / 2 - length / 3,
                length,
                length,
                true
            )
        } else {
            canvas.drawCircle(width / 2f, height / 2f, height * 0.14f, paint)
        }
        if (number > 0 && isEnd != true) {
            paint.color = Color.BLUE
            paint.getTextBounds(number.toString(), 0, number.toString().length, rect)
            canvas.drawText(
                number.toString(),
                width / 2f - padding / 2 - rect.width() / 2f,
                height / 2f + rect.height() / 2f,
                paint
            )
        }
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo?) {
        super.onInitializeAccessibilityNodeInfo(info)
        info!!.text = when (isEnd) {
            null -> "Click widget $number"
            false -> "Start swipe widget $number"
            else -> "end swipe widget $number"
        }
        info.className = Button::class.java.name
        info.isClickable = true
    }

    private fun drawTriangle(
        canvas: Canvas,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        inverted: Boolean
    ) {
        val p1 = Point(x, y)
        val pointX = x + width / 2
        val pointY = if (inverted) y + height else y - height
        val p2 = Point(pointX, pointY)
        val p3 = Point(x + width, y)
        val path = Path()
        path.fillType = Path.FillType.EVEN_ODD
        path.moveTo(p1.x.toFloat(), p1.y.toFloat())
        path.lineTo(p2.x.toFloat(), p2.y.toFloat())
        path.lineTo(p3.x.toFloat(), p3.y.toFloat())
        path.close()
        canvas.drawPath(path, paint)
    }

    fun drawTriangles(canvas: Canvas, padding: Float) {
        val w = width - padding * 2
        val h = height - padding * 2
        val verts = floatArrayOf(
            w, h,
            w + padding * 2, h + padding * 2,
            w + padding * 2, padding * 2
        )
        canvas.drawVertices(
            Canvas.VertexMode.TRIANGLES, verts.size, verts, 0, null, 0,
            intArrayOf(
                Color.GREEN, Color.BLUE, Color.RED,
                Color.BLACK, Color.YELLOW, Color.CYAN,
            ), 0, null, 0, 0, paint
        )
    }

}