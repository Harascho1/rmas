# Pokrivenost predavanja u aplikaciji Coffee Conquest

Mapa izmedju 14 prezentacija sa vezbi (`docs/*.pdf`) i koda u ovom repozitorijumu.
Za svaku lekciju i podlekciju stoji da li se koristi i gde tacno (`fajl:linija`).

Legenda: **DA** = koristi se | **DELIMICNO** = deo teme se koristi | **NE** = ne koristi se.

---

## 1. Zbirna tabela

| # | Lekcija | Status | Ukratko |
|---|---|---|---|
| v01 | Uvod (zahtevi projekta) | **DA** | Svi trazeni elementi postoje: registracija/prijava, lokacija, mapa, Firebase, Single Activity |
| v02 | Kotlin | **DA** | Ceo `shared/` modul plus aplikacija; koristi se i vise nego sto je predavano |
| v03 | Android Lifecycle | **DELIMICNO** | Samo `onCreate`; Compose preuzima ostatak ciklusa, nema `savedInstanceState` |
| v04 | Jetpack Compose | **DA** | Ceo UI je Compose; state, rekompozicija, navigacija, back stack |
| v05 | MVVM arhitektura | **DA** | 13 ViewModel-a, `MutableStateFlow` privatno / `StateFlow` javno, immutable state |
| v06 | Data Layer (Repository, DataStore) | **DELIMICNO** | Repository + data source + DI + korutine DA; **DataStore NE** |
| v07 | RoomDB, Retrofit | **DELIMICNO** | Room NE, Retrofit NE (Firebase SDK ih zamenjuje); permisije DA |
| v08 | Intenti i Broadcast Receiver-i | **NE** | Nema eksplicitnih/implicitnih intenta, `PendingIntent`-a ni `BroadcastReceiver`-a |
| v09 | Servisi i WorkManager | **NE** | Nema `Service` klase ni `Worker`-a |
| v10 | Notifikacije i Firebase | **DELIMICNO** | Firebase Auth + Firestore su ceo backend; **notifikacije NE**, RTDB NE, Cloud Storage NE |
| v11 | Mape | **DELIMICNO** | Mapa i markeri DA, ali preko **osmdroid (OpenStreetMap)**, ne Google Maps SDK |
| v12 | BT, BLE, NFC, Nearby API | **NE** | Nijedna tehnologija se ne koristi |
| v13 | Animacije u Jetpack Compose | **NE** | Nula poziva `animate*`, `AnimatedVisibility`, `tween`, `spring` |
| v14 | Testovi u Androidu | **DELIMICNO** | 20 unit testova u `shared/`; nema `androidTest`, nema Mockito/MockK |

---

## 2. Po lekciji: gde se sta koristi

### v01 Uvod

Prezentacija na slajdu 6 nabraja obavezne delove projekta. Svi su ispunjeni.

| Zahtev iz prezentacije | Koristi se | Gde u aplikaciji |
|---|---|---|
| Registracija i prijava korisnika | DA | `ui/auth/AuthScreen.kt`, `ui/auth/AuthViewModel.kt`, `data/firebase/AuthSource.kt:22` (register), `:81` (login) |
| Pracenje lokacije korisnika | DA | `data/LocationProvider.kt:76` (`current()`), `:129` (`lastKnown()`) |
| Interakcija delova aplikacije u odnosu na lokaciju | DA | Radijus filter i sortiranje po udaljenosti `data/firebase/CafeSource.kt:49-107`; pravilo 150 m `ui/checkin/CheckInViewModel.kt:36-43` |
| Implementacija mape i interakcija s mapom | DA | `ui/map/OsmMap.kt:59` (`AndroidView`), klik na marker `:89`, `ui/map/MapScreen.kt` |
| Serverski deo: Firebase | DA | `data/firebase/` (8 klasa), `firebase/firestore.rules`, `firebase/firestore.indexes.json` |
| Single ili Dual Activity arhitektura | DA | Jedna aktivnost: `MainActivity.kt:11`, sve ostalo su Compose destinacije |
| Odvajanje perzistencije i poziva servera od UI-ja | DA | UI nikad ne zove Firestore; ide preko `data/CoffeeRepository.kt:45` |

### v02 Kotlin

