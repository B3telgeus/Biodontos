package com.example.biodontos.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageProcessor(
    private val context: Context?,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    suspend fun compressUriToJpegBytes(
        uriString: String,
        maxDimension: Int = DEFAULT_MAX_DIMENSION,
        quality: Int = DEFAULT_JPEG_QUALITY
    ): ByteArray? = withContext(ioDispatcher) {
        val appContext = context ?: return@withContext null
        try {
            val uri = Uri.parse(uriString)
            val rawBytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext null
            compressRawBytesToJpeg(rawBytes, maxDimension, quality)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    suspend fun compressRawBytesToJpeg(
        rawBytes: ByteArray,
        maxDimension: Int = DEFAULT_MAX_DIMENSION,
        quality: Int = DEFAULT_JPEG_QUALITY
    ): ByteArray? = withContext(defaultDispatcher) {
        if (rawBytes.isEmpty()) return@withContext null
        try {
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOptions)
            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
                return@withContext null
            }

            val sampleSize = calculateInSampleSize(
                outWidth = boundsOptions.outWidth,
                outHeight = boundsOptions.outHeight,
                reqWidth = maxDimension,
                reqHeight = maxDimension
            )

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sampledBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
                ?: return@withContext null

            val exifOrientation = readExifOrientation(rawBytes)
            val orientedBitmap = applyExifOrientation(sampledBitmap, exifOrientation)
            val scaledBitmap = scaleDownIfNeeded(orientedBitmap, maxDimension)

            val output = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(10, 100), output)
            output.toByteArray()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    suspend fun decodeBase64ToBytes(dataUri: String?): ByteArray? = withContext(defaultDispatcher) {
        if (dataUri.isNullOrBlank()) return@withContext null
        try {
            val base64Part = if (dataUri.contains(",")) {
                dataUri.substringAfter(",")
            } else {
                dataUri
            }
            Base64.decode(base64Part.trim(), Base64.DEFAULT).takeIf { it.isNotEmpty() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun readExifOrientation(rawBytes: ByteArray): Int {
        return try {
            ByteArrayInputStream(rawBytes).use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return bitmap
        }
        return try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (_: Exception) {
            bitmap
        }
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val longestSide = maxOf(width, height)
        if (longestSide <= maxDimension || longestSide <= 0) return bitmap

        val scale = maxDimension.toFloat() / longestSide.toFloat()
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    companion object {
        const val DEFAULT_MAX_DIMENSION = 512
        const val DEFAULT_JPEG_QUALITY = 82

        fun calculateInSampleSize(
            outWidth: Int,
            outHeight: Int,
            reqWidth: Int,
            reqHeight: Int
        ): Int {
            var inSampleSize = 1
            if (outHeight > reqHeight || outWidth > reqWidth) {
                val halfHeight = outHeight / 2
                val halfWidth = outWidth / 2
                while (
                    (halfHeight / inSampleSize) >= reqHeight &&
                    (halfWidth / inSampleSize) >= reqWidth
                ) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize.coerceAtLeast(1)
        }
    }
}
