package ghasemi.abbas.autoclicker.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.core.graphics.ColorUtils
import ghasemi.abbas.autoclicker.utils.AndroidUtils
import kotlin.math.abs

class TouchesView(context: Context) : View(context) {
    private val paths = ArrayList<PathHolder>()
    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val width = AndroidUtils.dpf(5f)
    private val live = 600f
    private val interpolator = AccelerateInterpolator()

    init {
        setBackgroundColor(Color.WHITE)
    }

    override fun onDraw(canvas: Canvas) {
        val tm = System.currentTimeMillis()
        var i = 0
        var j = paths.size
        while (i < j) {
            val path = paths[i]
            val df = tm - path.start
            if (df > live) {
                paths.removeAt(i)
                j--
                continue
            }
            val x = interpolator.getInterpolation(df / live) * live
            paint.strokeWidth = width * x / 2 * 0.07f
            paint.color =
                ColorUtils.setAlphaComponent(path.color, ((live - df) * 255 / live).toInt())
            paint.strokeCap = path.cap
            canvas.drawPoint(path.x, path.y, paint)
            i++
        }
        if (paths.isNotEmpty()) {
            invalidate()
        }
    }

    private var color: Int = 0
    private var cap: Paint.Cap = Paint.Cap.BUTT

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                color = intArrayOf(
                    Color.BLACK,
                    Color.GREEN,
                    Color.BLUE,
                    Color.CYAN,
                    Color.RED,
                    0xffF57F17.toInt(),
                    0xff5D4037.toInt(),
                    0xff37474F.toInt(),
                    0xff00695C.toInt(),
                    Color.MAGENTA,
                ).random()
                cap = arrayOf(Paint.Cap.ROUND, Paint.Cap.ROUND).random()
                for (i in 0..<event.pointerCount) {
                    paths.add(
                        PathHolder(
                            event.getPointerId(i),
                            event.getX(i),
                            event.getY(i),
                            cap,
                            color
                        )
                    )
                }
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {

                for (i in 0..<event.pointerCount) {
                    val id = event.getPointerId(i)
                    var path: PathHolder? = null
                    for (j in paths.size - 1 downTo 0) {
                        if (paths[j].id == id) {
                            path = paths[j]
                            break
                        }
                    }
                    if (path == null ||
                        abs(path.x - event.getX(i)) >= width ||
                        abs(path.y - event.getY(i)) >= width
                    ) {
                        paths.add(
                            PathHolder(
                                event.getPointerId(i),
                                event.getX(i),
                                event.getY(i),
                                cap,
                                color,
                            )
                        )
                        if (paths.size == 1) {
                            invalidate()
                        }
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {

            }
        }
        return true
    }

    data class PathHolder(
        val id: Int,
        val x: Float,
        val y: Float,
        val cap: Paint.Cap,
        val color: Int,
        val start: Long = System.currentTimeMillis(),
    )
}