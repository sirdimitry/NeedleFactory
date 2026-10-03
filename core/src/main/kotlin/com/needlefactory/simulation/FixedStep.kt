package com.needlefactory.simulation

/** Drops overdue whole steps after a bounded catch-up; retains fractional alpha. */
class FixedStep(val seconds: Float = 1f / 60f, private val maxSteps: Int = 5,
                private val maxDelta: Float = 0.1f) {
    private var accumulator = 0.0
    var droppedSeconds = 0.0
        private set
    val alpha: Float get() = (accumulator / seconds).toFloat().coerceIn(0f, 1f)
    fun advance(delta: Float, step: () -> Unit): Int {
        if (!delta.isFinite() || delta <= 0f) return 0
        val accepted = delta.coerceAtMost(maxDelta)
        droppedSeconds += (delta - accepted).toDouble()
        accumulator += accepted
        var count = 0
        while (accumulator + 1e-9 >= seconds && count < maxSteps) {
            step()
            accumulator = (accumulator - seconds).coerceAtLeast(0.0)
            count++
        }
        if (accumulator + 1e-9 >= seconds) {
            val dropped = floorSteps(accumulator + 1e-9)
            accumulator = (accumulator - dropped).coerceAtLeast(0.0)
            droppedSeconds += dropped
        }
        return count
    }
    private fun floorSteps(value: Double) = kotlin.math.floor(value / seconds) * seconds
    fun reset() { accumulator = 0.0 }
}
