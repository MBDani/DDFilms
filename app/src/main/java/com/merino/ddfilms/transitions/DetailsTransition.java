package com.merino.ddfilms.transitions;

import android.transition.ChangeBounds;
import android.transition.ChangeTransform;
import android.transition.Fade;
import android.transition.TransitionSet;

public class DetailsTransition extends TransitionSet {

    public DetailsTransition() {
        setOrdering(ORDERING_TOGETHER);

        // Transición de entrada
        Fade fade = new Fade();
        fade.setDuration(500);
        addTransition(fade);

        // Transición de movimiento de la imagen
        ChangeBounds changeBounds = new ChangeBounds();
        changeBounds.setDuration(500);
        addTransition(changeBounds);

        // Escala
        ChangeTransform changeTransform = new ChangeTransform();
        changeTransform.setDuration(500);
        addTransition(changeTransform);
    }
}
