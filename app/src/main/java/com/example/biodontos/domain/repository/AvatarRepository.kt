package com.example.biodontos.domain.repository

interface AvatarRepository {
    suspend fun uploadDoctorAvatar(doctorId: String, imageUri: String): Result<String>
    suspend fun decodeBase64AvatarBytes(dataUri: String?): ByteArray?
}
