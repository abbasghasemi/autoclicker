package ghasemi.abbas.autoclicker.utils

import android.annotation.SuppressLint
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.view.ViewCompat
import ghasemi.abbas.autoclicker.utils.AndroidUtils.dp
import ghasemi.abbas.autoclicker.utils.AndroidUtils.isRTL

object LayoutHelper {
    const val MATCH_PARENT = -1f
    const val WRAP_CONTENT = -2f
    private fun getSize(size: Float): Int {
        return (if (size < 0) size else dp(size)).toInt()
    }

    //region Gravity
    private fun getAbsoluteGravity(gravity: Int): Int {
        return Gravity.getAbsoluteGravity(
            gravity,
            if (isRTL) ViewCompat.LAYOUT_DIRECTION_RTL else ViewCompat.LAYOUT_DIRECTION_LTR
        )
    }

    @get:SuppressLint("RtlHardcoded")
    val absoluteGravityStart: Int
        get() = if (isRTL) Gravity.RIGHT else Gravity.LEFT

    @get:SuppressLint("RtlHardcoded")
    val absoluteGravityEnd: Int
        get() = if (isRTL) Gravity.LEFT else Gravity.RIGHT

    //endregion
    //region ScrollView
    fun createScroll(width: Float, height: Float, gravity: Int): FrameLayout.LayoutParams {
        return FrameLayout.LayoutParams(
            getSize(width),
            getSize(height),
            gravity
        )
    }

    fun createScroll(
        width: Float,
        height: Float,
        gravity: Int,
        leftMargin: Float,
        topMargin: Float,
        rightMargin: Float,
        bottomMargin: Float
    ): FrameLayout.LayoutParams {
        val layoutParams =
            FrameLayout.LayoutParams(getSize(width), getSize(height), gravity)
        layoutParams.leftMargin = dp(leftMargin)
        layoutParams.topMargin = dp(topMargin)
        layoutParams.rightMargin = dp(rightMargin)
        layoutParams.bottomMargin = dp(bottomMargin)
        return layoutParams
    }

    //endregion
    //region FrameLayout
    fun createFrame(
        width: Float,
        height: Float,
        gravity: Int,
        leftMargin: Float,
        topMargin: Float,
        rightMargin: Float,
        bottomMargin: Float
    ): FrameLayout.LayoutParams {
        val layoutParams =
            FrameLayout.LayoutParams(getSize(width), getSize(height), gravity)
        layoutParams.setMargins(dp(leftMargin), dp(topMargin), dp(rightMargin), dp(bottomMargin))
        return layoutParams
    }

    fun createFrameMarginPx(
        width: Float,
        height: Float,
        gravity: Int,
        leftMarginPx: Int,
        topMarginPx: Int,
        rightMarginPx: Int,
        bottomMarginPx: Int
    ): FrameLayout.LayoutParams {
        val layoutParams =
            FrameLayout.LayoutParams(getSize(width), getSize(height), gravity)
        layoutParams.setMargins(leftMarginPx, topMarginPx, rightMarginPx, bottomMarginPx)
        return layoutParams
    }

    fun createFrame(width: Float, height: Float): FrameLayout.LayoutParams {
        return FrameLayout.LayoutParams(getSize(width), getSize(height))
    }

    fun createFrame(width: Float, height: Float, gravity: Int): FrameLayout.LayoutParams {
        return FrameLayout.LayoutParams(getSize(width), getSize(height), gravity)
    }

    fun createFrameRelatively(
        width: Float,
        height: Float,
        gravity: Int,
        startMargin: Float,
        topMargin: Float,
        endMargin: Float,
        bottomMargin: Float
    ): FrameLayout.LayoutParams {
        val layoutParams =
            FrameLayout.LayoutParams(getSize(width), getSize(height), getAbsoluteGravity(gravity))
        layoutParams.leftMargin = dp(if (isRTL) endMargin else startMargin)
        layoutParams.topMargin = dp(topMargin)
        layoutParams.rightMargin = dp(if (isRTL) startMargin else endMargin)
        layoutParams.bottomMargin = dp(bottomMargin)
        return layoutParams
    }

