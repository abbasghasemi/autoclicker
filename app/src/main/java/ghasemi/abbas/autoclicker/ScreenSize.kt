package ghasemi.abbas.autoclicker

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.view.WindowManager

object ScreenSize {
    fun of(context: Context): Pair<Int, Int> {
        val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = manager.maximumWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val size = Point()
            @Suppress("DEPRECATION")
            manager.defaultDisplay.getRealSize(size)
            size.x to size.y
        }
    }
}
