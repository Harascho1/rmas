package rs.coffeeconquest.shared.dto

import kotlinx.serialization.Serializable
import rs.coffeeconquest.shared.model.Role

/**
 * Sign-up input. Firebase Auth stores the email and password; everything else
 * becomes the `users/{uid}` document.
 */
@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val displayName: String,
    val city: String? = null,
    /** Only HUNTER and OWNER may be requested at sign-up; STAFF and ADMIN are granted by an admin. */
    val role: Role = Role.HUNTER,
)