| Podlekcija | Koristi se | Gde u aplikaciji |
|---|---|---|
| `val` / `var` | DA | Svuda; `val` je pravilo, `var` samo u lokalnom stanju npr. `data/firebase/PhotoSource.kt:80` |
| Tipovi podataka, konverzije | DA | `Fire.kt:80-88` (`toInt()`, `toLong()`, `toDouble()` nad Firestore poljima) |
| String templates | DA | `data/firebase/Fire.kt:66`, poruke o greskama `shared/rules/Validation.kt:21` |
| Nizovi i kolekcije | DA | `ui/map/MapScreen.kt:88` (`arrayOf` za permisije), liste svuda |
| `if/else`, `when` | DA | `ui/Navigation.kt:57` (`when (role)`), `data/firebase/Fire.kt:107` (`when` po tipu greske) |
| Range (`1..100`, `in`) | DA | `shared/rules/Validation.kt:10-15` (`USERNAME_LENGTH = 3..24`, `RATING_RANGE = 1..5`) |
| `for` petlja | DA | `shared/rules/ScoreRules.kt:52`, `data/LocationProvider.kt:95` |
| Null safety (`?`, `?:`, `!!`) | DA | `data/firebase/Fire.kt:25-27`, `:78-98`; `!!` se namerno ne koristi |
| Klase i primarni konstruktor | DA | `data/CoffeeRepository.kt:45`, `data/LocationProvider.kt:43` |
| Default vrednosti parametara | DA | `data/CoffeeRepository.kt:80-87`, `data/LocationProvider.kt:76` |
| `init` blok / sekundarni konstruktori | DELIMICNO | `init` DA (`ui/SessionViewModel.kt:23`); sekundarni konstruktori se ne koriste |
| Class properties sa custom `get()` | DA | `shared/rules/ScoreRules.kt:111` (`fraction`), `ui/checkin/CheckInViewModel.kt:36-63` |
| Nasledjivanje (`open`, `override`) | DA | `ViewModel()` potklase, `override fun onCreate` `MainActivity.kt:13`, `LruCache` `PhotoSource.kt:30` |
| Interfejsi | DA | `ui/UiState.kt:4` (`sealed interface`), `ui/SessionViewModel.kt:63` |
| Apstraktne klase | NE | Nema `abstract class` — hijerarhije su resene `sealed interface`-om |
| Data klase | DA | Svi DTO-i u `shared/dto/`, svi UI state-ovi (npr. `ui/map/MapViewModel.kt`) |
| Enum klase | DA | `shared/model/Enums.kt` (`Role`, `CafeStatus`, `CheckInMethod`, `CheckInStatus`, `LeaderboardScope`, `FeedEventType`, `ChallengeScope`) |
| Lambda izrazi | DA | Callback-ovi kroz ceo UI (`onCafeClick: (Cafe) -> Unit`), `ui/map/OsmMap.kt:89` |

### v03 Android Lifecycle

| Podlekcija | Koristi se | Gde u aplikaciji |
|---|---|---|
| Activity kao ulazna tacka | DA | `MainActivity.kt:11` (`ComponentActivity`) |
| Deklaracija aktivnosti u manifestu | DA | `frontend/src/main/AndroidManifest.xml:22-32` |
| `onCreate` | DA | `MainActivity.kt:13` — inicijalizacija `AppContainer`-a pa `setContent` |
| `onStart` / `onResume` / `onPause` / `onStop` / `onRestart` / `onDestroy` | NE | Nijedan se ne predefinise; Compose i `ViewModel` pokrivaju te potrebe |
| Oslobadjanje resursa pri prestanku vidljivosti | DA (Compose ekvivalent) | `ui/map/OsmMap.kt:51-57` — `DisposableEffect` radi `mapView.onResume()` / `onPause()` / `onDetach()` |
| `savedInstanceState` (Bundle) | NE | Ne koristi se |
| `onSaveInstanceState` / `onRestoreInstanceState` | NE | Ne koristi se |
| Prezivljavanje rotacije | DA (drugim putem) | `ViewModel` prezivljava rekonfiguraciju (`viewModel()` `ui/CoffeeConquestApp.kt:41`), a sesija se cuva u Firebase Auth-u (`AuthSource.kt:129`) |

> Napomena za odbranu: tema **jeste** pokrivena, ali kroz Compose/`ViewModel`
> mehanizme umesto klasicnih callback-ova. Ono sto stvarno nedostaje je
> `rememberSaveable` za unos u formama (vidi sekciju 4).

### v04 Jetpack Compose

| Podlekcija | Koristi se | Gde u aplikaciji |
|---|---|---|
| Struktura aplikacije (UI Layer / Data Layer) | DA | `ui/` i `data/` paketi su strogo razdvojeni |
| `@Composable` funkcije | DA | ~13 ekrana + `ui/common/Components.kt` (deljene komponente) |
| Composable prima podatke, ne vraca nista | DA | Svi ekrani; npr. `ui/feed/FeedScreen.kt:43` |
| Rekompozicija | DA | Svaka promena `StateFlow`-a preko `collectAsStateWithLifecycle()` |
| Izbegavanje bocnih efekata u composable-ima | DA | Ucitavanje ide kroz `LaunchedEffect` (`ui/feed/FeedScreen.kt:50`), nikad direktno u telu funkcije |
| `Modifier` | DA | Svuda; npr. `ui/CoffeeConquestApp.kt:80` |
| `Surface`, `Text`, `Button`, `Column`, `Row` | DA | `ui/CoffeeConquestApp.kt:44`, `ui/staff/StaffQrScreen.kt:53-107` |
| `MaterialTheme` | DA | `ui/theme/Theme.kt:69` — custom `colorScheme` + `Typography` + tamna tema |
| `remember { mutableStateOf(...) }` | DA | `ui/admin/AdminScreen.kt:203`, `ui/common/Components.kt:223`, `ui/map/OsmMap.kt:38` |
| `by remember` delegat | DA | `ui/common/Components.kt:223`, `ui/CoffeeConquestApp.kt:42` |
| `rememberSaveable` | NE | Ne koristi se nigde |
| Navigacija: principi, back stack | DA | `ui/CoffeeConquestApp.kt:77-196` |
| `NavHost` | DA | `ui/CoffeeConquestApp.kt:77` |
| `NavController` / `rememberNavController()` | DA | `ui/CoffeeConquestApp.kt:55` |
| `NavGraph` (destinacije) | DA | 13 destinacija definisano u `ui/Navigation.kt:15-38`, registrovano `ui/CoffeeConquestApp.kt:84-185` |
| `navigate()` | DA | `ui/CoffeeConquestApp.kt:87`, `:95`, `:101` ... |
| `popBackStack()` | DA | `ui/CoffeeConquestApp.kt:137`, `:149`, `:161`, `:171`, `:182` |
| Fiksna pocetna destinacija | DA | `startDestination = Routes.MAP` (`ui/CoffeeConquestApp.kt:79`) |

