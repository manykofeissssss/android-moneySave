package com.example.billkeeper.background

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private val Context.appearanceDataStore by preferencesDataStore(name = "appearance_settings")

enum class BackgroundStyle {
    SOLID_COLOR,
    IMAGE
}

data class AppearanceSettings(
    val themeSeedArgb: Int? = null,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.SOLID_COLOR,
    val backgroundColorArgb: Int? = null
)

class AppearancePreferences(context: Context) {
    private val appContext = context.applicationContext
    private val legacyBackgroundPreferences = BackgroundPreferences(appContext)

    val initialSettings = AppearanceSettings(
        backgroundStyle = if (legacyBackgroundPreferences.hasCustomBackground) {
            BackgroundStyle.IMAGE
        } else {
            BackgroundStyle.SOLID_COLOR
        }
    )

    val settings: Flow<AppearanceSettings> = appContext.appearanceDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map(::toAppearanceSettings)

    suspend fun update(settings: AppearanceSettings) {
        appContext.appearanceDataStore.edit { preferences ->
            preferences.remove(LEGACY_THEME_MODE_KEY)
            preferences[BACKGROUND_STYLE_KEY] = settings.backgroundStyle.name
            preferences.putOrRemove(THEME_SEED_KEY, settings.themeSeedArgb)
            preferences.putOrRemove(BACKGROUND_COLOR_KEY, settings.backgroundColorArgb)
        }
    }

    private fun toAppearanceSettings(preferences: Preferences): AppearanceSettings = AppearanceSettings(
        themeSeedArgb = preferences[THEME_SEED_KEY],
        backgroundStyle = preferences[BACKGROUND_STYLE_KEY]?.toEnumOrNull<BackgroundStyle>()
            ?: initialSettings.backgroundStyle,
        backgroundColorArgb = preferences[BACKGROUND_COLOR_KEY]
    )

    private fun androidx.datastore.preferences.core.MutablePreferences.putOrRemove(
        key: Preferences.Key<Int>,
        value: Int?
    ) {
        if (value == null) remove(key) else this[key] = value
    }

    private inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? =
        enumValues<T>().firstOrNull { it.name == this }

    private companion object {
        val LEGACY_THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val THEME_SEED_KEY = intPreferencesKey("theme_seed_argb")
        val BACKGROUND_STYLE_KEY = stringPreferencesKey("background_style")
        val BACKGROUND_COLOR_KEY = intPreferencesKey("background_color_argb")
    }
}

class BackgroundPreferences(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val imageMutex = Mutex()
    private var cachedImage: CachedBackgroundImage? = null

    private data class CachedBackgroundImage(
        val modifiedAt: Long,
        val fileSize: Long,
        val sourceWidth: Int,
        val sourceHeight: Int,
        val sampleSize: Int,
        val bitmap: Bitmap
    )

    val backgroundFile: File
        get() = File(appContext.filesDir, BACKGROUND_FILE_NAME)

    val hasCustomBackground: Boolean
        get() = preferences.getBoolean(KEY_CUSTOM_BACKGROUND, false) && backgroundFile.isFile

    suspend fun loadBackgroundBitmap(maxWidth: Int = 1080, maxHeight: Int = 1920): Bitmap? =
        withContext(Dispatchers.IO) {
            imageMutex.withLock {
                if (!hasCustomBackground) {
                    cachedImage = null
                    return@withLock null
                }
                val file = backgroundFile
                val modifiedAt = file.lastModified()
                val fileSize = file.length()
                cachedImage?.let { cached ->
                    if (cached.modifiedAt == modifiedAt && cached.fileSize == fileSize &&
                        cached.sampleSize <= BackgroundImageSampling.sampleSize(
                            cached.sourceWidth, cached.sourceHeight, maxWidth, maxHeight
                        )
                    ) {
                        return@withLock cached.bitmap
                    }
                }

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                    cachedImage = null
                    return@withLock null
                }
                val sampleSize = BackgroundImageSampling.sampleSize(
                    bounds.outWidth, bounds.outHeight, maxWidth, maxHeight
                )
                val bitmap = BitmapFactory.decodeFile(
                    file.absolutePath,
                    BitmapFactory.Options().apply { inSampleSize = sampleSize }
                )
                cachedImage = bitmap?.let {
                    CachedBackgroundImage(
                        modifiedAt, fileSize, bounds.outWidth, bounds.outHeight, sampleSize, it
                    )
                }
                bitmap
            }
        }

    suspend fun saveBackground(source: Uri) = withContext(Dispatchers.IO) {
        imageMutex.withLock {
            val temporaryFile = File(appContext.filesDir, "$BACKGROUND_FILE_NAME.tmp")
            try {
                appContext.contentResolver.openInputStream(source).use { input ->
                    requireNotNull(input) { "无法读取裁剪后的图片" }
                    temporaryFile.outputStream().use(input::copyTo)
                }
                check(temporaryFile.length() > 0L) { "裁剪后的图片为空" }

                if (backgroundFile.exists() && !backgroundFile.delete()) {
                    error("无法替换原背景图片")
                }
                // The UI may still draw the old bitmap; release the cache reference without recycling it.
                cachedImage = null
                check(temporaryFile.renameTo(backgroundFile)) { "无法保存背景图片" }
                preferences.edit().putBoolean(KEY_CUSTOM_BACKGROUND, true).apply()
            } finally {
                temporaryFile.delete()
            }
        }
    }

    suspend fun clearBackground() = withContext(Dispatchers.IO) {
        imageMutex.withLock {
            backgroundFile.delete()
            cachedImage = null
            preferences.edit().remove(KEY_CUSTOM_BACKGROUND).apply()
        }
    }

    companion object {
        private const val PREFERENCES_NAME = "appearance_preferences"
        private const val KEY_CUSTOM_BACKGROUND = "custom_background"
        private const val BACKGROUND_FILE_NAME = "custom_background.jpg"
    }
}

internal object BackgroundImageSampling {
    fun sampleSize(sourceWidth: Int, sourceHeight: Int, maxWidth: Int, maxHeight: Int): Int {
        require(sourceWidth > 0 && sourceHeight > 0 && maxWidth > 0 && maxHeight > 0)
        var sampleSize = 1
        // Round up so odd source dimensions also fit the decode bounds.
        while (sampleSize < (1 shl 30) &&
            ((sourceWidth.toLong() + sampleSize - 1) / sampleSize > maxWidth ||
                (sourceHeight.toLong() + sampleSize - 1) / sampleSize > maxHeight)
        ) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
