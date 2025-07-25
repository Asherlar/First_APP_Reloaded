package com.example.firstappreloaded

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.firstappreloaded.ui.theme.FirstAppReloadedTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService
    var imageCapture: ImageCapture? = null
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, camera will be initialized in composition
            setupCamera()
        } else {
            Toast.makeText(this, "Permiso de cámara necesario para escanear", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            == PackageManager.PERMISSION_GRANTED) {
            setupCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    private fun setupCamera() {
        // Verificar estado del almacenamiento
        checkStorageStatus()
        
        setContent {
            FirstAppReloadedTheme {
                ScannerScreen(
                    onBackClick = { finish() },
                    onCaptureClick = { capturePhoto() },
                    onCheckFilesClick = { checkSavedFiles() }
                )
            }
        }
    }
    
    private fun checkStorageStatus() {
        val documentsDir = getDocumentsDirectory()
        Log.i("StorageCheck", "=== ESTADO DEL ALMACENAMIENTO ===")
        Log.i("StorageCheck", "Directorio seleccionado: ${documentsDir.absolutePath}")
        Log.i("StorageCheck", "Directorio existe: ${documentsDir.exists()}")
        Log.i("StorageCheck", "Es escribible: ${documentsDir.canWrite()}")
        Log.i("StorageCheck", "Espacio libre: ${documentsDir.freeSpace / (1024*1024)} MB")
        
        // También verificar otros directorios candidatos
        val externalFilesDir = getExternalFilesDir(null)
        if (externalFilesDir != null) {
            Log.i("StorageCheck", "External files dir: ${externalFilesDir.absolutePath}")
            Log.i("StorageCheck", "External dir existe: ${externalFilesDir.exists()}")
        } else {
            Log.w("StorageCheck", "External files dir es NULL")
        }
        
        Log.i("StorageCheck", "Internal files dir: ${filesDir.absolutePath}")
        Log.i("StorageCheck", "Internal dir existe: ${filesDir.exists()}")
        Log.i("StorageCheck", "================================")
    }
    
    private fun capturePhoto() {
        val imageCapture = imageCapture ?: run {
            Log.e("CameraCapture", "ImageCapture no está inicializado")
            Toast.makeText(this, "Cámara no está lista", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Create time stamped name
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault())
            .format(System.currentTimeMillis())
        
        // Try multiple directory options
        val outputDir = getDocumentsDirectory()
        val outputFile = File(outputDir, "scan_$name.jpg")
        
        Log.d("CameraCapture", "Intentando guardar en: ${outputFile.absolutePath}")
        Log.d("CameraCapture", "Directorio existe: ${outputDir.exists()}")
        Log.d("CameraCapture", "Directorio es escribible: ${outputDir.canWrite()}")
        
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        
        // Set up image capture listener
        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    val errorMsg = "Error al capturar: ${exception.message}"
                    Log.e("CameraCapture", errorMsg, exception)
                    Log.e("CameraCapture", "Ruta intentada: ${outputFile.absolutePath}")
                    Toast.makeText(this@ScannerActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
                
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedPath = output.savedUri?.path ?: outputFile.absolutePath
                    val successMsg = "Documento guardado en: $savedPath"
                    Log.d("CameraCapture", successMsg)
                    Log.d("CameraCapture", "Archivo existe: ${outputFile.exists()}")
                    Log.d("CameraCapture", "Tamaño archivo: ${outputFile.length()} bytes")
                    
                    Toast.makeText(
                        this@ScannerActivity, 
                        "✅ Documento guardado exitosamente\n📍 ${outputFile.name}", 
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Optional: Show full path in log for debugging
                    Log.i("CameraCapture", "Ruta completa: ${outputFile.absolutePath}")
                }
            }
        )
    }
    
    private fun getDocumentsDirectory(): File {
        // Priority order for saving documents
        val candidates = listOf(
            // 1. External files directory (preferred)
            getExternalFilesDir(null),
            // 2. External files directory with Documents subfolder
            getExternalFilesDir("Documents"),
            // 3. Internal files directory (fallback)
            filesDir,
            // 4. Cache directory (last resort)
            cacheDir
        )
        
        for (dir in candidates) {
            if (dir != null && (dir.exists() || dir.mkdirs()) && dir.canWrite()) {
                Log.d("CameraCapture", "Usando directorio: ${dir.absolutePath}")
                return dir
            }
        }
        
        // If all else fails, use internal files directory
        val fallbackDir = filesDir
        if (!fallbackDir.exists()) {
            fallbackDir.mkdirs()
        }
        Log.w("CameraCapture", "Usando directorio de emergencia: ${fallbackDir.absolutePath}")
        return fallbackDir
    }
    
    private fun checkSavedFiles() {
        val documentsDir = getDocumentsDirectory()
        val files = documentsDir.listFiles { file -> 
            file.isFile && file.name.startsWith("scan_") && file.name.endsWith(".jpg")
        }
        
        if (files.isNullOrEmpty()) {
            Toast.makeText(this, "No se encontraron documentos guardados", Toast.LENGTH_LONG).show()
            Log.i("FileCheck", "No hay archivos en: ${documentsDir.absolutePath}")
        } else {
            val fileList = files.sortedByDescending { it.lastModified() }
                .take(5) // Mostrar los 5 más recientes
                .joinToString("\n") { "${it.name} (${it.length()} bytes)" }
            
            Toast.makeText(
                this, 
                "📁 Archivos encontrados (${files.size}):\n$fileList\n\nUbicación: ${documentsDir.name}",
                Toast.LENGTH_LONG
            ).show()
            
            Log.i("FileCheck", "=== ARCHIVOS GUARDADOS ===")
            Log.i("FileCheck", "Directorio: ${documentsDir.absolutePath}")
            Log.i("FileCheck", "Total archivos: ${files.size}")
            files.forEach { file ->
                Log.i("FileCheck", "- ${file.name} | ${file.length()} bytes | ${java.util.Date(file.lastModified())}")
            }
            Log.i("FileCheck", "========================")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

@Composable
fun ScannerScreen(
    onBackClick: () -> Unit = {},
    onCaptureClick: () -> Unit = {},
    onCheckFilesClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    // Calculate overlay dimensions for 120x70mm (approximately 3:1.75 ratio)
    val overlayWidth = screenWidth * 0.8f
    val overlayHeight = overlayWidth * 0.583f // 70/120 ratio
    
    // Camera state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
            == PackageManager.PERMISSION_GRANTED
        )
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            // Camera Preview
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onCameraReady = { imageCapture ->
                    (context as ScannerActivity).imageCapture = imageCapture
                }
            )
            
            // Document overlay frame
            Box(
                modifier = Modifier
                    .size(overlayWidth, overlayHeight)
                    .align(Alignment.Center)
                    .border(
                        width = 2.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp)
                    )
            )
        } else {
            // Fallback when no camera permission
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Permiso de cámara requerido",
                        fontSize = 24.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Para escanear documentos necesitas habilitar el permiso de cámara",
                        fontSize = 16.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // Top bar with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White
                )
            }
        }
        
        // Instructions and capture button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            ) {
                Text(
                    text = "Coloque el documento de 120x70mm dentro del marco blanco y toque el botón para capturar",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            Button(
                onClick = onCaptureClick,
                enabled = hasCameraPermission,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(
                    text = "📷",
                    fontSize = 24.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Botón temporal para verificar archivos guardados
            OutlinedButton(
                onClick = onCheckFilesClick,
                modifier = Modifier.padding(horizontal = 16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "📁 Verificar Archivos",
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onCameraReady: (ImageCapture) -> Unit
) {
    val composableContext = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    AndroidView(
        factory = { factoryContext ->
            PreviewView(factoryContext).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = modifier,
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(composableContext)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    
                    // Preview
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    
                    // ImageCapture
                    val imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build()
                    
                    // Select back camera as a default
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    
                    try {
                        // Unbind use cases before rebinding
                        cameraProvider.unbindAll()
                        
                        // Bind use cases to camera
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, 
                            cameraSelector, 
                            preview,
                            imageCapture
                        )
                        
                        // Notify that camera is ready
                        onCameraReady(imageCapture)
                        
                    } catch (exc: Exception) {
                        Log.e("CameraPreview", "Use case binding failed", exc)
                    }
                } catch (exc: Exception) {
                    Log.e("CameraPreview", "Camera provider initialization failed", exc)
                }
            }, ContextCompat.getMainExecutor(composableContext))
        }
    )
}
