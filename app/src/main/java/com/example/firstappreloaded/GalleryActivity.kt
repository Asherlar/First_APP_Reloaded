package com.example.firstappreloaded

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BitmapFactory
import android.util.Log
import com.example.firstappreloaded.ui.theme.FirstAppReloadedTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class GalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FirstAppReloadedTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GalleryScreen(
                        modifier = Modifier.padding(innerPadding),
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var documentFiles by remember { mutableStateOf<List<DocumentFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Cargar archivos al iniciar
    LaunchedEffect(Unit) {
        documentFiles = loadDocumentFiles(context as GalleryActivity)
        isLoading = false
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Galería de Documentos",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Cargando documentos...")
                }
            }
        } else if (documentFiles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📁",
                        fontSize = 64.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay documentos",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Escanea tu primer documento para verlo aquí",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Documentos encontrados: ${documentFiles.size}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(documentFiles) { document ->
                        DocumentCard(
                            document = document,
                            onClick = { /* TODO: Implementar vista detallada */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentCard(
    document: DocumentFile,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Imagen del documento
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color.Gray.copy(alpha = 0.1f))
            ) {
                document.bitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Documento escaneado",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: run {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📄",
                            fontSize = 32.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Información del documento
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = document.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = document.dateFormatted,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                Text(
                    text = document.sizeFormatted,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

data class DocumentFile(
    val file: File,
    val name: String,
    val dateFormatted: String,
    val sizeFormatted: String,
    val bitmap: android.graphics.Bitmap?
)

private fun loadDocumentFiles(context: GalleryActivity): List<DocumentFile> {
    val documentsDir = getDocumentsDirectory(context)
    Log.d("Gallery", "Buscando documentos en: ${documentsDir.absolutePath}")
    
    val files = documentsDir.listFiles { file -> 
        file.isFile && file.name.startsWith("scan_") && file.name.endsWith(".jpg")
    }?.sortedByDescending { it.lastModified() } ?: emptyList()
    
    Log.d("Gallery", "Archivos encontrados: ${files.size}")
    
    return files.map { file ->
        val bitmap = try {
            // Crear bitmap escalado para miniatura
            val options = BitmapFactory.Options().apply {
                inSampleSize = 4 // Reducir tamaño para mejor rendimiento
                inJustDecodeBounds = false
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: Exception) {
            Log.e("Gallery", "Error al cargar imagen: ${file.name}", e)
            null
        }
        
        DocumentFile(
            file = file,
            name = file.name.removePrefix("scan_").removeSuffix(".jpg"),
            dateFormatted = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(Date(file.lastModified())),
            sizeFormatted = formatFileSize(file.length()),
            bitmap = bitmap
        )
    }
}

private fun getDocumentsDirectory(context: GalleryActivity): File {
    // Usar la misma lógica que en ScannerActivity para encontrar documentos
    val candidates = listOf(
        context.getExternalFilesDir(null),
        context.getExternalFilesDir("Documents"),
        context.filesDir,
        context.cacheDir
    )
    
    for (dir in candidates) {
        if (dir != null && dir.exists()) {
            Log.d("Gallery", "Usando directorio: ${dir.absolutePath}")
            return dir
        }
    }
    
    return context.filesDir
}

private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    
    return when {
        mb >= 1 -> String.format("%.1f MB", mb)
        kb >= 1 -> String.format("%.1f KB", kb)
        else -> "$bytes B"
    }
}
