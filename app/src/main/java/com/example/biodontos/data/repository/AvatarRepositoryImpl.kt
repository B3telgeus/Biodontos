package com.example.biodontos.data.repository

import android.content.Context
import com.example.biodontos.core.network.SupabaseClient as AppSupabaseClient
import com.example.biodontos.core.util.ImageProcessor
import com.example.biodontos.domain.repository.AvatarRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AvatarRepositoryImpl(
    context: Context? = null,
    private val supabaseClient: SupabaseClient = AppSupabaseClient.client,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val imageProcessor: ImageProcessor = ImageProcessor(
        context = context?.applicationContext ?: context,
        ioDispatcher = ioDispatcher
    ),
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
    internal var storageUploader: (suspend (bucket: String, path: String, bytes: ByteArray) -> String)? = null
) : AvatarRepository {

    override suspend fun uploadDoctorAvatar(doctorId: String, imageUri: String): Result<String> {
        return withContext(ioDispatcher) {
            try {
                val cleanDoctorId = doctorId.trim()
                require(cleanDoctorId.isNotEmpty()) { "Identificador de doctor requerido" }
                require(imageUri.isNotBlank()) { "URI de imagen vacío" }

                if (imageUri.startsWith("http://", ignoreCase = true) ||
                    imageUri.startsWith("https://", ignoreCase = true)
                ) {
                    return@withContext Result.success(imageUri)
                }

                val jpegBytes = if (imageUri.startsWith("data:image", ignoreCase = true)) {
                    val raw = imageProcessor.decodeBase64ToBytes(imageUri)
                        ?: return@withContext Result.failure(IllegalArgumentException("No se pudo procesar la imagen seleccionada"))
                    imageProcessor.compressRawBytesToJpeg(raw) ?: raw
                } else {
                    imageProcessor.compressUriToJpegBytes(imageUri)
                        ?: return@withContext Result.failure(IllegalArgumentException("No se pudo procesar la imagen seleccionada"))
                }

                val objectPath = "$cleanDoctorId/avatar.jpg"
                val baseUrl = storageUploader?.invoke(BUCKET_DOCTOR_AVATARS, objectPath, jpegBytes)
                    ?: run {
                        val bucket = supabaseClient.storage.from(BUCKET_DOCTOR_AVATARS)
                        bucket.upload(path = objectPath, data = jpegBytes, upsert = true)
                        bucket.publicUrl(objectPath)
                    }

                val versionedUrl = if (baseUrl.contains("?")) {
                    "$baseUrl&v=${clockMillis()}"
                } else {
                    "$baseUrl?v=${clockMillis()}"
                }
                Result.success(versionedUrl)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
            }
        }
    }

    override suspend fun decodeBase64AvatarBytes(dataUri: String?): ByteArray? {
        return imageProcessor.decodeBase64ToBytes(dataUri)
    }

    companion object {
        const val BUCKET_DOCTOR_AVATARS = "doctor-avatars"
    }
}
