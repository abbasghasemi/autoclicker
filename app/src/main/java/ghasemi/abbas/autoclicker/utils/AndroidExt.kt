package ghasemi.abbas.autoclicker.utils

import android.R
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.widget.EditText

var EditText.value
    get() = this.text.toString()
    set(value) {
        this.setText(value)
    }

fun rippleBackground(content: Drawable? = null, mask: Drawable? = null): Drawable {
    return RippleDrawable(
        ColorStateList(
            arrayOf(
                intArrayOf(R.attr.state_enabled.inv()),
                intArrayOf(R.attr.state_empty)
            ), intArrayOf(
                Color.GRAY, Color.TRANSPARENT
            )
        ), content, mask
    )
}

fun circularRippleBackground(): Drawable = RippleDrawable(
    ColorStateList.valueOf(Color.argb(55, 0, 0, 0)),
    null,
    GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(Color.WHITE)
    }
)
