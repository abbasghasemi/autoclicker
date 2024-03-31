package ghasemi.abbas.autoclicker.ui.components

import android.content.Context
import android.widget.ImageView
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import ghasemi.abbas.autoclicker.R

class PlayPauseView(context: Context) : ImageView(context) {
    private var mPlayToPauseAnim: AnimatedVectorDrawableCompat? = null
    private var mPauseToPlay: AnimatedVectorDrawableCompat? = null

    init {
        mPlayToPauseAnim = AnimatedVectorDrawableCompat.create(context, R.drawable.play_to_pause)
        mPauseToPlay = AnimatedVectorDrawableCompat.create(context, R.drawable.pause_to_play)
        setState(STATE_PAUSE)
    }

    fun setState(state: Int) {
        when (state) {
            STATE_PLAY -> {
                setImageDrawable(mPlayToPauseAnim)
                mPlayToPauseAnim!!.start()
            }

            STATE_PAUSE -> {
                setImageDrawable(mPauseToPlay)
                mPauseToPlay!!.start()
            }
        }
    }

    companion object {
        const val STATE_PLAY = 1
        const val STATE_PAUSE = 2
    }
}