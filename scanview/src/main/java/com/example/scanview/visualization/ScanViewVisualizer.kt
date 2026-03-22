package com.example.scanview.visualization

import android.view.View
import android.view.ViewGroup
import com.example.scanview.data.InteractionRecord

object ScanViewVisualizer {
    fun attachOverlay(
        parent: ViewGroup,
        interactions: List<InteractionRecord>,
        config: VisualizationConfig = VisualizationConfig()
    ): ScanViewOverlay {
        val overlay = ScanViewOverlay(parent.context)
        overlay.setInteractions(interactions, config)
        
        parent.addView(
            overlay,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        
        return overlay
    }
    fun detachOverlay(parent: ViewGroup, overlay: ScanViewOverlay) {
        parent.removeView(overlay)
    }

    fun removeAllOverlays(parent: ViewGroup) {
        val overlays = mutableListOf<View>()
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is ScanViewOverlay) {
                overlays.add(child)
            }
        }
        overlays.forEach { parent.removeView(it) }
    }
}
