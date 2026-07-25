@file:Suppress("DEPRECATION")

package com.example.billkeeper.ui.background

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.example.billkeeper.background.BackgroundPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val BACKGROUND_ALPHA = 0.24f

@Composable
fun AppBackground(
    preferences: BackgroundPreferences,
    revision: Int,
    modifier: Modifier = Modifier
) {
    val image by rememberFileBitmap(
        path = preferences.backgroundFile.absolutePath,
        load = preferences.hasCustomBackground,
        revision = revision
    )

    Box(modifier = modifier.background(Color(0xFFFAFAFA))) {
        image?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = BACKGROUND_ALPHA
            )
        }
    }
}

@Composable
fun BackgroundSettingsDialog(
    preferences: BackgroundPreferences,
    onBackgroundChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val cropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            pendingUri = result.uriContent
            errorMessage = null
        } else if (result.error != null) {
            errorMessage = result.error?.localizedMessage ?: "图片裁剪失败"
        }
    }
    val imagePicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        uri?.let {
            cropLauncher.launch(
                CropImageContractOptions(
                    uri = it,
                    cropImageOptions = CropImageOptions(
                        fixAspectRatio = true,
                        aspectRatioX = 9,
                        aspectRatioY = 16,
                        guidelines = CropImageView.Guidelines.ON,
                        multiTouchEnabled = true,
                        outputCompressFormat = Bitmap.CompressFormat.JPEG,
                        outputCompressQuality = 90,
                        outputRequestWidth = 1080,
                        outputRequestHeight = 1920,
                        outputRequestSizeOptions = CropImageView.RequestSizeOptions.RESIZE_EXACT,
                        activityTitle = "裁剪背景图"
                    )
                )
            )
        }
    }

    val preview by rememberUriBitmap(pendingUri)
    val currentBackground by rememberFileBitmap(
        path = preferences.backgroundFile.absolutePath,
        load = preferences.hasCustomBackground,
        revision = 0
    )
    val displayedPreview = preview ?: currentBackground

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("自定义背景") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF0F2F0)),
                    contentAlignment = Alignment.Center
                ) {
                    if (displayedPreview != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(9f / 16f)
                                .background(Color(0xFFFAFAFA))
                        ) {
                            Image(
                                bitmap = displayedPreview,
                                contentDescription = "背景预览",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                alpha = BACKGROUND_ALPHA
                            )
                        }
                        Text(
                            text = if (pendingUri != null) "新背景预览" else "当前背景",
                            color = Color(0xFF1B5E20),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Text(if (preferences.hasCustomBackground) "选择图片以更换当前背景" else "尚未设置背景图片")
                        }
                    }
                    if (isSaving) CircularProgressIndicator()
                }

                Button(
                    onClick = {
                        errorMessage = null
                        imagePicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Text(if (preferences.hasCustomBackground || pendingUri != null) " 重新选择图片" else " 选择背景图片")
                }

                if (preferences.hasCustomBackground) {
                    OutlinedButton(
                        onClick = {
                            preferences.clearBackground()
                            pendingUri = null
                            onBackgroundChanged()
                            onDismiss()
                        },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("恢复默认背景")
                    }
                }

                errorMessage?.let { Text(it, color = Color(0xFFC62828)) }
            }
        },
        confirmButton = {
            if (pendingUri != null) {
                TextButton(
                    onClick = {
                        val uri = pendingUri ?: return@TextButton
                        isSaving = true
                        scope.launch {
                            runCatching {
                                withContext(Dispatchers.IO) { preferences.saveBackground(uri) }
                            }.onSuccess {
                                onBackgroundChanged()
                                onDismiss()
                            }.onFailure {
                                errorMessage = it.localizedMessage ?: "背景保存失败"
                                isSaving = false
                            }
                        }
                    },
                    enabled = !isSaving
                ) {
                    Text("应用")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("取消") }
        }
    )
}

@Composable
private fun rememberFileBitmap(path: String, load: Boolean, revision: Int) = produceState<ImageBitmap?>(
    initialValue = null,
    key1 = path,
    key2 = load,
    key3 = revision
) {
    value = if (load) {
        withContext(Dispatchers.IO) { BitmapFactory.decodeFile(path)?.asImageBitmap() }
    } else {
        null
    }
}

@Composable
private fun rememberUriBitmap(uri: Uri?) = run {
    val context = LocalContext.current
    produceState<ImageBitmap?>(initialValue = null, key1 = uri) {
        value = uri?.let {
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(it).use { stream ->
                    stream?.let(BitmapFactory::decodeStream)?.asImageBitmap()
                }
            }
        }
    }
}
