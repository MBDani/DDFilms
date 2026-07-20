package com.merino.ddfilms.transitions

import android.transition.ChangeBounds
import android.transition.ChangeTransform
import android.transition.Fade
import android.transition.TransitionSet

class DetailsTransition : TransitionSet() {
    init {
        ordering = ORDERING_TOGETHER

        // Transición de entrada
        val fade = Fade().apply { duration = 500 }
        addTransition(fade)

        // Transición de movimiento de la imagen
        val changeBounds = ChangeBounds().apply { duration = 500 }
        addTransition(changeBounds)

        // Escala
        val changeTransform = ChangeTransform().apply { duration = 500 }
        addTransition(changeTransform)
    }
}
