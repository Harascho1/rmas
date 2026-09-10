package rs.coffeeconquest.shared.rules

/**
 * Input rules: the app greys out the submit button with them, and checks them
 * again before writing. What a client must not be trusted with at all is
 * enforced in firebase/firestore.rules instead.
 */
object Validation {

    val USERNAME_LENGTH = 3..24
    val PASSWORD_MIN_LENGTH = 8
    val DISPLAY_NAME_LENGTH = 2..40
    val CAFE_NAME_LENGTH = 2..80
    val REVIEW_COMMENT_MAX = 500
    val RATING_RANGE = 1..5

    private val usernameRegex = Regex("^[a-zA-Z0-9._-]+$")
    private val emailRegex = Regex("^[^@\\s]+@[^@\\s.]+\\.[^@\\s]+$")

    fun username(value: String): String? = when {
        value.length !in USERNAME_LENGTH -> "Korisnicko ime mora imati ${USERNAME_LENGTH.first}-${USERNAME_LENGTH.last} karaktera."
        !usernameRegex.matches(value) -> "Dozvoljena su slova, brojevi, tacka, crtica i donja crta."
        else -> null
    }

    fun email(value: String): String? =
        if (emailRegex.matches(value)) null else "Neispravna email adresa."

    fun password(value: String): String? =
        if (value.length >= PASSWORD_MIN_LENGTH) null else "Lozinka mora imati bar $PASSWORD_MIN_LENGTH karaktera."

    fun displayName(value: String): String? =
        if (value.trim().length in DISPLAY_NAME_LENGTH) null else "Ime mora imati ${DISPLAY_NAME_LENGTH.first}-${DISPLAY_NAME_LENGTH.last} karaktera."

    fun cafeName(value: String): String? =
        if (value.trim().length in CAFE_NAME_LENGTH) null else "Naziv kafica mora imati ${CAFE_NAME_LENGTH.first}-${CAFE_NAME_LENGTH.last} karaktera."

    fun rating(value: Int): String? =
        if (value in RATING_RANGE) null else "Ocena mora biti od ${RATING_RANGE.first} do ${RATING_RANGE.last}."

    fun reviewComment(value: String?): String? = when {
        value == null -> null
        value.length > REVIEW_COMMENT_MAX -> "Komentar sme imati najvise $REVIEW_COMMENT_MAX karaktera."
        else -> null
    }
}
