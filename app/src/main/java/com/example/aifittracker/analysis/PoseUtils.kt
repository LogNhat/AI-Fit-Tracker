package com.example.aifittracker.analysis

import kotlin.math.atan2

object PoseUtils {
    fun calculateAngle(
        firstX: Float, firstY: Float,
        middleX: Float, middleY: Float,
        lastX: Float, lastY: Float
    ): Double {
        var angle = Math.toDegrees(
            (atan2(lastY - middleY, lastX - middleX) -
                    atan2(firstY - middleY, firstX - middleX)).toDouble()
        )
        angle = Math.abs(angle)
        if (angle > 180) {
            angle = 360.0 - angle
        }
        return angle
    }
}
