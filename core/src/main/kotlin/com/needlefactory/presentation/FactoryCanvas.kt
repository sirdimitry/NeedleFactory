package com.needlefactory.presentation

import com.needlefactory.game.SafeArea
import kotlin.math.min
import kotlin.math.max

/** Pixel density of the gameplay modules stays independent of orientation.
 * Insets reserve content; no orientation-specific factory fit or padded GL surface. */
data class FactoryCanvas(val width: Float, val height: Float, val unitsPerPixel: Float) {
    companion object {
        fun forWindow(width: Int, height: Int, safe: SafeArea): FactoryCanvas {
            require(width > 0 && height > 0)
            // Reserve the larger obstructed axis irrespective of display orientation.
            // Status/navigation insets often stay top/bottom even in landscape.
            val longUsable = max(width,height)-max(safe.left+safe.right,safe.top+safe.bottom)
            // Calibrate one device baseline in BOTH orientations. Compact aspect ratios
            // reserve enough long-axis space for fixed modules, never a landscape-only zoom.
            val unit = max(400f / min(width, height), 814f / longUsable.coerceAtLeast(1))
            return FactoryCanvas((width-safe.left-safe.right).coerceAtLeast(1)*unit,
                (height-safe.top-safe.bottom).coerceAtLeast(1)*unit, unit)
        }
    }
}