### v05 MVVM arhitektura

| Podlekcija | Koristi se | Gde u aplikaciji |
|---|---|---|
| ViewModel kao posrednik model - pogled | DA | 13 ViewModel-a: `SessionViewModel`, `AuthViewModel`, `MapViewModel`, `LeaderboardViewModel`, `FeedViewModel`, `ProfileViewModel`, `CafeDetailViewModel`, `AddCafeViewModel`, `CheckInViewModel`, `OwnerViewModel`, `CafeManageViewModel`, `AdminViewModel`, `StaffQrViewModel` |
| ViewModel prezivljava unistenje aktivnosti | DA | `ui/CoffeeConquestApp.kt:41` (`viewModel()` default parametar) |
| Immutability principle | DA | Svi state-ovi su `data class` sa `val` poljima; npr. `ui/checkin/CheckInViewModel.kt:23` |
| Grupisanje state-a po funkcionalnosti | DA | Jedan `data class` po ekranu (`MapUiState`, `AdminUiState`, `CheckInUiState`...) |
| ViewModel je state-holder ekrana, ne male komponente | DA | Male komponente (`ui/common/Components.kt`) primaju samo podatke i lambde |
| Ne drzati referencu na Activity u ViewModel-u | DA | Nijedan ViewModel ne vidi `Context`; `Context` je zarobljen u `AppContainer.init` (`data/AppContainer.kt:29`) |
| Composable dobija samo deo state-a koji mu treba | DA | `ui/leaderboard/LeaderboardScreen.kt`, `ui/profile/ProfileScreen.kt` prosledjuju slice-ove i metode |
| `MutableStateFlow` privatan, `StateFlow` javan | DA | Isti obrazac u svih 13 ViewModel-a; npr. `ui/SessionViewModel.kt:20-21`, `ui/map/MapViewModel.kt:41-42` |
| `asStateFlow()` | DA | `ui/checkin/CheckInViewModel.kt:72` i svi ostali |
| `collectAsState()` u composable-u | DA (novija varijanta) | Koristi se `collectAsStateWithLifecycle()` — `ui/CoffeeConquestApp.kt:42`, `ui/admin/AdminScreen.kt:59` |
| `viewModel()` umesto `MyViewModel()` | DA | `ui/staff/StaffQrScreen.kt:39`, `ui/checkin/CheckInScreen.kt:62` itd. |
| Alternativa `mutableStateOf` + `private set` | NE | Dosledno se koristi `StateFlow` varijanta |

### v06 Data Layer — Repository pattern, korutine, DataStore

| Podlekcija | Koristi se | Gde u aplikaciji |
|---|---|---|
| Repository pattern | DA | `data/CoffeeRepository.kt:45` — jedini ulaz u podatke |
| Repository komunicira sa vise data source klasa | DA | 7 izvora ubrizgano u konstruktor `data/CoffeeRepository.kt:45-53` |
| Jedna data source klasa = jedan izvor | DA | `AuthSource`, `CafeSource`, `CheckInSource`, `ChallengeSource`, `SocialSource`, `PhotoSource`, `AdminSource`, `BadgeSource` |
| Ostali slojevi ne vide data source klase | DA | ViewModel-i vide samo `AppContainer.repository` |
| Immutability prema visim slojevima | DA | Vracaju se `data class` DTO-i iz `shared/dto/` |
| Dependency Injection | DA (rucni) | `data/AppContainer.kt:17-47` — konstruktorski DI bez framework-a |
| Korutine (`suspend`) | DA | Sve metode repozitorijuma su `suspend`; `data/CoffeeRepository.kt:64-208` |
| `launch` iz `viewModelScope` | DA | `ui/SessionViewModel.kt:29`, `ui/admin/AdminViewModel.kt:42` i svuda drugde |
| Ne lansirati korutine iz Composable-a | DA | Iskljucivo `LaunchedEffect` ili `viewModelScope` |
| `Dispatchers` / `withContext` | DA | `data/firebase/PhotoSource.kt:39`, `:63`; `ui/common/Components.kt:228` |
| `delay` | DA | `ui/staff/StaffQrViewModel.kt:84` (odbrojavanje isteka QR koda) |
| `async` / `await` (paralelne korutine) | NE | Nema `async`; svi pozivi su sekvencijalni |
| `runBlocking` | NE | Ne koristi se (i ne treba u Androidu) |
| `Flow` za pracenje promena u podacima | DELIMICNO | Koristi se samo `StateFlow` u ViewModel-ima; repozitorijum vraca obicne vrednosti, ne `Flow` |
| Obrada izuzetaka u korutinama | DA | `runCatching { }.fold { }` obrazac; mapiranje gresaka `data/firebase/Fire.kt:107` |
| **Preferences DataStore** | **NE** | Dependency `androidx-datastore-preferences` je deklarisan (`frontend/build.gradle.kts`) ali se nigde ne koristi |
| **Proto DataStore** | **NE** | Ne koristi se |

