package com.merino.ddfilms.transitions

import android.transition.ChangeBounds
import android.transition.ChangeImageTransform
import android.transition.TransitionSet

class DetailsTransition : TransitionSet() {
    init {
        ordering = ORDERING_TOGETHER

        // Transición de movimiento de la imagen
        val changeBounds = ChangeBounds().apply { duration = 350 }
        addTransition(changeBounds)

        // Escala de matriz de ImageView
        val changeImageTransform = ChangeImageTransform().apply { duration = 350 }
        addTransition(changeImageTransform)
    }
}
