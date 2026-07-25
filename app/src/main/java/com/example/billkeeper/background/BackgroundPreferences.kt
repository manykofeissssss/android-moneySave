package com.example.billkeeper.background

import android.content.Context
import android.net.Uri
import java.io.File

class BackgroundPreferences(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val backgroundFile: File
        get() = File(appContext.filesDir, BACKGROUND_FILE_NAME)

    val hasCustomBackground: Boolean
        get() = preferences.getBoolean(KEY_CUSTOM_BACKGROUND, false) && backgroundFile.isFile

    fun saveBackground(source: Uri) {
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
            check(temporaryFile.renameTo(backgroundFile)) { "无法保存背景图片" }
            preferences.edit().putBoolean(KEY_CUSTOM_BACKGROUND, true).apply()
        } finally {
            temporaryFile.delete()
        }
    }

    fun clearBackground() {
        backgroundFile.delete()
        preferences.edit().remove(KEY_CUSTOM_BACKGROUND).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "appearance_preferences"
        private const val KEY_CUSTOM_BACKGROUND = "custom_background"
        private const val BACKGROUND_FILE_NAME = "custom_background.jpg"
    }
}
