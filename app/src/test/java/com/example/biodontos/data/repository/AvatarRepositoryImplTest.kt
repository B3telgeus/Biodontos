package com.example.biodontos.data.repository

import android.graphics.Bitmap
import android.graphics.Color
import com.example.biodontos.core.util.ImageProcessor
import io.github.jan.supabase.SupabaseClient
import io.mockk.mockk
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AvatarRepositoryImplTest {

    private val supabaseClient: SupabaseClient = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun `calculateInSampleSize scales down large camera dimensions by powers of 2`() {
        val sampleSize = ImageProcessor.calculateInSampleSize(
            outWidth = 4032,
            outHeight = 3024,
            reqWidth = 512,
            reqHeight = 512
        )
        assertEquals(4, sampleSize)

        val smallSample = ImageProcessor.calculateInSampleSize(
            outWidth = 300,
            outHeight = 300,
            reqWidth = 512,
            reqHeight = 512
        )
        assertEquals(1, smallSample)
    }

    @Test
    fun `uploadDoctorAvatar compresses base64 image and uploads to doctor-avatars with cache busting`() = runTest(testDispatcher) {
        val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLUE)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        val base64 = android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP)
        val dataUri = "data:image/jpeg;base64,$base64"

        val processor = ImageProcessor(
            context = null,
            ioDispatcher = testDispatcher,
            defaultDispatcher = testDispatcher
        )
        var capturedBucket: String? = null
        var capturedPath: String? = null
        var capturedBytes: ByteArray? = null

        val repo = AvatarRepositoryImpl(
            context = null,
            supabaseClient = supabaseClient,
            ioDispatcher = testDispatcher,
            imageProcessor = processor,
            clockMillis = { 1700000000000L },
            storageUploader = { bucket, path, bytes ->
                capturedBucket = bucket
                capturedPath = path
                capturedBytes = bytes
                "https://example.supabase.co/storage/v1/object/public/$bucket/$path"
            }
        )

        val result = repo.uploadDoctorAvatar("doc-123", dataUri)

        assertTrue(result.isSuccess)
        assertEquals(
            "https://example.supabase.co/storage/v1/object/public/doctor-avatars/doc-123/avatar.jpg?v=1700000000000",
            result.getOrNull()
        )
        assertEquals("doctor-avatars", capturedBucket)
        assertEquals("doc-123/avatar.jpg", capturedPath)
        assertNotNull(capturedBytes)
        assertTrue((capturedBytes?.size ?: 0) > 0)
    }

    @Test
    fun `uploadDoctorAvatar fails when image cannot be decoded`() = runTest(testDispatcher) {
        val processor = ImageProcessor(
            context = null,
            ioDispatcher = testDispatcher,
            defaultDispatcher = testDispatcher
        )
        val repo = AvatarRepositoryImpl(
            context = null,
            supabaseClient = supabaseClient,
            ioDispatcher = testDispatcher,
            imageProcessor = processor
        )

        val result = repo.uploadDoctorAvatar("doc-123", "content://invalid/uri")
        assertTrue(result.isFailure)
    }

    @Test
    fun `uploadDoctorAvatar rethrows CancellationException`() = runTest(testDispatcher) {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        val base64 = android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP)
        val dataUri = "data:image/jpeg;base64,$base64"

        val processor = ImageProcessor(
            context = null,
            ioDispatcher = testDispatcher,
            defaultDispatcher = testDispatcher
        )
        val repo = AvatarRepositoryImpl(
            context = null,
            supabaseClient = supabaseClient,
            ioDispatcher = testDispatcher,
            imageProcessor = processor,
            storageUploader = { _, _, _ -> throw CancellationException("Cancelled upload") }
        )

        try {
            repo.uploadDoctorAvatar("doc-123", dataUri)
            fail("Expected CancellationException")
        } catch (e: CancellationException) {
            assertEquals("Cancelled upload", e.message)
        }
    }

    @Test
    fun `decodeBase64AvatarBytes decodes legacy data URI and returns null on blank`() = runTest(testDispatcher) {
        val processor = ImageProcessor(
            context = null,
            ioDispatcher = testDispatcher,
            defaultDispatcher = testDispatcher
        )
        val repo = AvatarRepositoryImpl(
            context = null,
            supabaseClient = supabaseClient,
            ioDispatcher = testDispatcher,
            imageProcessor = processor
        )

        assertNull(repo.decodeBase64AvatarBytes(null))
        assertNull(repo.decodeBase64AvatarBytes("   "))

        val bytes = byteArrayOf(1, 2, 3, 4)
        val encoded = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        val decoded = repo.decodeBase64AvatarBytes("data:image/jpeg;base64,$encoded")
        assertNotNull(decoded)
        assertEquals(4, decoded?.size)
    }
}