    fun createFrameRelatively(width: Float, height: Float, gravity: Int): FrameLayout.LayoutParams {
        return FrameLayout.LayoutParams(
            getSize(width),
            getSize(height),
            getAbsoluteGravity(gravity)
        )
    }

    //endregion
    //region RelativeLayout
    fun createRelative(
        width: Float,
        height: Float,
        leftMargin: Float,
        topMargin: Float,
        rightMargin: Float,
        bottomMargin: Float,
        alignParent: Int,
        alignRelative: Int,
        anchorRelative: Int
    ): RelativeLayout.LayoutParams {
        val layoutParams = RelativeLayout.LayoutParams(getSize(width), getSize(height))
        if (alignParent >= 0) {
            layoutParams.addRule(alignParent)
        }
        if (alignRelative >= 0 && anchorRelative >= 0) {
            layoutParams.addRule(alignRelative, anchorRelative)
        }
        layoutParams.leftMargin = dp(leftMargin)
        layoutParams.topMargin = dp(topMargin)
        layoutParams.rightMargin = dp(rightMargin)
        layoutParams.bottomMargin = dp(bottomMargin)
        return layoutParams
    }

    fun createRelative(
        width: Float,
        height: Float,
        leftMargin: Float,
        topMargin: Float,
        rightMargin: Float,
        bottomMargin: Float
    ): RelativeLayout.LayoutParams {
        return createRelative(
            width,
            height,
            leftMargin,
            topMargin,
            rightMargin,
            bottomMargin,
            -1,
            -1,
            -1
        )
    }

    fun createRelative(
        width: Float,
        height: Float,
        leftMargin: Float,
        topMargin: Float,
        rightMargin: Float,
        bottomMargin: Float,
        alignParent: Int
    ): RelativeLayout.LayoutParams {
        return createRelative(
            width,
            height,
            leftMargin,
            topMargin,
            rightMargin,
            bottomMargin,
            alignParent,
            -1,
            -1
        )
    }

    fun createRelative(
        width: Float,
        height: Float,
        leftMargin: Float,
        topMargin: Float,
        rightMargin: Float,
        bottomMargin: Float,
        alignRelative: Int,
        anchorRelative: Int
    ): RelativeLayout.LayoutParams {
        return createRelative(
            width,
            height,
            leftMargin,
            topMargin,
            rightMargin,
            bottomMargin,
            -1,
            alignRelative,
            anchorRelative
        )
    }

    fun createRelative(
        width: Float,
        height: Float,
        alignParent: Int,
        alignRelative: Int,
        anchorRelative: Int
    ): RelativeLayout.LayoutParams {
        return createRelative(
            width,
            height,
            0f,
            0f,
            0f,
            0f,
            alignParent,
            alignRelative,
            anchorRelative
        )
    }

    fun createRelative(width: Float, height: Float): RelativeLayout.LayoutParams {
        return createRelative(width, height, 0f, 0f, 0f, 0f, -1, -1, -1)
    }

    fun createRelative(width: Float, height: Float, alignParent: Int): RelativeLayout.LayoutParams {
        return createRelative(width, height, 0f, 0f, 0f, 0f, alignParent, -1, -1)
    }

    fun createRelative(
        width: Float,
        height: Float,
        alignRelative: Int,
        anchorRelative: Int
    ): RelativeLayout.LayoutParams {
        return createRelative(
            width,
            height,
            0f,
            0f,
            0f,
            0f,
            -1,
            alignRelative,
            anchorRelative
        )
    }