### v07 Data Layer II — Room, Retrofit, permisije

| Podlekcija | Koristi se | Gde u aplikaciji |
|---|---|---|
| Room: `@Entity`, `@Dao`, `@Database` | **NE** | Nema lokalne baze — Firestore ima svoj offline kes |
| Room relacije (tabela veze, multimap) | NE | — |
| Retrofit: interfejs servisa, `@GET`, `@POST`, `@Body` | **NE** | Umesto REST-a koristi se Firebase SDK (`data/firebase/`) |
| JSON konverter (Moshi/Gson) | NE (drugi alat) | Koristi se `kotlinx.serialization` (`@Serializable` u `shared/`) |
| Singleton za mrezni pristup | DA (ekvivalent) | `object Fire` (`data/firebase/Fire.kt:19`) drzi jedinstvene `FirebaseAuth` i `FirebaseFirestore` instance |
| Poziv servisa iz `viewModelScope` sa `try/catch` | DA | `ui/cafe/CafeDetailViewModel.kt:39`, `ui/owner/CafeManageViewModel.kt:45` (`runCatching` varijanta) |
| `INTERNET` permisija u manifestu | DA | `AndroidManifest.xml:4` |
| Ostale permisije | DA | `ACCESS_NETWORK_STATE`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `CAMERA` — `AndroidManifest.xml:5-8` |
| `uses-feature` (opciona oprema) | DA | `AndroidManifest.xml:10-12` — kamera i GPS oznaceni kao `required="false"` |
| Trazenje permisije u toku rada | DA | `ui/map/MapScreen.kt:81-90` (lokacija), `ui/checkin/CheckInScreen.kt:72-74` (kamera) |
| Aplikacija ne puca ako korisnik odbije | DA | `data/LocationProvider.kt:77`, `:130` vracaju `null`; mapa pada na `LocationProvider.DEFAULT` (`:151`) |
| Objasnjenje zasto je permisija potrebna | DA | Ekran za check-in objasnjava koji je provajder odgovorio i zasto mrezni fiks nije dovoljan (`ui/checkin/CheckInScreen.kt`) |

### v08 Intenti i Broadcast Receiver-i

| Podlekcija | Koristi se | Gde u aplikaciji |
|---|---|---|
| Eksplicitni Intent | NE | Nema `Intent(this, X::class.java)` — aplikacija ima jednu aktivnost |
| Implicitni Intent (`ACTION_VIEW`, `ACTION_SEND`) | NE | Nema deljenja, otvaranja mape u drugoj aplikaciji, poziva itd. |
| App Chooser (`createChooser`) | NE | — |
| Intent Filter u manifestu | DELIMICNO | Samo obavezni MAIN/LAUNCHER (`AndroidManifest.xml:26-29`); nema custom filtera ni deep linkova |
| Extras / `Bundle` | NE | Argumenti se prenose kroz rute navigacije (`navArgument`, `ui/CoffeeConquestApp.kt:132`) |
| `PendingIntent` | NE | — |
| `BroadcastReceiver`, `onReceive`, registracija | NE | — |
| `sendBroadcast` | NE | — |

> Indirektno: `rememberLauncherForActivityResult` (kamera, QR skener) interno
> radi preko intenta, ali ih ne pisemo mi.

### v09 Servisi i WorkManager

| Podlekcija | Koristi se | Gde u aplikaciji |
|---|---|---|
| Foreground / Background / Bound servis | NE | Nema nijedne `Service` klase |
| `onStartCommand`, `onBind`, `START_STICKY` ... | NE | — |
| Deklaracija servisa u manifestu | NE | `AndroidManifest.xml` nema `<service>` |
| `WorkManager`, `Worker`, `doWork()` | NE | Dependency `work-runtime-ktx` nije ni dodat |
| `WorkRequest`, `Constraints` | NE | — |
| Nadovezivanje task-ova | NE | — |

### v10 Notifikacije i Firebase

