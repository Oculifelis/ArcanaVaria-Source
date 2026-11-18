package com.arcanavaria.lab;

/** A lab block entity that goes back to its first state when its ward resets (everyone inside died or left). */
public interface Resettable {
    void labReset();
}
