package com.example.timeapk.ui.theme

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class AnimationSpecsTest {
    @After
    fun tearDown() {
        AnimationSpecs.setReducedMotionEnabled(false)
    }

    @Test
    fun reducedMotion_keepsResponsiveScaleAndAlphaStatic() {
        AnimationSpecs.setReducedMotionEnabled(true)

        assertEquals(1f, AnimationSpecs.responsiveScale(0.98f), 0.001f)
        assertEquals(1f, AnimationSpecs.responsiveAlpha(0.85f), 0.001f)
    }

    @Test
    fun normalMotion_usesRequestedResponsiveTargets() {
        AnimationSpecs.setReducedMotionEnabled(false)

        assertEquals(0.98f, AnimationSpecs.responsiveScale(0.98f), 0.001f)
        assertEquals(0.85f, AnimationSpecs.responsiveAlpha(0.85f), 0.001f)
    }
}
