package rs.coffeeconquest.app.data

import android.content.Context
import rs.coffeeconquest.app.data.firebase.AdminSource
import rs.coffeeconquest.app.data.firebase.AuthSource
import rs.coffeeconquest.app.data.firebase.BadgeSource
import rs.coffeeconquest.app.data.firebase.CafeSource
import rs.coffeeconquest.app.data.firebase.ChallengeSource
import rs.coffeeconquest.app.data.firebase.CheckInSource
import rs.coffeeconquest.app.data.firebase.PhotoSource
import rs.coffeeconquest.app.data.firebase.SocialSource

/**
 * Manual dependency container. One repository, one set of Firebase data sources,
 * created once per process - a DI framework would be more machinery than this app needs.
 */
object AppContainer {

    lateinit var repository: CoffeeRepository
        private set

    lateinit var location: LocationProvider
        private set

    private var initialised = false

    fun init(context: Context) {
        if (initialised) return
        val appContext = context.applicationContext

        val social = SocialSource()
        val challenges = ChallengeSource()
        val cafes = CafeSource(challenges)
        val checkIns = CheckInSource(cafes, challenges, BadgeSource(), social)

        repository = CoffeeRepository(
            auth = AuthSource(social),
            cafeSource = cafes,
            checkInSource = checkIns,
            challengeSource = challenges,
            social = social,
            photos = PhotoSource(),
            admin = AdminSource(),
        )
        location = LocationProvider(appContext)
        initialised = true
    }
}