| Podlekcija | Koristi se | Gde u aplikaciji |
|---|---|---|
| `NotificationCompat.Builder` | **NE** | Nema notifikacija; sve se vidi u feedu (`ui/feed/FeedScreen.kt`) |
| Notification channel | NE | — |
| Tap akcija preko `PendingIntent` | NE | — |
| `NotificationManager.notify()` | NE | — |
| Kreiranje Firebase projekta, `google-services.json` | DA | `frontend/google-services.json` + uputstvo u `README.md` |
| Firebase Gradle plugin i BOM | DA | `gradle/libs.versions.toml` (`firebaseBom = "34.4.0"`), `frontend/build.gradle.kts` |
| Firebase Authentication | DA | `data/firebase/AuthSource.kt` |
| `createUserWithEmailAndPassword` | DA | `data/firebase/AuthSource.kt:42` |
| `signInWithEmailAndPassword` | DA | `data/firebase/AuthSource.kt:90` |
| UID kao kljuc korisnika u bazi | DA | `data/firebase/Fire.kt:24-32` — id dokumenta u `users` **jeste** uid |
| `Firebase.auth.currentUser` | DA | `data/firebase/Fire.kt:25` |
| `updateProfile` / promena sifre | DELIMICNO | Profil se azurira u Firestore-u (`AuthSource.kt:108`); Auth `updateProfile`/`updatePassword`/reset lozinke se ne koriste |
| Odjava (`signOut`) | DA | `data/firebase/AuthSource.kt:126` |
| Realtime Database (RTDB) | NE | Koristi se Firestore |
| `ValueEventListener` / realtime osluskivanje | NE | Nema `addSnapshotListener` — svi upiti su jednokratni `get()` |
| Firestore: reference, kolekcije, dokumenti | DA | `data/firebase/Fire.kt:31-66` (11 kolekcija i podkolekcija) |
| Firestore `get()` (citanje dokumenta) | DA | `data/firebase/Fire.kt:74-76` (`fetch()` helperi preko `await()`) |
| Firestore `set()` (upis) | DA | `data/firebase/CheckInSource.kt:309`, `AuthSource.kt:74` |
| Firestore `update()` | DA | `data/firebase/AdminSource.kt:22`, `CafeSource.kt:216` |
| Firestore `delete()` | DA | `data/firebase/ChallengeSource.kt:85` |
| Firestore upiti (`orderBy`, `where...`) | DA | `data/firebase/CafeSource.kt:49-66`, `CheckInSource.kt:339-359` |
| `whereArrayContains` | DA | `data/firebase/CafeSource.kt:243` (staff niz na kaficu) |
| Cloud Storage (upload/download slika) | **NE** | Trazi Blaze plan; slike idu kao base64 u Firestore (`data/firebase/PhotoSource.kt`) |

### v11 Mape

| Podlekcija | Koristi se | Gde u aplikaciji |
|---|---|---|
| Mapa u aplikaciji | DA | `ui/map/OsmMap.kt`, `ui/map/MapScreen.kt` |
| Google Maps SDK | **NE** | Svesno je izabran **osmdroid / OpenStreetMap** |
| Google Cloud projekat + API key | NE | Nije potreban — OSM ne trazi kljuc ni karticu |
| `MAPS_API_KEY` u `local.properties` + `meta-data` | NE | — |
| `secrets-gradle-plugin` | NE | — |
| `GoogleMap` composable, `cameraPositionState` | NE (ekvivalent) | `MapView` kroz `AndroidView` (`ui/map/OsmMap.kt:59`), centriranje `:63` |
| Markeri na mapi | DA | `ui/map/OsmMap.kt:66-95` — marker korisnika i marker po kaficu |
| Klik na marker | DA | `ui/map/OsmMap.kt:89` (`setOnMarkerClickListener`) |
| `WRITE_EXTERNAL_STORAGE` za kes tajlova | NE | osmdroid kesira preko `getSharedPreferences` (`ui/map/OsmMap.kt:40`), bez te permisije |
| Zoom i multi-touch | DA | `ui/map/OsmMap.kt:46-47` |

### v12 BT, BLE, NFC, Nearby API

| Podlekcija | Koristi se |
|---|---|
| `BluetoothAdapter`, `BluetoothDevice`, `BluetoothSocket` | NE |
| Pronalazak i uparivanje uredjaja | NE |
| Bluetooth Low Energy, GATT server | NE |
| NFC tagovi, `NdefRecord`, parsiranje | NE |
| Intent filteri za NFC | NE |
| Nearby Connections / Nearby Messages / Fast Pair | NE |

Cela lekcija je neiskoriscena. Funkcionalno mesto gde bi NFC/BLE bio prirodan —
potvrda prisustva u kaficu — resen je **rotirajucim QR kodom**
(`data/firebase/CheckInSource.kt:298` izdavanje, `:328` provera).

### v13 Animacije u Jetpack Compose

| Podlekcija | Koristi se |
|---|---|
| `AnimatedVisibility` | NE |
| `AnimatedContent` | NE |
| `animate*AsState` (`animateFloatAsState`, `animateColorAsState`...) | NE |
| `updateTransition` | NE |
| `Modifier.animateContentSize` | NE |
| `rememberInfiniteTransition` / `infiniteRepeatable` | NE |
| `tween`, `spring`, `keyframes`, `repeatable` | NE |
| Enter/exit tranzicije izmedju destinacija | NE |

