package rs.coffeeconquest.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.ui.graphics.vector.ImageVector
import rs.coffeeconquest.shared.model.Role

/** Every destination in the app, in one place. */
object Routes {
    const val AUTH = "auth"
    const val MAP = "map"
    const val LEADERBOARD = "leaderboard"
    const val FEED = "feed"
    const val PROFILE = "profile"
    const val OWNER = "owner"
    const val ADMIN = "admin"
    const val STAFF = "staff"

    const val CAFE_DETAIL = "cafe/{cafeId}"
    fun cafeDetail(cafeId: String) = "cafe/$cafeId"

    const val CHECK_IN = "checkin/{cafeId}"
    fun checkIn(cafeId: String) = "checkin/$cafeId"

    const val ADD_CAFE = "cafe/new"

    const val USER_PROFILE = "user/{userId}"
    fun userProfile(userId: String) = "user/$userId"

    const val CAFE_MANAGE = "manage/{cafeId}"
    fun cafeManage(cafeId: String) = "manage/$cafeId"
}

/** A tab in the bottom bar. Which tabs exist depends on the signed-in role. */
data class TabDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * Role-driven navigation: a hunter plays the game, an owner runs a business, staff
 * only needs the QR screen, an admin moderates. Everyone keeps map and profile.
 */
fun tabsFor(role: Role): List<TabDestination> {
    val map = TabDestination(Routes.MAP, "Mapa", Icons.Filled.Map)
    val leaderboard = TabDestination(Routes.LEADERBOARD, "Lista", Icons.Filled.EmojiEvents)
    val feed = TabDestination(Routes.FEED, "Feed", Icons.Filled.DynamicFeed)
    val profile = TabDestination(Routes.PROFILE, "Profil", Icons.Filled.Person)

    return when (role) {
        Role.HUNTER -> listOf(map, leaderboard, feed, profile)
        Role.OWNER -> listOf(
            map,
            TabDestination(Routes.OWNER, "Moji kafici", Icons.Filled.Dashboard),
            feed,
            profile,
        )
        Role.STAFF -> listOf(
            map,
            TabDestination(Routes.STAFF, "QR kod", Icons.Filled.QrCode2),
            feed,
            profile,
        )
        Role.ADMIN -> listOf(
            map,
            TabDestination(Routes.ADMIN, "Moderacija", Icons.Filled.AdminPanelSettings),
            leaderboard,
            profile,
        )
    }
}
