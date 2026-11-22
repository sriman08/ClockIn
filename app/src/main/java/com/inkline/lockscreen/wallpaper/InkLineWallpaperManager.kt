package com.inkline.lockscreen.wallpaper

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.WindowManager
import com.inkline.lockscreen.data.TaskRepository
import com.inkline.lockscreen.data.prefs.ThemeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

sealed class WallpaperApplyResult {
    data object Success : WallpaperApplyResult()
    data class RestrictedFallbackNeeded(val reason: String) : WallpaperApplyResult()
    data class Error(val message: String) : WallpaperApplyResult()
}

class InkLineWallpaperManager(
    private val context: Context,
    private val taskRepository: TaskRepository,
    private val themeRepository: ThemeRepository
) {

    private val applyMutex = Mutex()

    suspend fun renderAndApplyLockScreen(today: LocalDate = LocalDate.now()): WallpaperApplyResult =
        applyMutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    val wallpaperManager = WallpaperManager.getInstance(context)
                    if (!wallpaperManager.isWallpaperSupported || !wallpaperManager.isSetWallpaperAllowed) {
                        return@withContext WallpaperApplyResult.RestrictedFallbackNeeded(
                            "This device restricts automatic lock-screen wallpaper updates. Use Save PNG & Set Manually below."
                        )
                    }

                    val bitmap = renderCurrentBitmap(today)
                    try {
                        val cropHint = Rect(0, 0, bitmap.width, bitmap.height)
                        wallpaperManager.setBitmap(
                            bitmap,
                            cropHint,
                            false,
                            WallpaperManager.FLAG_LOCK
                        )
                        WallpaperApplyResult.Success
                    } finally {
                        bitmap.recycle()
                    }
                } catch (sec: SecurityException) {
                    WallpaperApplyResult.RestrictedFallbackNeeded(
                        "Your device blocked automatic lock-screen updates (${sec.message ?: "SecurityException"}). Use Save PNG & Set Manually."
                    )
                } catch (e: Exception) {
                    WallpaperApplyResult.Error(e.message ?: "Unable to update lock-screen wallpaper.")
                }
            }
        }

    suspend fun exportLockScreenImageToGallery(today: LocalDate = LocalDate.now()): Result<Uri> =
        applyMutex.withLock {
            withContext(Dispatchers.IO) {
                runCatching {
                    val bitmap = renderCurrentBitmap(today)
                    try {
                        val filename = "clockin_LockScreen_${today}_${System.currentTimeMillis() % 10000}.png"
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val values = ContentValues().apply {
                                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                                put(
                                    MediaStore.Images.Media.RELATIVE_PATH,
                                    "${Environment.DIRECTORY_PICTURES}/clock.in"
                                )
                                put(MediaStore.Images.Media.IS_PENDING, 1)
                            }
                            val resolver = context.contentResolver
                            val uri = resolver.insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                values
                            ) ?: error("Could not create MediaStore image entry")

                            resolver.openOutputStream(uri)?.use { out ->
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            } ?: error("Could not open output stream for image")

                            values.clear()
                            values.put(MediaStore.Images.Media.IS_PENDING, 0)
                            resolver.update(uri, values, null, null)
                            uri
                        } else {
                            val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                                ?: context.filesDir
                            val outFile = File(dir, filename)
                            FileOutputStream(outFile).use { out ->
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                            Uri.fromFile(outFile)
                        }
                    } finally {
                        bitmap.recycle()
                    }
                }
            }
        }

    fun launchSystemWallpaperPicker(imageUri: Uri? = null): Boolean {
        val wallpaperManager = WallpaperManager.getInstance(context)

        if (imageUri != null) {
            val cropIntent = runCatching {
                wallpaperManager.getCropAndSetWallpaperIntent(imageUri).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }.getOrNull()

            if (cropIntent != null && runCatching { context.startActivity(cropIntent) }.isSuccess) {
                return true
            }

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(imageUri, "image/png")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (runCatching {
                    context.startActivity(
                        Intent.createChooser(viewIntent, "Open in Gallery to Set as Lock Screen")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }.isSuccess
            ) {
                return true
            }
        }

        val fallbackIntent = Intent(Intent.ACTION_SET_WALLPAPER).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(
                Intent.createChooser(fallbackIntent, "Set Lock Screen Wallpaper")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.isSuccess
    }

    private suspend fun renderCurrentBitmap(today: LocalDate): Bitmap {
        val settings = themeRepository.getCurrentSettings()
        val todayTask = taskRepository.getTaskForDate(today)
        val (widthPx, heightPx, density) = resolveDisplayMetrics()

        return InkLineBitmapRenderer.render(
            widthPx = widthPx,
            heightPx = heightPx,
            density = density,
            today = today,
            todayTask = todayTask,
            settings = settings
        )
    }

    private fun resolveDisplayMetrics(): Triple<Int, Int, Float> {
        val appDensity = context.resources.displayMetrics.density
            .takeIf { it > 0f }
            ?: Resources.getSystem().displayMetrics.density.coerceAtLeast(1f)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wm = context.getSystemService(WindowManager::class.java)
            val bounds = wm?.maximumWindowMetrics?.bounds
            if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
                return Triple(bounds.width(), bounds.height(), appDensity)
            }
        }
        val metrics = context.resources.displayMetrics
        return Triple(metrics.widthPixels, metrics.heightPixels, appDensity)
    }
}
