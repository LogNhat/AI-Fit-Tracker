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

    @Test
    fun testCalculateAngle_ObtuseAngle() {
        // 135 degrees: (-1, 1) -> (0, 0) -> (1, 0)
        val angle = PoseUtils.calculateAngle(
            firstX = -1f, firstY = 1f,
            middleX = 0f, middleY = 0f,
            lastX = 1f, lastY = 0f
        )
        assertEquals(135.0, angle, 0.01)
    }

    @Test
    fun testCalculateAngle_NegativeCoordinates() {
        // 90 degrees with negative coordinates: (-1, -2) -> (-1, -1) -> (0, -1)
        val angle = PoseUtils.calculateAngle(
            firstX = -1f, firstY = -2f,
            middleX = -1f, middleY = -1f,
            lastX = 0f, lastY = -1f
        )
        assertEquals(90.0, angle, 0.01)
    }

    @Test
    fun testCalculateAngle_OverlappingPoints() {
        // Overlapping middle and last points: (1, 1) -> (0, 0) -> (0, 0)
        val angle = PoseUtils.calculateAngle(
            firstX = 1f, firstY = 1f,
            middleX = 0f, middleY = 0f,
            lastX = 0f, lastY = 0f
        )
        // atan2(0, 0) is 0. atan2(1, 1) is 45 deg. Difference is -45, abs value is 45.
        assertEquals(45.0, angle, 0.01)
    }
}

