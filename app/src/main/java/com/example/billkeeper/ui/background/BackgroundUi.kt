@file:Suppress("DEPRECATION")

package com.example.billkeeper.ui.background

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.example.billkeeper.background.BackgroundPreferences
import com.example.billkeeper.background.AppearancePreferences
import com.example.billkeeper.background.AppearanceSettings
import com.example.billkeeper.background.BackgroundStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val LIGHT_BACKGROUND_ALPHA = 0.45f
private const val DARK_BACKGROUND_ALPHA = 0.52f
private const val LIGHT_SCRIM_ALPHA = 0.08f
private const val DARK_SCRIM_ALPHA = 0.22f
private const val DEFAULT_THEME_SEED = 0xFF1B5E20.toInt()

private enum class ColorTarget {
    THEME,
    BACKGROUND
}

@Composable
fun AppBackground(
    preferences: BackgroundPreferences,
    settings: AppearanceSettings,
    darkTheme: Boolean,
    revision: Int,
    modifier: Modifier = Modifier
) {
    val showImage = settings.backgroundStyle == BackgroundStyle.IMAGE && preferences.hasCustomBackground
    val image by rememberFileBitmap(
        path = preferences.backgroundFile.absolutePath,
        load = showImage,
        revision = revision
    )
    val backgroundColor = if (!darkTheme && settings.backgroundStyle == BackgroundStyle.SOLID_COLOR) {
        settings.backgroundColorArgb?.let(::Color) ?: MaterialTheme.colorScheme.background
    } else {
        MaterialTheme.colorScheme.background
    }
    val imageAlpha = if (darkTheme) DARK_BACKGROUND_ALPHA else LIGHT_BACKGROUND_ALPHA
    val scrimColor = if (darkTheme) {
        Color.Black.copy(alpha = DARK_SCRIM_ALPHA)
    } else {
        Color.White.copy(alpha = LIGHT_SCRIM_ALPHA)
    }

    Box(modifier = modifier.background(backgroundColor)) {
        image?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = imageAlpha
            )
            Box(Modifier.fillMaxSize().background(scrimColor))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundSettingsDialog(
    preferences: BackgroundPreferences,
    appearancePreferences: AppearancePreferences,
    settings: AppearanceSettings,
    darkTheme: Boolean,
    onBackgroundChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var removeStoredImage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var pendingThemeColorArgb by remember(settings) { mutableStateOf(settings.themeSeedArgb) }
    var pendingBackgroundStyle by remember(settings) { mutableStateOf(settings.backgroundStyle) }
    var pendingBackgroundColorArgb by remember(settings) { mutableStateOf(settings.backgroundColorArgb) }
    var colorTarget by remember { mutableStateOf(ColorTarget.THEME) }
    val defaultBackgroundColor = MaterialTheme.colorScheme.background.toArgb()
    val activeColor = when (colorTarget) {
        ColorTarget.THEME -> Color(pendingThemeColorArgb ?: DEFAULT_THEME_SEED)
        ColorTarget.BACKGROUND -> Color(pendingBackgroundColorArgb ?: defaultBackgroundColor)
    }

    val cropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            pendingUri = result.uriContent
            pendingBackgroundStyle = BackgroundStyle.IMAGE
            removeStoredImage = false
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
        load = preferences.hasCustomBackground && !removeStoredImage,
        revision = 0
    )
    val displayedPreview = preview ?: currentBackground

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("外观设置") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!darkTheme) {
                    Text("自定义颜色", fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ColorTarget.values().forEachIndexed { index, target ->
                            SegmentedButton(
                                selected = colorTarget == target,
                                onClick = { colorTarget = target },
                                shape = SegmentedButtonDefaults.itemShape(index, ColorTarget.values().size),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (target == ColorTarget.THEME) "主题色" else "背景色")
                            }
                        }
                    }

                    HsvColorPicker(
                        color = activeColor,
                        onColorChange = { color ->
                            when (colorTarget) {
                                ColorTarget.THEME -> pendingThemeColorArgb = color.toArgb()
                                ColorTarget.BACKGROUND -> {
                                    pendingBackgroundColorArgb = color.toArgb()
                                    pendingBackgroundStyle = BackgroundStyle.SOLID_COLOR
                                }
                            }
                        }
                    )
                    ColorSwatches(
                        selectedColor = activeColor,
                        onColorSelected = { color ->
                            when (colorTarget) {
                                ColorTarget.THEME -> pendingThemeColorArgb = color.toArgb()
                                ColorTarget.BACKGROUND -> {
                                    pendingBackgroundColorArgb = color.toArgb()
                                    pendingBackgroundStyle = BackgroundStyle.SOLID_COLOR
                                }
                            }
                        }
                    )

                    AppearancePreview(
                        themeColor = Color(pendingThemeColorArgb ?: DEFAULT_THEME_SEED),
                        backgroundColor = if (pendingBackgroundStyle == BackgroundStyle.SOLID_COLOR) {
                            Color(pendingBackgroundColorArgb ?: defaultBackgroundColor)
                        } else {
                            MaterialTheme.colorScheme.background
                        }
                    )
                }

                Text("背景样式", fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    BackgroundStyle.values().forEachIndexed { index, style ->
                        SegmentedButton(
                            selected = pendingBackgroundStyle == style,
                            onClick = { pendingBackgroundStyle = style },
                            shape = SegmentedButtonDefaults.itemShape(index, BackgroundStyle.values().size),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(when (style) {
                                BackgroundStyle.SOLID_COLOR -> "纯色"
                                BackgroundStyle.IMAGE -> "图片"
                            })
                        }
                    }
                }

                if (pendingBackgroundStyle == BackgroundStyle.IMAGE) {
                    ImageBackgroundPreview(
                        image = displayedPreview,
                        darkTheme = darkTheme
                    )
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
                    Spacer(Modifier.width(6.dp))
                    Text(if (preferences.hasCustomBackground || pendingUri != null) "重新选择图片" else "选择背景图片")
                }

                if (preferences.hasCustomBackground || pendingUri != null) {
                    OutlinedButton(
                        onClick = {
                            pendingUri = null
                            removeStoredImage = true
                            pendingBackgroundStyle = BackgroundStyle.SOLID_COLOR
                        },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("移除背景图片") }
                }

                OutlinedButton(
                    onClick = {
                        pendingThemeColorArgb = null
                        pendingBackgroundStyle = BackgroundStyle.SOLID_COLOR
                        pendingBackgroundColorArgb = null
                        pendingUri = null
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("恢复默认外观") }

                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (isSaving) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (pendingBackgroundStyle == BackgroundStyle.IMAGE &&
                        pendingUri == null &&
                        (!preferences.hasCustomBackground || removeStoredImage)
                    ) {
                        errorMessage = "请先选择背景图片"
                        return@TextButton
                    }
                    isSaving = true
                    scope.launch {
                        runCatching {
                            pendingUri?.let { uri ->
                                withContext(Dispatchers.IO) { preferences.saveBackground(uri) }
                            }
                            if (removeStoredImage) {
                                withContext(Dispatchers.IO) { preferences.clearBackground() }
                            }
                            appearancePreferences.update(
                                AppearanceSettings(
                                    themeSeedArgb = pendingThemeColorArgb,
                                    backgroundStyle = pendingBackgroundStyle,
                                    backgroundColorArgb = pendingBackgroundColorArgb
                                )
                            )
                        }.onSuccess {
                            onBackgroundChanged()
                            onDismiss()
                        }.onFailure {
                            errorMessage = it.localizedMessage ?: "外观设置保存失败"
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving
            ) { Text("应用") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("取消") }
        }
    )
}