Jedino "kretanje" u aplikaciji su ugradjeni Material 3 indikatori
(`CircularProgressIndicator`, `LinearProgressIndicator`), koji nisu nasa animacija.

### v14 Testovi u Androidu

| Podlekcija | Koristi se | Gde u aplikaciji |
|---|---|---|
| Automatizovani testovi | DA | 20 testova, `./gradlew test` |
| Unit testovi u `src/test/` | DA | `shared/src/test/kotlin/rs/coffeeconquest/shared/` |
| `@Test` anotacija i assert funkcije | DA | `ScoreRulesTest.kt` (6), `GeoTest.kt` (5), `CafeFilterTest.kt` (5), `ValidationTest.kt` (4) |
| Testiranje utility / matematickih funkcija | DA | `GeoTest.kt` (haversine, bounding box), `ScoreRulesTest.kt` (bodovanje, nivoi) |
| Testiranje poslovne logike (Domain Layer) | DA | Ceo `shared/rules/` je testiran |
| Testable arhitektura (slojevi, slaba spregnutost) | DA | `shared/` je cist JVM modul bez ijedne Android zavisnosti (`shared/build.gradle.kts`) |
| Testiranje ViewModel-a | **NE** | Nijedan ViewModel nema test |
| Testiranje repozitorijuma | **NE** | `CoffeeRepository` nema test |
| `androidTest` (instrumentisani testovi) | **NE** | Folder `frontend/src/androidTest/` ne postoji |
| UI testovi (Screen UI / User flow) | NE | — |
| Mockito / MockK | **NE** | Nije potrebno jer je testiran samo cist JVM kod bez zavisnosti |
| JUnit | DELIMICNO | Koristi se `kotlin.test` na JUnit Platform-i (`shared/build.gradle.kts`), ne direktno `junit:junit` |

---

## 3. Sta imamo u aplikaciji a NEMA u prezentacijama

Ovo je lista stvari koje treba istaci na odbrani — nista od navedenog nije
predavano na vezbama.

### Kotlin preko gradiva

| Sta | Gde |
|---|---|
| `sealed interface` sa generikom i varijansom (`out T`) | `ui/UiState.kt:4` |
| `data object` (Kotlin 1.9+) | `ui/UiState.kt:5`, `ui/SessionViewModel.kt:64-65` |
| `inline` + `reified` extension funkcije | `data/firebase/Fire.kt:97` (`DocumentSnapshot.enum<T>()`) |
| Extension funkcije nad tudjim tipovima | `data/firebase/Fire.kt:74-98`, `data/firebase/Mappers.kt` (ceo fajl) |
| `runCatching` + `Result.fold` umesto `try/catch` | `ui/SessionViewModel.kt:35-42`, `ui/checkin/CheckInViewModel.kt:81-84` |
| `buildList` / `buildMap` | `data/LocationProvider.kt:84`, `data/firebase/AuthSource.kt:112` |
| `use` (auto-close) | `ui/checkin/CheckInViewModel.kt:146` |
| `coerceIn` / `coerceAtMost` / `takeIf` | `shared/rules/ScoreRules.kt:77`, `data/firebase/CafeSource.kt:52` |
| `Regex` validacija | `shared/rules/Validation.kt:17-18` |
| `java.time` (`Instant`, `LocalDate`, `ZoneId`) | `shared/rules/Time.kt` |
| `object` kao singleton i kao imenovani prostor | `data/firebase/Fire.kt:19`, `shared/rules/ScoreRules.kt:12` |
| `kotlinx.serialization` (`@Serializable`) | `shared/dto/`, `shared/model/` |

### Gradle i struktura projekta

| Sta | Gde |
|---|---|
| Multi-modul projekat: cist JVM modul + Android modul | `settings.gradle.kts`, `shared/build.gradle.kts`, `frontend/build.gradle.kts` |
| Version catalog (centralizovane verzije) | `gradle/libs.versions.toml` |
| Gradle Kotlin DSL (`.gradle.kts`) umesto Groovy | ceo build |
| Deljena poslovna logika izmedju modula | `shared/` koristi i aplikacija i testovi |

### Compose i Material 3 preko gradiva

