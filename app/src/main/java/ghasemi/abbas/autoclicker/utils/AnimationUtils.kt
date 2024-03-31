package ghasemi.abbas.autoclicker.utils

import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.Transition
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.ViewGroup

object AnimationUtils {
    fun changeBounds(root: ViewGroup?) {
        TransitionManager.endTransitions(root)
        val transition: Transition = TransitionSet()
            .addTransition(Fade(Fade.MODE_IN))
            .addTransition(ChangeBounds())
            .addTransition(Fade(Fade.MODE_OUT))
            .setDuration(250)
        TransitionManager.beginDelayedTransition(root, transition)
    }
}