@Composable
private fun HsvColorPicker(
    color: Color,
    onColorChange: (Color) -> Unit
) {
    val hsv = remember(color) {
        FloatArray(3).also { AndroidColor.colorToHSV(color.toArgb(), it) }
    }
    val hueColor = Color(AndroidColor.HSVToColor(floatArrayOf(hsv[0], 1f, 1f)))

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(8.dp))
                .colorDrag { position, size ->
                    val saturation = (position.x / size.width).coerceIn(0f, 1f)
                    val value = (1f - position.y / size.height).coerceIn(0f, 1f)
                    onColorChange(Color(AndroidColor.HSVToColor(floatArrayOf(hsv[0], saturation, value))))
                }
        ) {
            drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            val selector = Offset(hsv[1] * size.width, (1f - hsv[2]) * size.height)
            drawCircle(Color.Black.copy(alpha = 0.7f), 10.dp.toPx(), selector, style = Stroke(4.dp.toPx()))
            drawCircle(Color.White, 8.dp.toPx(), selector, style = Stroke(3.dp.toPx()))
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .colorDrag { position, size ->
                    val hue = (position.x / size.width).coerceIn(0f, 1f) * 360f
                    onColorChange(Color(AndroidColor.HSVToColor(floatArrayOf(hue, hsv[1], hsv[2]))))
                }
        ) {
            drawRect(
                Brush.horizontalGradient(
                    listOf(
                        Color.Red,
                        Color.Yellow,
                        Color.Green,
                        Color.Cyan,
                        Color.Blue,
                        Color.Magenta,
                        Color.Red
                    )
                )
            )
            val x = hsv[0] / 360f * size.width
            drawLine(Color.White, Offset(x, 0f), Offset(x, size.height), 3.dp.toPx())
            drawLine(Color.Black.copy(alpha = 0.6f), Offset(x - 2.dp.toPx(), 0f), Offset(x - 2.dp.toPx(), size.height), 1.dp.toPx())
            drawLine(Color.Black.copy(alpha = 0.6f), Offset(x + 2.dp.toPx(), 0f), Offset(x + 2.dp.toPx(), size.height), 1.dp.toPx())
        }
    }
}