| Sta | Gde |
|---|---|
| `Scaffold` + `TopAppBar` + `NavigationBar` | `ui/CoffeeConquestApp.kt:60-75`, 21 `TopAppBar` u aplikaciji |
| `ModalBottomSheet` (filteri) | `ui/map/MapScreen.kt:215`, `:330` |
| `LazyColumn` / `LazyRow` (efikasne liste) | 18 mesta, npr. `ui/leaderboard/LeaderboardScreen.kt` |
| `SnackbarHost` za poruke | `ui/admin/AdminScreen.kt:60`, `ui/cafe/CafeDetailScreen.kt:68` |
| `FilterChip`, `DropdownMenu`, `Slider`, `Badge`, `Switch` | kroz ceo UI |
| Tamna tema (`isSystemInDarkTheme`) + custom `Typography` | `ui/theme/Theme.kt:58-78` |
| `collectAsStateWithLifecycle()` (umesto `collectAsState()`) | svih 13 ekrana |
| `LaunchedEffect` | 14 mesta, npr. `ui/feed/FeedScreen.kt:50` |
| `DisposableEffect` (ciscenje resursa) | `ui/map/OsmMap.kt:51` |
| `AndroidView` interop (View sistem u Compose-u) | `ui/map/OsmMap.kt:59` |
| `enableEdgeToEdge()` | `MainActivity.kt:16` |
| Navigacija sa argumentima (`navArgument`, `NavType`) | `ui/CoffeeConquestApp.kt:132`, `:145`, `:167`, `:178` |
| Ocuvanje state-a tabova (`popUpTo` + `saveState` + `restoreState` + `launchSingleTop`) | `ui/CoffeeConquestApp.kt:190-196` |
| Navigacija zavisna od role korisnika | `ui/Navigation.kt:51-77` |
| ActivityResult API (kamera, permisije, QR) | `ui/checkin/CheckInScreen.kt:68-78`, `ui/map/MapScreen.kt:81` |

### Firestore preko gradiva

| Sta | Gde |
|---|---|
| Batch upisi (atomicno vise dokumenata) | `data/firebase/AuthSource.kt:73`, `CheckInSource.kt:85`, `CafeSource.kt:312`, `BadgeSource.kt:44` |
| `FieldValue.increment` (atomicni brojaci) | `data/firebase/CheckInSource.kt:109-148`, `CafeSource.kt:318` |
| `FieldValue.arrayUnion` | `data/firebase/CafeSource.kt:258` |
| Podkolekcije (`reviews`, `visitors`, `conquered`, `badges`, `qrTokens`) | `data/firebase/Fire.kt:41-55` |
| Namerna denormalizacija (jedan ekran = jedan upit) | `data/firebase/Fire.kt:11-18` (objasnjeno), `Mappers.kt` |
| Ogledalne kolekcije (`visitors` / `conquered`) | `data/firebase/Fire.kt:47-50` |
| `whereIn` ogranicenje od 30 i deljenje upita u komade | `data/firebase/Fire.kt:104`, `SocialSource.kt:221`, `:353` |
| `FieldPath.documentId()` upit | `data/firebase/SocialSource.kt:354` |
| Kompozitni indeksi (deploy fajl) | `firebase/firestore.indexes.json` |
| **Sigurnosna pravila** kao pravi sigurnosni sloj | `firebase/firestore.rules` (dodela role sebi, skidanje blokade, moderacija, listanje QR kodova) |
| Firestore Admin SDK seed skripta (Node.js) | `firebase/seed/seed.js` |
| Prevodjenje Firebase gresaka u poruke na srpskom | `data/firebase/Fire.kt:107-126` |

### Lokacija preko gradiva

| Sta | Gde |
|---|---|
| Oba provajdera (`GPS_PROVIDER` + `NETWORK_PROVIDER`) istovremeno | `data/LocationProvider.kt:84-91` |
| `Channel` + `withTimeoutOrNull` za "prvi koji stigne" | `data/LocationProvider.kt:79`, `:114-119` |
| Grace period za tacniji GPS fiks | `data/LocationProvider.kt:118` |
| Fallback na poslednju poznatu poziciju | `data/LocationProvider.kt:129-140` |
| Prikaz korisniku koji je provajder odgovorio | `data/LocationProvider.kt:26-32`, `ui/checkin/CheckInScreen.kt` |
| Uredno uklanjanje listener-a u `finally` | `data/LocationProvider.kt:120-125` |

### Domenska logika i zastita od varanja

| Sta | Gde |
|---|---|
| Haversine udaljenost | `shared/rules/Geo.kt:24` |
| Bounding box za pre-filtriranje upita | `shared/rules/Geo.kt:33` |
| Pravilo 150 m za GPS check-in | `shared/rules/Geo.kt:15`, provera `data/firebase/CheckInSource.kt:236` |
| Cooldown od 6 sati po kaficu | `shared/rules/ScoreRules.kt:28`, provera `CheckInSource.kt:211` |
| Dnevni limit check-inova | `shared/rules/ScoreRules.kt:31`, provera `CheckInSource.kt:60` |
| Provera nemoguce brzine (130 km/h) | `shared/rules/Geo.kt:21`, provera `CheckInSource.kt:273` |
| Rotirajuci QR token sa rokom trajanja | `data/firebase/CheckInSource.kt:298` (izdavanje), `:328` (potrosnja) |
| Model bodovanja sa live pregledom pre slanja | `shared/rules/ScoreRules.kt:66`, pregled `ui/checkin/CheckInViewModel.kt:46-56` |
| Nivoi i napredak (kumulativni pragovi) | `shared/rules/ScoreRules.kt:36-55` |
| Streak po danima u fiksnoj vremenskoj zoni | `shared/rules/Time.kt:17-27`, `CheckInSource.kt:278` |
| Bedzevi (katalog + dodela) | `shared/model/Badges.kt`, `data/firebase/BadgeSource.kt` |
| Gradski sampion racunat iz nedeljne liste | `data/firebase/SocialSource.kt:182` |

