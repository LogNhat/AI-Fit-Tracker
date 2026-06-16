package com.example.aifittracker.analysis

import org.junit.Assert.assertEquals
import org.junit.Test

class PoseUtilsTest {

    @Test
    fun testCalculateAngle_RightAngle() {
        // A right angle: (0, 1) -> (0, 0) -> (1, 0)
        val angle = PoseUtils.calculateAngle(
            firstX = 0f, firstY = 1f,     // top
            middleX = 0f, middleY = 0f,   // origin (corner)
            lastX = 1f, lastY = 0f        // right
        )
        assertEquals(90.0, angle, 0.01)
    }

    @Test
    fun testCalculateAngle_StraightLine() {
        // A straight line: (-1, 0) -> (0, 0) -> (1, 0)
        val angle = PoseUtils.calculateAngle(
            firstX = -1f, firstY = 0f,
            middleX = 0f, middleY = 0f,
            lastX = 1f, lastY = 0f
        )
        assertEquals(180.0, angle, 0.01)
    }

    @Test
    fun testCalculateAngle_AcuteAngle() {
        // 45 degrees: (1, 1) -> (0, 0) -> (1, 0)
        val angle = PoseUtils.calculateAngle(
            firstX = 1f, firstY = 1f,
            middleX = 0f, middleY = 0f,
            lastX = 1f, lastY = 0f
        )
        assertEquals(45.0, angle, 0.01)
    }
}