@Composable
private fun ColorSwatches(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    val colors = remember {
        listOf(
            0xFF1B5E20,
            0xFF1565C0,
            0xFF00695C,
            0xFF6A1B9A,
            0xFFC62828,
            0xFFEF6C00,
            0xFF37474F,
            0xFF7B1FA2
        ).map { Color(it) }
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(colors) { swatch ->
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(swatch)
                    .border(
                        width = if (swatch.toArgb() == selectedColor.toArgb()) 3.dp else 1.dp,
                        color = if (swatch.toArgb() == selectedColor.toArgb()) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(swatch) }
            )
        }
    }
}

@Composable
private fun AppearancePreview(
    themeColor: Color,
    backgroundColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("外观预览", color = backgroundColor.readableTextColor())
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeColor,
                    contentColor = themeColor.readableTextColor()
                )
            ) { Text("主题按钮") }
        }
    }
}

@Composable
private fun ImageBackgroundPreview(
    image: ImageBitmap?,
    darkTheme: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (image == null) {
            Text("尚未选择背景图片")
        } else {
            Image(
                bitmap = image,
                contentDescription = "背景预览",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = if (darkTheme) DARK_BACKGROUND_ALPHA else LIGHT_BACKGROUND_ALPHA
            )
            Box(
                Modifier.fillMaxSize().background(
                    if (darkTheme) Color.Black.copy(alpha = DARK_SCRIM_ALPHA)
                    else Color.White.copy(alpha = LIGHT_SCRIM_ALPHA)
                )
            )
        }
    }
}

private fun Modifier.colorDrag(onPosition: (Offset, IntSize) -> Unit): Modifier =
    this
        .pointerInput(onPosition) {
            detectTapGestures { position -> onPosition(position, size) }
        }
        .pointerInput(onPosition) {
            detectDragGestures { change, _ ->
                onPosition(change.position, size)
                change.consume()
            }
        }

private fun Color.readableTextColor(): Color =
    if (luminance() > 0.48f) Color(0xFF101410) else Color.White

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