### Ostalo

| Sta | Gde |
|---|---|
| 4 role sa razlicitim ekranima i dozvolama | `shared/model/Enums.kt:11-25`, `ui/Navigation.kt:51` |
| QR generisanje (ZXing `QRCodeWriter`) | `ui/staff/StaffQrViewModel.kt:96` |
| QR skeniranje (`ScanContract`) | `ui/checkin/CheckInScreen.kt:76` |
| Crtanje custom markera preko `Canvas`/`Paint`/`Bitmap` | `ui/map/OsmMap.kt:101-148` |
| Slike kao base64 u Firestore + `LruCache` (8 MB) | `data/firebase/PhotoSource.kt:30`, `:35` |
| Progresivno smanjivanje slike dok ne stane ispod 500 KB | `data/firebase/PhotoSource.kt:75-101` |
| Jedinstveni `UiState<T>` obrazac (Loading / Error / Ready) | `ui/UiState.kt`, prikaz `ui/common/Components.kt:95` (`StateContent`) |
| Filteri koji se kombinuju bez kompozitnog indeksa po kombinaciji | `data/firebase/CafeSource.kt:49-107`, `shared/dto/Cafe.kt:114-139` |
| Jedinstvenost korisnickog imena preko `usernames/{usernameLower}` | `data/firebase/AuthSource.kt:37`, `:75` |
| Prijava korisnickim imenom **ili** emailom | `data/firebase/AuthSource.kt:81-88` |

---

## 4. Sta ima u prezentacijama a NEMA u aplikaciji

| Tema | Lekcija | Zasto ne / sta bi trebalo |
|---|---|---|
| **DataStore (Preferences)** | v06 | Dependency je deklarisan u `frontend/build.gradle.kts` ali se **nigde ne koristi** — ovo je najlaksa rupa za popuniti (npr. zapamtiti poslednji radijus filtera ili tamnu temu) |
| **Proto DataStore** | v06 | Nema potrebe za tipiziranom lokalnom semom |
| **Room baza** | v07 | Nema lokalne baze; Firestore ima ugradjen offline kes |
| **Retrofit** | v07 | Nema REST servera — Firebase SDK je zamena |
| **Intenti (eksplicitni i implicitni)** | v08 | Single Activity + Compose navigacija; nema deljenja sadrzaja ni otvaranja drugih aplikacija |
| **PendingIntent** | v08 | Ima smisla tek uz notifikacije |
| **BroadcastReceiver** | v08 | Nema sistemskih dogadjaja koje aplikacija prati |
| **Servisi (Foreground/Background/Bound)** | v09 | Nista ne radi u pozadini |
| **WorkManager** | v09 | Nema odlozenih zadataka |
| **Notifikacije** | v10 | Namerno izostavljene u verziji 0.1.0 (`README.md`, sekcija "Sta 0.1.0 namerno nema"); sve se vidi u feedu |
| **Realtime Database** | v10 | Izabran Firestore zbog upita i skaliranja |
| **Realtime osluskivanje (`addSnapshotListener`)** | v10 | Svi upiti su jednokratni `get()`; rang lista i feed se osvezavaju rucno |
| **Cloud Storage za slike** | v10 | Trazi Blaze (placeni) plan — zaobidjeno base64 zapisom u Firestore |
| **Firebase `updatePassword` / reset lozinke** | v10 | Nije implementirana promena lozinke |
| **Google Maps SDK + API key** | v11 | Google trazi podatke o kreditnoj kartici; osmdroid radi isto bez kljuca |
| **Sve animacije** | v13 | Cela lekcija neiskoriscena — najveci pojedinacni propust ako se boduje pokrivenost gradiva |
| **Bluetooth / BLE / NFC / Nearby** | v12 | Cela lekcija neiskoriscena; QR kod resava isti problem |
| **`rememberSaveable` / `onSaveInstanceState`** | v03, v04 | Nema cuvanja unosa u formama preko unistenja procesa |
| **Lifecycle callback-ovi osim `onCreate`** | v03 | Compose i `ViewModel` ih pokrivaju, ali nisu eksplicitno demonstrirani |
| **`Flow` van `StateFlow`** | v06 | Repozitorijum vraca vrednosti, ne tokove; nema `flow {}`, `map`, `combine` nad tokovima |
| **`async` / `await`** | v06 | Svi pozivi su sekvencijalni; paralelizacija bi ubrzala npr. ucitavanje profila i statistike |
| **Instrumentisani testovi (`androidTest`)** | v14 | Folder ne postoji |
| **UI testovi (Screen / User flow)** | v14 | Nema Compose test biblioteke |
| **Testovi ViewModel-a i repozitorijuma** | v14 | Testira se samo cist `shared/` modul |
| **Mockito / MockK** | v14 | Nema sta da se mock-uje jer se Android delovi ne testiraju |
| **Apstraktne klase** | v02 | Zamenjene `sealed interface`-om |
| **Sekundarni konstruktori** | v02 | Zamenjeni default vrednostima parametara |
