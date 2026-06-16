package com.example.aifittracker.analysis

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlin.math.atan2

class PoseAnalyzer(
    private val exerciseType: ExerciseType,
    private val onPoseDetected: (Pose, Int, Int) -> Unit,
    private val onRepDetected: () -> Unit,
    private val onStateChanged: (String) -> Unit,
    private val onFeedbackChanged: (String) -> Unit,
    private val onActiveLegChanged: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .build()

    private val detector = PoseDetection.getClient(options)
    
    // Squat State
    private var squatState = SquatState.STANDING
    private var squatReachedBottom = false

    // Push-up State
    private var pushUpState = PushUpState.UP
    private var pushUpReachedBottom = false

    // Plank State
    private var plankState = PlankState.INCORRECT

    // Jumping Jack State
    private var jumpingJackState = JumpingJackState.CLOSED
    private var jackReachedOpen = false

    // Bicep Curl State
    private var bicepCurlState = BicepCurlState.EXTENDED
    private var bicepCurlReachedPeak = false

    // Power saving mode state
    private var lastValidPoseTimestamp: Long = System.currentTimeMillis()
    private var frameCounter = 0

    private fun checkPoseVisibility(pose: Pose): Boolean {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        var visibleCount = 0
        if (leftShoulder != null && leftShoulder.inFrameLikelihood > 0.5f) visibleCount++
        if (rightShoulder != null && rightShoulder.inFrameLikelihood > 0.5f) visibleCount++
        if (leftHip != null && leftHip.inFrameLikelihood > 0.5f) visibleCount++
        if (rightHip != null && rightHip.inFrameLikelihood > 0.5f) visibleCount++

        return visibleCount >= 2
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastValidPose = currentTime - lastValidPoseTimestamp

        // Nếu không phát hiện tư thế người trong 10 giây, giảm tần suất quét xuống 1/10 frame để tiết kiệm pin
        if (timeSinceLastValidPose > 10000) {
            frameCounter++
            if (frameCounter % 10 != 0) {
                imageProxy.close()
                return
            }
        } else {
            frameCounter = 0
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val isRotated = rotationDegrees == 90 || rotationDegrees == 270
            val width = if (isRotated) imageProxy.height else imageProxy.width
            val height = if (isRotated) imageProxy.width else imageProxy.height

            val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)
            detector.process(image)
                .addOnSuccessListener { pose ->
                    if (checkPoseVisibility(pose)) {
                        lastValidPoseTimestamp = System.currentTimeMillis()
                    }
                    onPoseDetected(pose, width, height)
                    when (exerciseType) {
                        ExerciseType.SQUAT -> analyzeSquat(pose)
                        ExerciseType.PUSH_UP -> analyzePushUp(pose)
                        ExerciseType.PLANK -> analyzePlank(pose)
                        ExerciseType.JUMPING_JACK -> analyzeJumpingJack(pose)
                        ExerciseType.BICEP_CURL -> analyzeBicepCurl(pose)
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun analyzeSquat(pose: Pose) {
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val leftSideVisible = leftHip != null && leftKnee != null && leftAnkle != null &&
                leftHip.inFrameLikelihood > 0.5f && leftKnee.inFrameLikelihood > 0.5f && leftAnkle.inFrameLikelihood > 0.5f

        val rightSideVisible = rightHip != null && rightKnee != null && rightAnkle != null &&
                rightHip.inFrameLikelihood > 0.5f && rightKnee.inFrameLikelihood > 0.5f && rightAnkle.inFrameLikelihood > 0.5f

        if (leftSideVisible || rightSideVisible) {
            val isLeftActive = if (leftSideVisible && rightSideVisible) {
                val leftAvgConf = (leftHip!!.inFrameLikelihood + leftKnee!!.inFrameLikelihood + leftAnkle!!.inFrameLikelihood) / 3f
                val rightAvgConf = (rightHip!!.inFrameLikelihood + rightKnee!!.inFrameLikelihood + rightAnkle!!.inFrameLikelihood) / 3f
                leftAvgConf >= rightAvgConf
            } else {
                leftSideVisible
            }

            onActiveLegChanged(if (isLeftActive) "Left Leg" else "Right Leg")

            val kneeAngle = if (isLeftActive) {
                PoseUtils.calculateAngle(
                    leftHip!!.position.x, leftHip.position.y,
                    leftKnee!!.position.x, leftKnee.position.y,
                    leftAnkle!!.position.x, leftAnkle.position.y
                )
            } else {
                PoseUtils.calculateAngle(
                    rightHip!!.position.x, rightHip.position.y,
                    rightKnee!!.position.x, rightKnee.position.y,
                    rightAnkle!!.position.x, rightAnkle.position.y
                )
            }

            when (squatState) {
                SquatState.STANDING -> {
                    if (kneeAngle < 160) {
                        squatState = SquatState.DESCENDING
                        onStateChanged(squatState.name)
                        onFeedbackChanged("Going down...")
                    }
                }
                SquatState.DESCENDING -> {
                    if (kneeAngle < 95) {
                        squatState = SquatState.BOTTOM
                        squatReachedBottom = true
                        onStateChanged(squatState.name)
                        onFeedbackChanged("Nice depth!")
                    } else if (kneeAngle > 165) {
                        squatState = SquatState.STANDING
                        onStateChanged(squatState.name)
                        onFeedbackChanged("Too shallow! Go deeper.")
                    }
                }
                SquatState.BOTTOM -> {
                    if (kneeAngle > 105) {
                        squatState = SquatState.ASCENDING
                        onStateChanged(squatState.name)
                        onFeedbackChanged("Push up!")
                    }
                }
                SquatState.ASCENDING -> {
                    if (kneeAngle > 160) {
                        if (squatReachedBottom) {
                            onRepDetected()
                            onFeedbackChanged("Good rep!")
                        } else {
                            onFeedbackChanged("Go deeper next time!")
                        }
                        squatState = SquatState.STANDING
                        squatReachedBottom = false
                        onStateChanged(squatState.name)
                    }
                }
            }
        } else {
            onActiveLegChanged("None")
            if (squatState != SquatState.STANDING) {
                squatState = SquatState.STANDING
                squatReachedBottom = false
                onStateChanged(squatState.name)
                onFeedbackChanged("Ready")
            }
        }
    }

    private fun analyzePushUp(pose: Pose) {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)

        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        val leftSideVisible = leftShoulder != null && leftElbow != null && leftWrist != null &&
                leftShoulder.inFrameLikelihood > 0.5f && leftElbow.inFrameLikelihood > 0.5f && leftWrist.inFrameLikelihood > 0.5f

        val rightSideVisible = rightShoulder != null && rightElbow != null && rightWrist != null &&
                rightShoulder.inFrameLikelihood > 0.5f && rightElbow.inFrameLikelihood > 0.5f && rightWrist.inFrameLikelihood > 0.5f

        if (leftSideVisible || rightSideVisible) {
            val isLeftActive = if (leftSideVisible && rightSideVisible) {
                val leftAvgConf = (leftShoulder!!.inFrameLikelihood + leftElbow!!.inFrameLikelihood + leftWrist!!.inFrameLikelihood) / 3f
                val rightAvgConf = (rightShoulder!!.inFrameLikelihood + rightElbow!!.inFrameLikelihood + rightWrist!!.inFrameLikelihood) / 3f
                leftAvgConf >= rightAvgConf
            } else {
                leftSideVisible
            }

            onActiveLegChanged(if (isLeftActive) "Left Arm" else "Right Arm")

            val elbowAngle = if (isLeftActive) {
                PoseUtils.calculateAngle(
                    leftShoulder!!.position.x, leftShoulder.position.y,
                    leftElbow!!.position.x, leftElbow.position.y,
                    leftWrist!!.position.x, leftWrist.position.y
                )
            } else {
                PoseUtils.calculateAngle(
                    rightShoulder!!.position.x, rightShoulder.position.y,
                    rightElbow!!.position.x, rightElbow.position.y,
                    rightWrist!!.position.x, rightWrist.position.y
                )
            }

            when (pushUpState) {
                PushUpState.UP -> {
                    if (elbowAngle < 150) {
                        pushUpState = PushUpState.DESCENDING
                        onStateChanged(pushUpState.name)
                        onFeedbackChanged("Going down...")
                    }
                }
                PushUpState.DESCENDING -> {
                    if (elbowAngle < 90) {
                        pushUpState = PushUpState.BOTTOM
                        pushUpReachedBottom = true
                        onStateChanged(pushUpState.name)
                        onFeedbackChanged("Good depth!")
                    } else if (elbowAngle > 160) {
                        pushUpState = PushUpState.UP
                        onStateChanged(pushUpState.name)
                        onFeedbackChanged("Too shallow! Go lower.")
                    }
                }
                PushUpState.BOTTOM -> {
                    if (elbowAngle > 100) {
                        pushUpState = PushUpState.ASCENDING
                        onStateChanged(pushUpState.name)
                        onFeedbackChanged("Push up!")
                    }
                }
                PushUpState.ASCENDING -> {
                    if (elbowAngle > 150) {
                        if (pushUpReachedBottom) {
                            onRepDetected()
                            onFeedbackChanged("Good rep!")
                        } else {
                            onFeedbackChanged("Go lower next time!")
                        }
                        pushUpState = PushUpState.UP
                        pushUpReachedBottom = false
                        onStateChanged(pushUpState.name)
                    }
                }
            }
        } else {
            onActiveLegChanged("None")
            if (pushUpState != PushUpState.UP) {
                pushUpState = PushUpState.UP
                pushUpReachedBottom = false
                onStateChanged(pushUpState.name)
                onFeedbackChanged("Ready")
            }
        }
    }

    private fun analyzePlank(pose: Pose) {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val leftSideVisible = leftShoulder != null && leftHip != null && leftKnee != null && leftAnkle != null &&
                leftShoulder.inFrameLikelihood > 0.5f && leftHip.inFrameLikelihood > 0.5f && leftKnee.inFrameLikelihood > 0.5f && leftAnkle.inFrameLikelihood > 0.5f

        val rightSideVisible = rightShoulder != null && rightHip != null && rightKnee != null && rightAnkle != null &&
                rightShoulder.inFrameLikelihood > 0.5f && rightHip.inFrameLikelihood > 0.5f && rightKnee.inFrameLikelihood > 0.5f && rightAnkle.inFrameLikelihood > 0.5f

        if (leftSideVisible || rightSideVisible) {
            val isLeftActive = if (leftSideVisible && rightSideVisible) {
                val leftAvgConf = (leftShoulder!!.inFrameLikelihood + leftHip!!.inFrameLikelihood + leftKnee!!.inFrameLikelihood + leftAnkle!!.inFrameLikelihood) / 4f
                val rightAvgConf = (rightShoulder!!.inFrameLikelihood + rightHip!!.inFrameLikelihood + rightKnee!!.inFrameLikelihood + rightAnkle!!.inFrameLikelihood) / 4f
                leftAvgConf >= rightAvgConf
            } else {
                leftSideVisible
            }

            onActiveLegChanged(if (isLeftActive) "Left Profile" else "Right Profile")

            val shoulder = if (isLeftActive) leftShoulder!! else rightShoulder!!
            val hip = if (isLeftActive) leftHip!! else rightHip!!
            val knee = if (isLeftActive) leftKnee!! else rightKnee!!
            val ankle = if (isLeftActive) leftAnkle!! else rightAnkle!!

            val hipAngle = PoseUtils.calculateAngle(
                shoulder.position.x, shoulder.position.y,
                hip.position.x, hip.position.y,
                knee.position.x, knee.position.y
            )
            val kneeAngle = PoseUtils.calculateAngle(
                hip.position.x, hip.position.y,
                knee.position.x, knee.position.y,
                ankle.position.x, ankle.position.y
            )

            val isBackStraight = hipAngle in 160.0..200.0
            val areLegsStraight = kneeAngle > 160.0

            if (isBackStraight && areLegsStraight) {
                plankState = PlankState.CORRECT
                onStateChanged(plankState.name)
                onFeedbackChanged("Perfect form! Keep holding.")
            } else {
                plankState = PlankState.INCORRECT
                onStateChanged(plankState.name)
                if (hipAngle < 160.0) {
                    onFeedbackChanged("Lower your hips!")
                } else if (hipAngle > 200.0) {
                    onFeedbackChanged("Straighten your back!")
                } else {
                    onFeedbackChanged("Keep legs straight!")
                }
            }
        } else {
            onActiveLegChanged("None")
            if (plankState != PlankState.INCORRECT) {
                plankState = PlankState.INCORRECT
                onStateChanged(plankState.name)
                onFeedbackChanged("Ready")
            }
        }
    }

    private fun analyzeJumpingJack(pose: Pose) {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)

        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        val isPoseVisible = leftShoulder != null && leftWrist != null && leftAnkle != null && leftHip != null &&
                rightShoulder != null && rightWrist != null && rightAnkle != null && rightHip != null &&
                leftShoulder.inFrameLikelihood > 0.5f && leftWrist.inFrameLikelihood > 0.5f && leftAnkle.inFrameLikelihood > 0.5f &&
                rightShoulder.inFrameLikelihood > 0.5f && rightWrist.inFrameLikelihood > 0.5f && rightAnkle.inFrameLikelihood > 0.5f

        if (isPoseVisible) {
            onActiveLegChanged("Full Body")

            // Check if hands are raised above shoulders (Y decreases going up)
            val isHandsUp = leftWrist!!.position.y < leftShoulder!!.position.y &&
                    rightWrist!!.position.y < rightShoulder!!.position.y

            // Check if legs are spread out (Ankle width > 1.4x Hip width)
            val hipWidth = Math.abs(leftHip!!.position.x - rightHip!!.position.x)
            val ankleDistance = Math.abs(leftAnkle!!.position.x - rightAnkle!!.position.x)
            val isLegsSpread = ankleDistance > (hipWidth * 1.5f)

            when (jumpingJackState) {
                JumpingJackState.CLOSED -> {
                    if (isHandsUp && isLegsSpread) {
                        jumpingJackState = JumpingJackState.OPEN
                        jackReachedOpen = true
                        onStateChanged(jumpingJackState.name)
                        onFeedbackChanged("Open!")
                    } else if (isHandsUp && !isLegsSpread) {
                        onFeedbackChanged("Spread your legs!")
                    } else if (!isHandsUp && isLegsSpread) {
                        onFeedbackChanged("Raise your hands!")
                    }
                }
                JumpingJackState.OPEN -> {
                    if (!isHandsUp && !isLegsSpread) {
                        if (jackReachedOpen) {
                            onRepDetected()
                            onFeedbackChanged("Good jack!")
                        }
                        jumpingJackState = JumpingJackState.CLOSED
                        jackReachedOpen = false
                        onStateChanged(jumpingJackState.name)
                    } else if (!isHandsUp && isLegsSpread) {
                        onFeedbackChanged("Raise your hands!")
                    } else if (isHandsUp && !isLegsSpread) {
                        onFeedbackChanged("Close your legs!")
                    }
                }
            }
        } else {
            onActiveLegChanged("None")
            if (jumpingJackState != JumpingJackState.CLOSED) {
                jumpingJackState = JumpingJackState.CLOSED
                jackReachedOpen = false
                onStateChanged(jumpingJackState.name)
                onFeedbackChanged("Ready")
            }
        }
    }

    private fun analyzeBicepCurl(pose: Pose) {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)

        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        val leftSideVisible = leftShoulder != null && leftElbow != null && leftWrist != null &&
                leftShoulder.inFrameLikelihood > 0.5f && leftElbow.inFrameLikelihood > 0.5f && leftWrist.inFrameLikelihood > 0.5f

        val rightSideVisible = rightShoulder != null && rightElbow != null && rightWrist != null &&
                rightShoulder.inFrameLikelihood > 0.5f && rightElbow.inFrameLikelihood > 0.5f && rightWrist.inFrameLikelihood > 0.5f

        if (leftSideVisible || rightSideVisible) {
            val isLeftActive = if (leftSideVisible && rightSideVisible) {
                val leftAvgConf = (leftShoulder!!.inFrameLikelihood + leftElbow!!.inFrameLikelihood + leftWrist!!.inFrameLikelihood) / 3f
                val rightAvgConf = (rightShoulder!!.inFrameLikelihood + rightElbow!!.inFrameLikelihood + rightWrist!!.inFrameLikelihood) / 3f
                leftAvgConf >= rightAvgConf
            } else {
                leftSideVisible
            }

            onActiveLegChanged(if (isLeftActive) "Left Arm" else "Right Arm")

            val shoulder = if (isLeftActive) leftShoulder!! else rightShoulder!!
            val elbow = if (isLeftActive) leftElbow!! else rightElbow!!
            val wrist = if (isLeftActive) leftWrist!! else rightWrist!!

            val elbowAngle = PoseUtils.calculateAngle(
                shoulder.position.x, shoulder.position.y,
                elbow.position.x, elbow.position.y,
                wrist.position.x, wrist.position.y
            )

            when (bicepCurlState) {
                BicepCurlState.EXTENDED -> {
                    if (elbowAngle < 140) {
                        bicepCurlState = BicepCurlState.FLEXING
                        onStateChanged(bicepCurlState.name)
                        onFeedbackChanged("Curl up...")
                    }
                }
                BicepCurlState.FLEXING -> {
                    if (elbowAngle < 45) {
                        bicepCurlState = BicepCurlState.FLEXED
                        bicepCurlReachedPeak = true
                        onStateChanged(bicepCurlState.name)
                        onFeedbackChanged("Squeeze!")
                    } else if (elbowAngle > 150) {
                        bicepCurlState = BicepCurlState.EXTENDED
                        onStateChanged(bicepCurlState.name)
                        onFeedbackChanged("Ready")
                    }
                }
                BicepCurlState.FLEXED -> {
                    if (elbowAngle > 55) {
                        bicepCurlState = BicepCurlState.EXTENDING
                        onStateChanged(bicepCurlState.name)
                        onFeedbackChanged("Lower down...")
                    }
                }
                BicepCurlState.EXTENDING -> {
                    if (elbowAngle > 140) {
                        if (bicepCurlReachedPeak) {
                            onRepDetected()
                            onFeedbackChanged("Good curl!")
                        } else {
                            onFeedbackChanged("Curl higher next time!")
                        }
                        bicepCurlState = BicepCurlState.EXTENDED
                        bicepCurlReachedPeak = false
                        onStateChanged(bicepCurlState.name)
                    } else if (elbowAngle < 50) {
                        // Went back up
                        bicepCurlState = BicepCurlState.FLEXED
                        onStateChanged(bicepCurlState.name)
                        onFeedbackChanged("Squeeze!")
                    }
                }
            }
        } else {
            onActiveLegChanged("None")
            if (bicepCurlState != BicepCurlState.EXTENDED) {
                bicepCurlState = BicepCurlState.EXTENDED
                bicepCurlReachedPeak = false
                onStateChanged(bicepCurlState.name)
                onFeedbackChanged("Ready")
            }
        }
    }

    fun close() {
        detector.close()
    }
}
