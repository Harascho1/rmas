package rs.coffeeconquest.app.data.firebase

import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import rs.coffeeconquest.shared.dto.UserProfile
import rs.coffeeconquest.shared.model.Role

class AdminSource {
    suspend fun allUsers(limit: Int = 100): List<UserProfile> =
        Fire.users()
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .fetch()
            .map { it.toUserProfile() }

    suspend fun setBanned(userId: String, banned: Boolean, reason: String) {
        Fire.user(userId).update(
            mapOf(
                "isBanned" to banned,
                "banReason" to reason.takeIf { banned },
            ),
        ).await()
    }

    suspend fun setRole(userId: String, role: Role) {
        Fire.user(userId).update("role", role.name).await()
    }
}
