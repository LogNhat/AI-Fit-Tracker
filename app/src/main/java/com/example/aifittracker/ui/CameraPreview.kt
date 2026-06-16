package com.example.aifittracker.ui

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.aifittracker.analysis.ExerciseType
import com.example.aifittracker.analysis.PoseAnalyzer
import java.util.concurrent.Executors

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    exerciseType: ExerciseType,
    isFrontCamera: Boolean = true,
    onPoseDetected: (com.google.mlkit.vision.pose.Pose, Int, Int) -> Unit,
    onRepDetected: () -> Unit,
    onStateChanged: (String) -> Unit,
    onFeedbackChanged: (String) -> Unit,
    onActiveLegChanged: (String) -> Unit
) {
    key(exerciseType, isFrontCamera) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
        val analyzer = remember(exerciseType) {
            PoseAnalyzer(
                exerciseType = exerciseType,
                onPoseDetected = onPoseDetected,
                onRepDetected = onRepDetected,
                onStateChanged = onStateChanged,
                onFeedbackChanged = onFeedbackChanged,
                onActiveLegChanged = onActiveLegChanged
            )
        }
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
        val isDisposed = remember { mutableStateOf(false) }

        DisposableEffect(analyzer, cameraExecutor, cameraProviderFuture) {
            onDispose {
                isDisposed.value = true
                try {
                    if (cameraProviderFuture.isDone) {
                        cameraProviderFuture.get().unbindAll()
                    }
                } catch (exc: Exception) {
                    // ignore
                }
                analyzer.close()
                cameraExecutor.shutdown()
            }
        }

        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    keepScreenOn = true
                }

                cameraProviderFuture.addListener({
                    if (isDisposed.value) return@addListener
                    
                    try {
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(cameraExecutor, analyzer)
                            }

                        val cameraSelector = if (isFrontCamera) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (exc: Exception) {
                        // Handle failure
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = modifier
        )
    }
}
