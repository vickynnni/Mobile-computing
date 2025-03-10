package com.example.finalhomework

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            MyAppNavHost(navController = navController)
        }
    }
}

@Composable
fun MyAppNavHost(navController: NavHostController) {
    val context = LocalContext.current
    val imageUris = remember { mutableStateListOf<Uri>() }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            navController.navigate("cameraScreen")
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            navController.navigate("audioScreen")
        } else {
            Toast.makeText(context, "Audio recording permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    NavHost(
        navController = navController,
        startDestination = "mainScreen"
    ) {
        composable("mainScreen") {
            MainScreen(
                onNavigateToAudios = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        navController.navigate("audioScreen")
                    } else {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onNavigateToCameraScreen = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    ) {
                        navController.navigate("cameraScreen")
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            )
        }
        composable("cameraScreen") {
            CameraScreen(
                onImageCaptured = { uri -> imageUris.add(uri) },
                onNavigateToGallery = { navController.navigate("galleryScreen") }
            )
        }
        composable("galleryScreen") {
            GalleryScreen(
                imageUris = imageUris,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("audioScreen") {
            AudioRecorderScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun CameraScreen(onImageCaptured: (Uri) -> Unit, onNavigateToGallery: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var previewUseCase by remember { mutableStateOf<Preview?>(null) }
    var imageCaptureUseCase by remember { mutableStateOf<ImageCapture?>(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }


    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            previewUseCase = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            imageCaptureUseCase = ImageCapture.Builder()
                                .setTargetRotation(previewView.display.rotation)
                                .build()

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, previewUseCase, imageCaptureUseCase)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                                imageCaptureUseCase?.let { imageCapture ->
                                    val imageFile = File(
                                        context.getExternalFilesDir("Images"),
                                        "IMG.jpg"
                                    )

                                    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()

                                    imageCapture.takePicture(outputFileOptions, cameraExecutor,
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onError(exception: ImageCaptureException) {
                                                    Toast.makeText(context, "No photo taken, it failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                                            }

                                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                val savedUri = outputFileResults.savedUri ?: Uri.fromFile(imageFile)
                                                    onImageCaptured(savedUri)
                                            }
                                        }
                                    )
                            }
                        }
                    ) {
                        Text("Take Photo")
                    }

                    Button(onClick = onNavigateToGallery) {
                        Text("Gallery")
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(onNavigateToAudios: () -> Unit, onNavigateToCameraScreen: () -> Unit) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onNavigateToCameraScreen,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Camera",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Button(
                onClick = onNavigateToAudios,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Audios",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun GalleryScreen(imageUris: List<Uri>, onNavigateBack: () -> Unit) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Button(
                onClick = onNavigateBack,
                modifier = Modifier.padding(18.dp)
            ) {
                Spacer(modifier = Modifier.size(8.dp))
                Text("Back")
            }

            if (imageUris.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No images!!",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.padding(8.dp)
                ) {
                    items(imageUris) { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = "Image",
                            modifier = Modifier
                                .size(160.dp)
                                .padding(4.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

private const val LOG_TAG = "AudioRecordTest"

@Composable
fun AudioRecorderScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val fileName = "${context.externalCacheDir?.absolutePath}/audiotest.3gp"

    var mStartRecording by remember { mutableStateOf(true) }
    var mStartPlaying by remember { mutableStateOf(true) }
    var recorder: MediaRecorder? by remember { mutableStateOf(null) }
    var player: MediaPlayer? by remember { mutableStateOf(null) }

    fun startPlaying() {
        player = MediaPlayer().apply {
            try {
                setDataSource(fileName)
                prepare()
                start()
                setOnCompletionListener {
                    mStartPlaying = true
                }
            } catch (e: IOException) {
                Toast.makeText(context, "Playback failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun stopPlaying(player: MediaPlayer?) {
        player?.release()
    }

    fun startRecording() {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
            } catch (e: IOException) {
                Toast.makeText(context, "Recording preparation failed", Toast.LENGTH_SHORT).show()
                return@apply
            }

            start()
        }
    }

    fun stopRecording(recorder: MediaRecorder?) {
        recorder?.apply {
            try {
                stop()
            } catch (e: Exception) {
            }
            release()
        }
    }

    fun onRecord(start: Boolean) {
        if (start) {
            startRecording()
        } else {
            stopRecording(recorder)
            recorder = null
        }
    }

    fun onPlay(start: Boolean) {
        if (start) {
            startPlaying()
        } else {
            stopPlaying(player)
            player = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onRecord(mStartRecording)
                    mStartRecording = !mStartRecording
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(if (mStartRecording) "Start recording" else "Stop recording")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onPlay(mStartPlaying)
                    mStartPlaying = !mStartPlaying
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                enabled = mStartRecording
            ) {
                Text(if (mStartPlaying) "Start playing" else "Stop playing")
            }
    }
}