    //endregion
    //region LinearLayout
    fun createLinear(
        width: Float,
        height: Float,
        weight: Float,
        gravity: Int,
        leftMargin: Float,
        topMargin: Float,
        rightMargin: Float,
        bottomMargin: Float
    ): LinearLayout.LayoutParams {
        val layoutParams =
            LinearLayout.LayoutParams(getSize(width), getSize(height), weight)
        layoutParams.setMargins(
            dp(leftMargin),
            dp(topMargin),
            dp(rightMargin),
            dp(bottomMargin)
        )
        layoutParams.gravity = gravity
        return layoutParams
    }

    fun createLinear(
        width: Float,
        height: Float,
        weight: Float,
        leftMargin: Float,
        topMargin: Float,
        rightMargin: Float,
        bottomMargin: Float
    ): LinearLayout.LayoutParams {
        val layoutParams =
            LinearLayout.LayoutParams(getSize(width), getSize(height), weight)
        layoutParams.setMargins(
            dp(leftMargin),
            dp(topMargin),
            dp(rightMargin),
            dp(bottomMargin)
        )
        return layoutParams
    }

    fun createLinear(
        width: Float,
        height: Float,
        gravity: Int,
        leftMargin: Float,
        topMargin: Float,
        rightMargin: Float,
        bottomMargin: Float
    ): LinearLayout.LayoutParams {
        val layoutParams =
            LinearLayout.LayoutParams(getSize(width), getSize(height))
        layoutParams.setMargins(
            dp(leftMargin),
            dp(topMargin),
            dp(rightMargin),
            dp(bottomMargin)
        )
        layoutParams.gravity = gravity
        return layoutParams
    }

    fun createLinear(
        width: Float,
        height: Float,
        leftMargin: Float,
        topMargin: Float,
        rightMargin: Float,
        bottomMargin: Float
    ): LinearLayout.LayoutParams {
        val layoutParams =
            LinearLayout.LayoutParams(getSize(width), getSize(height))
        layoutParams.setMargins(dp(leftMargin), dp(topMargin), dp(rightMargin), dp(bottomMargin))
        return layoutParams
    }

    fun createLinear(
        width: Float,
        height: Float,
        weight: Float,
        gravity: Int
    ): LinearLayout.LayoutParams {
        val layoutParams =
            LinearLayout.LayoutParams(getSize(width), getSize(height), weight)
        layoutParams.gravity = gravity
        return layoutParams
    }

    fun createLinear(width: Float, height: Float, gravity: Int): LinearLayout.LayoutParams {
        val layoutParams =
            LinearLayout.LayoutParams(getSize(width), getSize(height))
        layoutParams.gravity = gravity
        return layoutParams
    }

    fun createLinear(width: Float, height: Float, weight: Float): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            getSize(width),
            getSize(height),
            weight
        )
    }

    fun createLinear(width: Float, height: Float): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(getSize(width), getSize(height))
    }

    fun createLinearRelatively(
        width: Float,
        height: Float,
        gravity: Int,
        weight: Float,
        startMargin: Float,
        topMargin: Float,
        endMargin: Float,
        bottomMargin: Float
    ): LinearLayout.LayoutParams {
        val layoutParams = LinearLayout.LayoutParams(
            getSize(width),
            getSize(height),
            weight
        )
        layoutParams.leftMargin = dp(if (isRTL) endMargin else startMargin)
        layoutParams.topMargin = dp(topMargin)
        layoutParams.rightMargin = dp(if (isRTL) startMargin else endMargin)
        layoutParams.bottomMargin = dp(bottomMargin)
        layoutParams.gravity = getAbsoluteGravity(gravity)
        return layoutParams
    }

    fun createLinearRelatively(
        width: Float,
        height: Float,
        gravity: Int,
        weight: Float,
    ): LinearLayout.LayoutParams {
        val params = LinearLayout.LayoutParams(
            getSize(width),
            getSize(height),
            weight
        )
        params.gravity = getAbsoluteGravity(gravity)
        return params
    } //endregion
}