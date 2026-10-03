package com.needlefactory.debug

/** Diagnostic event state only. No rendering, gameplay, simulation or economic truth. */
class BootStatus {
    var presses = 0
        private set
    var pauses = 0
        private set
    var resumes = 0
        private set
    var paused = false
        private set

    fun press() { if (!paused) presses++ }
    fun pause() {
        if (!paused) {
            paused = true
            pauses++
        }
    }
    fun resume() {
        if (paused) {
            paused = false
            resumes++
        }
    }
}
