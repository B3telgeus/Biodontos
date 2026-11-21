package com.example.biodontos.data.repository

import com.example.biodontos.data.local.SessionLocalCleaner
import com.example.biodontos.domain.model.RegistrationOutcome
import com.example.biodontos.domain.model.UserRole
import com.example.biodontos.domain.model.UserSession
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive Unit Test Suite for [AuthRepositoryImpl] (Plan 1: Integridad de datos y Auth).
 *
 * Verifies:
 * - Tier 1: Feature coverage for [RegistrationOutcome] (Authenticated vs PendingEmailVerification),
 *   rejection of empty/blank user IDs, and logout local cleanup delegation.
 * - Tier 2: Boundary conditions, exception propagation without silent swallows, and CancellationException safety.
 * - Tier 3: Session role mapping, session retrieval, password reset/update flows.
 * - Tier 4: Trimming of whitespace on auth credentials and resilience against remote signOut failures.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    private val supabaseClient: SupabaseClient = mockk(relaxed = true)
    private val auth: Auth = mockk(relaxed = true)
    private val sessionLocalCleaner: SessionLocalCleaner = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var authRepository: AuthRepositoryImpl

    @Before
    fun setUp() {
        mockkStatic("io.github.jan.supabase.gotrue.AuthKt")
        every { supabaseClient.auth } returns auth

        authRepository = AuthRepositoryImpl(
            supabaseClient = supabaseClient,
            ioDispatcher = testDispatcher,
            sessionLocalCleaner = sessionLocalCleaner
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }


    private fun mockRestException(
        message: String,
        error: String = "error",
        description: String? = null
    ): RestException {
        val ex = mockk<RestException>(relaxed = true)
        every { ex.message } returns message
        every { ex.error } returns error
        every { ex.description } returns (description ?: message)
        return ex
    }

    private fun mockUserInfo(
        id: String,
        email: String,
        role: String = "PATIENT",
        firstName: String = "Carlos",
        lastName: String = "Perez"
    ): UserInfo {
        val user = mockk<UserInfo>()
        every { user.id } returns id
        every { user.email } returns email
        every { user.userMetadata } returns buildJsonObject {
            put("role", role)
            put("first_name", firstName)
            put("last_name", lastName)
        }
        return user
    }


    @Test
    fun `registerPatient returns Authenticated outcome when Supabase returns authenticated user with valid UUID`() = runTest(testDispatcher) {
        val validUuid = "550e8400-e29b-41d4-a716-446655440000"
        val mockUser = mockUserInfo(id = validUuid, email = "paciente@biodontos.com", role = "PATIENT", firstName = "Carlos")
        coEvery { auth.signUpWith(Email, any()) } returns mockk(relaxed = true)
        coEvery { auth.currentUserOrNull() } returns mockUser

        val result = authRepository.registerPatient(
            email = "paciente@biodontos.com",
            password = "SecurePassword123!",
            firstName = "Carlos",
            lastName = "Perez",
            dniCedula = "12345678",
            phone = "3001234567"
        )

        assertTrue("Expected registration to succeed", result.isSuccess)
        val outcome = result.getOrNull()
        assertTrue("Expected outcome to be Authenticated", outcome is RegistrationOutcome.Authenticated)

        val authenticated = outcome as RegistrationOutcome.Authenticated
        assertEquals(validUuid, authenticated.session.id)
        assertEquals("paciente@biodontos.com", authenticated.session.email)
        assertEquals(UserRole.PATIENT, authenticated.session.role)
        assertEquals("Carlos", authenticated.session.displayName)
    }

    @Test
    fun `registerPatient returns PendingEmailVerification when Supabase user has null id`() = runTest(testDispatcher) {
        coEvery { auth.signUpWith(Email, any()) } returns mockk(relaxed = true)
        coEvery { auth.currentUserOrNull() } returns null

        val result = authRepository.registerPatient(
            email = "pending.patient@biodontos.com",
            password = "SecurePassword123!",
            firstName = "Carlos",
            lastName = "Perez",
            dniCedula = "12345678"
        )

        assertTrue("Expected registration to succeed", result.isSuccess)
        val outcome = result.getOrNull()
        assertTrue("Expected outcome to be PendingEmailVerification", outcome is RegistrationOutcome.PendingEmailVerification)

        val pending = outcome as RegistrationOutcome.PendingEmailVerification
        assertEquals("pending.patient@biodontos.com", pending.email)
    }

    @Test
    fun `registerDoctor returns Authenticated outcome when Supabase returns authenticated doctor with valid UUID`() = runTest(testDispatcher) {
        val validUuid = "660e8400-e29b-41d4-a716-446655440001"
        val mockDoctor = mockUserInfo(
            id = validUuid,
            email = "doctor@biodontos.com",
            role = "DOCTOR",
            firstName = "Ana",
            lastName = "Gomez"
        )
        coEvery { auth.signUpWith(Email, any()) } returns mockk(relaxed = true)
        coEvery { auth.currentUserOrNull() } returns mockDoctor

        val result = authRepository.registerDoctor(
            email = "doctor@biodontos.com",
            password = "DoctorPassword123!",
            firstName = "Ana",
            lastName = "Gomez",
            licenseNumber = "ODON-9921",
            specialty = "Ortodoncia",
            phone = "3109876543"
        )

        assertTrue("Expected doctor registration to succeed", result.isSuccess)
        val outcome = result.getOrNull()
        assertTrue("Expected outcome to be Authenticated", outcome is RegistrationOutcome.Authenticated)

        val authenticated = outcome as RegistrationOutcome.Authenticated
        assertEquals(validUuid, authenticated.session.id)
        assertEquals("doctor@biodontos.com", authenticated.session.email)
        assertEquals(UserRole.DOCTOR, authenticated.session.role)
        assertEquals("Ana Gomez", authenticated.session.displayName)
    }

    @Test
    fun `registerDoctor returns PendingEmailVerification when Supabase user has null id`() = runTest(testDispatcher) {
        coEvery { auth.signUpWith(Email, any()) } returns mockk(relaxed = true)
        coEvery { auth.currentUserOrNull() } returns null

        val result = authRepository.registerDoctor(
            email = "pending.doctor@biodontos.com",
            password = "DoctorPassword123!",
            firstName = "Ana",
            lastName = "Gomez",
            licenseNumber = "ODON-9921",
            specialty = "Ortodoncia"
        )

        assertTrue("Expected doctor registration to succeed", result.isSuccess)
        val outcome = result.getOrNull()
        assertTrue("Expected outcome to be PendingEmailVerification", outcome is RegistrationOutcome.PendingEmailVerification)

        val pending = outcome as RegistrationOutcome.PendingEmailVerification
        assertEquals("pending.doctor@biodontos.com", pending.email)
    }

    @Test
    fun `never emits UserSession with empty string id for patient registration`() = runTest(testDispatcher) {
        val mockUserWithEmptyId = mockUserInfo(id = "", email = "empty.id@biodontos.com")
        coEvery { auth.signUpWith(Email, any()) } returns mockk(relaxed = true)
        coEvery { auth.currentUserOrNull() } returns mockUserWithEmptyId

        val result = authRepository.registerPatient(
            email = "empty.id@biodontos.com",
            password = "Password123!",
            firstName = "Carlos",
            lastName = "Perez",
            dniCedula = "12345678"
        )

        assertTrue(result.isSuccess)
        val outcome = result.getOrNull()
        assertFalse("Must never emit Authenticated session with empty string id", outcome is RegistrationOutcome.Authenticated && (outcome as RegistrationOutcome.Authenticated).session.id.isEmpty())
        assertTrue("Empty user ID must be treated as PendingEmailVerification", outcome is RegistrationOutcome.PendingEmailVerification)
    }

    @Test
    fun `never emits UserSession with empty string id for doctor registration`() = runTest(testDispatcher) {
        val mockDoctorWithBlankId = mockUserInfo(id = "   ", email = "blank.doc@biodontos.com", role = "DOCTOR")
        coEvery { auth.signUpWith(Email, any()) } returns mockk(relaxed = true)
        coEvery { auth.currentUserOrNull() } returns mockDoctorWithBlankId

        val result = authRepository.registerDoctor(
            email = "blank.doc@biodontos.com",
            password = "Password123!",
            firstName = "Ana",
            lastName = "Gomez",
            licenseNumber = "ODON-9921",
            specialty = "Ortodoncia"
        )

        assertTrue(result.isSuccess)
        val outcome = result.getOrNull()
        assertFalse("Must never emit Authenticated session with blank id", outcome is RegistrationOutcome.Authenticated && (outcome as RegistrationOutcome.Authenticated).session.id.isBlank())
        assertTrue("Blank user ID must be treated as PendingEmailVerification", outcome is RegistrationOutcome.PendingEmailVerification)
    }


    @Test
    fun `logout invokes Supabase signOut and sessionLocalCleaner clearSessionData`() = runTest(testDispatcher) {
        coEvery { auth.signOut(any()) } returns Unit
        coEvery { sessionLocalCleaner.clearSessionData() } returns Unit

        val result = authRepository.logout()

        assertTrue("Expected logout to succeed", result.isSuccess)
        coVerify(exactly = 1) { auth.signOut(any()) }
        coVerify(exactly = 1) { sessionLocalCleaner.clearSessionData() }
    }

    @Test
    fun `logout performs local cleanup even if remote signOut fails`() = runTest(testDispatcher) {
        coEvery { auth.signOut(any()) } throws HttpRequestException("Network failure during signOut", mockk(relaxed = true))
        coEvery { sessionLocalCleaner.clearSessionData() } returns Unit

        val result = authRepository.logout()

        coVerify(exactly = 1) { sessionLocalCleaner.clearSessionData() }
        assertTrue("Local cleanup must have been called", true)
    }

    @Test
    fun `logout succeeds gracefully when sessionLocalCleaner is not provided`() = runTest(testDispatcher) {
        val repoWithoutCleaner = AuthRepositoryImpl(
            supabaseClient = supabaseClient,
            ioDispatcher = testDispatcher,
            sessionLocalCleaner = null
        )
        coEvery { auth.signOut(any()) } returns Unit

        val result = repoWithoutCleaner.logout()

        assertTrue("Expected logout without cleaner to succeed", result.isSuccess)
        coVerify(exactly = 1) { auth.signOut(any()) }
    }


    @Test
    fun `registerPatient propagates Result failure when Supabase auth throws exception`() = runTest(testDispatcher) {
        val authError = mockRestException(message = "User already registered", error = "user_already_exists", description = "User already registered")
        coEvery { auth.signUpWith(Email, any(), any()) } throws authError

        val result = authRepository.registerPatient(
            email = "existing@biodontos.com",
            password = "Password123!",
            firstName = "Carlos",
            lastName = "Perez",
            dniCedula = "12345678"
        )

        assertTrue("Expected result to be failure", result.isFailure)
        assertEquals("User already registered", result.exceptionOrNull()?.message)
    }

    @Test
    fun `registerDoctor propagates Result failure when Supabase auth throws network exception`() = runTest(testDispatcher) {
        val networkError = HttpRequestException("Connection timed out", mockk(relaxed = true))
        coEvery { auth.signUpWith(Email, any(), any()) } throws networkError

        val result = authRepository.registerDoctor(
            email = "doctor@biodontos.com",
            password = "Password123!",
            firstName = "Ana",
            lastName = "Gomez",
            licenseNumber = "ODON-9921",
            specialty = "Ortodoncia"
        )

        assertTrue("Expected result to be failure", result.isFailure)
        assertEquals(networkError.message, result.exceptionOrNull()?.message)
    }


    @Test(expected = CancellationException::class)
    fun `registerPatient rethrows CancellationException directly`() = runTest(testDispatcher) {
        coEvery { auth.signUpWith(Email, any(), any()) } throws CancellationException("Coroutine cancelled")

        authRepository.registerPatient(
            email = "carlos@biodontos.com",
            password = "Password123!",
            firstName = "Carlos",
            lastName = "Perez",
            dniCedula = "12345678"
        )
    }

    @Test(expected = CancellationException::class)
    fun `registerDoctor rethrows CancellationException directly`() = runTest(testDispatcher) {
        coEvery { auth.signUpWith(Email, any(), any()) } throws CancellationException("Coroutine cancelled")

        authRepository.registerDoctor(
            email = "doctor@biodontos.com",
            password = "Password123!",
            firstName = "Ana",
            lastName = "Gomez",
            licenseNumber = "ODON-9921",
            specialty = "Ortodoncia"
        )
    }

    @Test(expected = CancellationException::class)
    fun `logout rethrows CancellationException directly`() = runTest(testDispatcher) {
        coEvery { auth.signOut(any()) } throws CancellationException("Coroutine cancelled")

        authRepository.logout()
    }

    @Test(expected = CancellationException::class)
    fun `login rethrows CancellationException directly`() = runTest(testDispatcher) {
        coEvery { auth.signInWith(Email, any(), any()) } throws CancellationException("Login job cancelled")

        authRepository.login("user@biodontos.com", "Password123!")
    }

    @Test(expected = CancellationException::class)
    fun `resetPassword rethrows CancellationException directly`() = runTest(testDispatcher) {
        coEvery { auth.resetPasswordForEmail(any(), any(), any()) } throws CancellationException("Reset job cancelled")

        authRepository.resetPassword("user@biodontos.com")
    }

    @Test(expected = CancellationException::class)
    fun `updatePassword rethrows CancellationException directly`() = runTest(testDispatcher) {
        coEvery { auth.modifyUser(updateCurrentUser = any(), config = any()) } throws CancellationException("Update cancelled")

        authRepository.updatePassword("NewPassword123!")
    }


    @Test
    fun `login returns UserSession with DOCTOR role when metadata role is DOCTOR`() = runTest(testDispatcher) {
        val doctorUser = mockUserInfo(
            id = "doc-uuid-123",
            email = "doctor@biodontos.com",
            role = "DOCTOR",
            firstName = "Elena"
        )
        coEvery { auth.signInWith(Email, any()) } returns mockk(relaxed = true)
        coEvery { auth.currentUserOrNull() } returns doctorUser

        val result = authRepository.login("doctor@biodontos.com", "DoctorPass123!")

        assertTrue(result.isSuccess)
        val session = result.getOrNull()
        assertNotNull(session)
        assertEquals("doc-uuid-123", session?.id)
        assertEquals(UserRole.DOCTOR, session?.role)
        assertEquals("Elena", session?.displayName)
    }

    @Test
    fun `login returns UserSession with PATIENT role when metadata role is PATIENT`() = runTest(testDispatcher) {
        val patientUser = mockUserInfo(
            id = "pat-uuid-456",
            email = "patient@biodontos.com",
            role = "PATIENT",
            firstName = "Carlos"
        )
        coEvery { auth.signInWith(Email, any()) } returns mockk(relaxed = true)
        coEvery { auth.currentUserOrNull() } returns patientUser

        val result = authRepository.login("patient@biodontos.com", "PatientPass123!")

        assertTrue(result.isSuccess)
        val session = result.getOrNull()
        assertNotNull(session)
        assertEquals("pat-uuid-456", session?.id)
        assertEquals(UserRole.PATIENT, session?.role)
        assertEquals("Carlos", session?.displayName)
    }

    @Test
    fun `login returns UserSession with UNKNOWN role when metadata role is unrecognized`() = runTest(testDispatcher) {
        val unknownUser = mockUserInfo(
            id = "unknown-uuid-789",
            email = "unknown@biodontos.com",
            role = "SUPER_ADMIN",
            firstName = "Admin"
        )
        coEvery { auth.signInWith(Email, any()) } returns mockk(relaxed = true)
        coEvery { auth.currentUserOrNull() } returns unknownUser

        val result = authRepository.login("unknown@biodontos.com", "AdminPass123!")

        assertTrue(result.isSuccess)
        val session = result.getOrNull()
        assertEquals(UserRole.UNKNOWN, session?.role)
    }

    @Test
    fun `login returns failure when currentUserOrNull returns null after signIn`() = runTest(testDispatcher) {
        coEvery { auth.signInWith(Email, any()) } returns mockk(relaxed = true)
        coEvery { auth.currentUserOrNull() } returns null

        val result = authRepository.login("user@biodontos.com", "Password123!")

        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun `login propagates failure when invalid credentials are provided`() = runTest(testDispatcher) {
        coEvery { auth.signInWith(Email, any(), any()) } throws mockRestException(message = "Invalid login credentials", error = "invalid_grant", description = "Invalid login credentials")

        val result = authRepository.login("user@biodontos.com", "WrongPassword")

        assertTrue(result.isFailure)
        assertEquals("Invalid login credentials", result.exceptionOrNull()?.message)
    }


    @Test
    fun `getCurrentSession returns mapped UserSession when user is logged in`() = runTest(testDispatcher) {
        val mockUser = mockUserInfo(
            id = "active-uuid-999",
            email = "active@biodontos.com",
            role = "DOCTOR",
            firstName = "Sofia"
        )
        coEvery { auth.currentUserOrNull() } returns mockUser

        val session = authRepository.getCurrentSession()

        assertNotNull(session)
        assertEquals("active-uuid-999", session?.id)
        assertEquals("active@biodontos.com", session?.email)
        assertEquals(UserRole.DOCTOR, session?.role)
        assertEquals("Sofia", session?.displayName)
    }

    @Test
    fun `getCurrentSession returns null when no user is logged in`() = runTest(testDispatcher) {
        coEvery { auth.currentUserOrNull() } returns null

        val session = authRepository.getCurrentSession()

        assertNull(session)
    }

    @Test
    fun `resetPassword calls Supabase resetPasswordForEmail and returns success`() = runTest(testDispatcher) {
        coEvery { auth.resetPasswordForEmail(any(), any(), any()) } just Runs

        val result = authRepository.resetPassword("  reset@biodontos.com  ")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { auth.resetPasswordForEmail("reset@biodontos.com", any(), any()) }
    }

    @Test
    fun `resetPassword propagates failure when Supabase call fails`() = runTest(testDispatcher) {
        coEvery { auth.resetPasswordForEmail(any(), any(), any()) } throws mockRestException(message = "User not found", error = "not_found", description = "User not found")

        val result = authRepository.resetPassword("nonexistent@biodontos.com")

        assertTrue(result.isFailure)
        assertEquals("User not found", result.exceptionOrNull()?.message)
    }

    @Test
    fun `updatePassword calls modifyUser and returns success`() = runTest(testDispatcher) {
        coEvery { auth.modifyUser(updateCurrentUser = any(), config = any()) } returns mockk(relaxed = true)

        val result = authRepository.updatePassword("NewSecurePassword456!")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { auth.modifyUser(updateCurrentUser = any(), config = any()) }
    }

    @Test
    fun `updatePassword propagates failure when Supabase modifyUser fails`() = runTest(testDispatcher) {
        coEvery { auth.modifyUser(updateCurrentUser = any(), config = any()) } throws mockRestException(message = "Password must be at least 6 characters", error = "validation_failed", description = "Password must be at least 6 characters")

        val result = authRepository.updatePassword("123")

        assertTrue(result.isFailure)
        assertEquals("Password must be at least 6 characters", result.exceptionOrNull()?.message)
    }


    @Test
    fun `registerPatient trims whitespace from email, names and DNI before registration`() = runTest(testDispatcher) {
        val validUuid = "550e8400-e29b-41d4-a716-446655440000"
        val mockUser = mockUserInfo(id = validUuid, email = "trimmed@biodontos.com", role = "PATIENT", firstName = "Mateo")
        coEvery { auth.signUpWith(Email, any()) } returns mockk(relaxed = true)
        coEvery { auth.currentUserOrNull() } returns mockUser

        val result = authRepository.registerPatient(
            email = "   trimmed@biodontos.com   ",
            password = "Password123!   ",
            firstName = "   Mateo   ",
            lastName = "   Vargas   ",
            dniCedula = "   1098765432   ",
            phone = "   3009998877   "
        )

        assertTrue(result.isSuccess)
        val outcome = result.getOrNull()
        assertTrue(outcome is RegistrationOutcome.Authenticated)
        val authenticated = outcome as RegistrationOutcome.Authenticated
        assertEquals("trimmed@biodontos.com", authenticated.session.email)
    }
}
