# Coffee Conquest — 0.1.0

Mobilna aplikacija za "zavrsavanje po gradu": korisnik obilazi kafice, potvrdjuje
da je popio kafu (GPS, QR kod ili na rec), skuplja poene i trka se sa ostalima na
rang listi. Back-end je **Firebase** — Authentication i Cloud Firestore;
aplikacija je Kotlin + Jetpack Compose.

## Sadrzaj

| Modul | Sta je | Kljucne tehnologije |
|---|---|---|
| `shared/` | DTO-i, enumi, pravila bodovanja i validacije | Kotlin JVM |
| `frontend/` | Android aplikacija | Compose, Material 3, Firebase SDK, osmdroid, ZXing |
| `firebase/` | Sigurnosna pravila, indeksi i seed skripta | Firestore rules, Node + firebase-admin |

`shared` drzi pravila bodovanja (`ScoreRules`), geo provere (`Geo`), validaciju
(`Validation`) i vreme (`Time`) na jednom mestu, pa aplikacija racuna "ovaj
check-in vredi 37 poena" istom aritmetikom pre i posle slanja.

## Firebase — podesavanje

Aplikacija se **nece pokrenuti** dok se ne napravi Firebase projekat. `frontend/google-services.json`
u repozitorijumu je samo obrazac sa laznim vrednostima da bi build prosao.

1. [console.firebase.google.com](https://console.firebase.google.com) → **Add project**.
2. **Build → Authentication → Sign-in method → Email/Password → Enable.**
3. **Build → Firestore Database → Create database**: **Standard** edition, baza
   `(default)`, region `europe-west3` (region se kasnije ne moze menjati), i
   zakljucana (production) pravila — prava pravila stizu u koraku 5.
4. **Project settings → Your apps → Add app → Android**, sa `rs.coffeeconquest.app`
   kao imenom paketa. Preuzeti `google-services.json` i njime **zameniti**
   `frontend/google-services.json`.
5. Postaviti pravila i indekse (Firebase CLI: `npm i -g firebase-tools`):

   ```bash
   firebase login
   cp .firebaserc.example .firebaserc     # pa upisati svoj project id
   firebase deploy --only firestore:rules,firestore:indexes
   ```

   Bez ovog koraka aplikacija radi, ali su podaci ili potpuno otvoreni ili
   potpuno zatvoreni, a upiti koji traze slozeni indeks vracaju gresku.

6. Demo nalozi i kafici:

   ```bash
   cd firebase/seed
   npm install
   # Project settings -> Service accounts -> Generate new private key,
   # snimiti kao firebase/seed/serviceAccountKey.json
   npm run seed
   ```

## Model podataka (Firestore)

Firestore nema JOIN, pa je sema namerno denormalizovana: check-in nosi ime kafica,
stavka u feedu nosi ime autora, a kafic nosi svoje brojace. Jedan ekran = jedan upit.

```
users/{uid}                    profil, rola, poeni, streak, brojaci
usernames/{usernameLower}      -> { uid, email }   jedinstvenost + prijava korisnickim imenom
users/{uid}/badges/{code}      osvojeni bedzevi
users/{uid}/conquered/{cafeId} licna "mapa osvajanja"

cafes/{cafeId}                 POI + brojaci (checkInCount, reviewCount, ratingSum)
cafes/{cafeId}/reviews/{uid}   jedna recenzija po korisniku - id dokumenta je uid autora
cafes/{cafeId}/visitors/{uid}  broj poseta po korisniku - odatle "osvajac" i cooldown
cafes/{cafeId}/qrTokens/{token} rotirajuci QR kod; id dokumenta je sam token

photos/{id}                    slike, base64 u sopstvenoj kolekciji
checkins/{id}                  svi check-inovi
challenges/{id}                bonus izazovi
follows/{followerId_followeeId} pracenja
feed/{id}                      aktivnosti
```

Dve ogledalne kolekcije (`visitors` i `conquered`) postoje da bi "ko je osvojio
ovaj kafic" i "koje sam kafice osvojio" bila po jedna jeftina pretraga umesto
agregacije po `checkins`.

### Slike bez Cloud Storage

Firebase je Cloud Storage stavio iza naplatnog naloga (Blaze), pa slike umesto
tamo idu u Firestore, kao base64 u kolekciju `photos`. Dokument u Firestore-u
staje u 1 MiB, a base64 dodaje jos trecinu, pa `PhotoSource` svaku sliku smanjuje
ispod 500 KB pre upisa — smanjuje kvalitet, pa rezoluciju, dok ne stane.

Zato slike imaju svoju kolekciju: kafic i check-in nose samo *id* slike, pa
listanje mape nikad ne povlaci i same slike. Ucitane slike se drze u `LruCache`
od 8 MB, tako da skrolovanje liste ne place ponovo isto citanje.

Cena je stvarna: slike trose citanja dokumenata umesto protoka i ne mogu biti
velike. Sa Blaze planom bi `PhotoSource` presao na Cloud Storage i ostalo bi sve isto.

## Role

Svaka rola ima svoj cilj i svoj ekran, ne samo drugaciju dozvolu:

| Rola | Sta radi |
|---|---|
| **HUNTER** | Check-in, ocene, rang lista, feed, pracenje drugih, predlaganje novih kafica |
| **OWNER** | Dashboard sa statistikom, uredjivanje kafica, bonus izazovi, odgovori na recenzije |
| **STAFF** | Jedan ekran: rotirajuci QR kod koji gost skenira kao dokaz da je zaista u kaficu |
| **ADMIN** | Odobravanje predlozenih kafica, resavanje sumnjivih check-inova, role i blokade |

**Gradski sampion** namerno *nije* rola — racuna se iz nedeljne gradske liste
(`SocialSource.cityChampion`) i daje pravo da se napravi izazov za ceo grad.
Rola se ne moze dodeliti sebi: pravila u `firebase/firestore.rules` zabranjuju
korisniku da menja sopstveno polje `role`, pa `STAFF` i `ADMIN` dodeljuje samo
administrator (ili seed skripta, koja ide preko Admin SDK-a).

## Pretraga i filteri

Mapa i tabela dele isti upit. Poziciju i radijus resava sam Firestore upit
(bounding box po `latitude`, pa tacan haversine), dok se ostali filteri primenjuju
na rezultat - tako se slobodno kombinuju, umesto da svaka kombinacija trazi svoj
slozeni indeks.

| Filter | Cime se filtrira |
|---|---|
| **Radijus** | Klizac 0,5 - 25 km; ista vrednost ide u `Geo.boundingBox` |
| **Tekst** | Naziv i adresa |
| **Tip** | `CafeType`: kafic, przionica, poslasticarnica, bar, restoran, ostalo |
| **Atributi** | Tagovi (`CafeAttributes`): wifi, terasa, za-rad, specialty... - kafic mora imati *sve* izabrane |
| **Autor** | Korisnicko ime onoga ko je predlozio kafic, ili prekidac "samo moji predlozi" |
| **Datum** | Dodato u poslednjih 7 / 30 / 365 dana |
| **Ocena** | Prosecna ocena 3+, 4+ ili 4,5+ |

Filteri se primenjuju kad se list zatvori, pa jedno podesavanje = jedan upit.
Broj aktivnih filtera stoji na dugmetu, a `Ponisti` ih brise sve odjednom.

## Lokacija (GPS i mreza)

`LocationProvider` slusa **oba** Android provajdera odjednom preko `LocationManager`:

- `GPS_PROVIDER` - tacnost od nekoliko metara, ali trazi otvoreno nebo i duze traje.
- `NETWORK_PROVIDER` - odgovara za oko sekundu i radi u zatvorenom, ali ume da
  promasi za stotine metara.

GPS fiks prekida cekanje odmah. Ako prvi stigne mrezni fiks, jos `gpsGraceMs`
(4 s) se ceka na tacniji GPS, pa se vraca sta je bolje. Ako nista ne stigne u
`timeoutMs` (10 s), koristi se poslednja poznata pozicija umesto praznog rezultata.

Aplikacija pokazuje koji je provajder odgovorio - na mapi i na ekranu za check-in -
jer mrezni fiks od ~1 km ne moze da prodje pravilo od 150 m, pa korisnik treba da
zna da mu treba GPS, a ne da misli da je aplikacija pokvarena.

## Bodovanje

Definisano u `shared/rules/ScoreRules.kt`:

```
osnovni check-in         10
prvi put u tom kaficu   +15
slika                    +5
QR potvrda              +10
streak                   +2 po danu, do 7 dana
bez dokaza (HONOR)       x0.5 i ide adminu na proveru
aktivan izazov           x1.5 – x3.0
```

Nivo N zahteva `100 * N` poena, kumulativno.

## Zastita od varanja

- **Udaljenost** — GPS check-in dalji od 150 m se odbija (`Geo.MAX_CHECKIN_DISTANCE_M`).
- **Cooldown** — isti kafic boduje najvise jednom na 6 sati.
- **Dnevni limit** — preko 10 check-inova dnevno se oznacava za proveru.
- **Nemoguca brzina** — ako bi izmedju dva check-ina trebalo putovati brze od
  130 km/h, check-in ide adminu.
- **QR kod** — rotirajuci token sa rokom od 5 minuta. Token je *id dokumenta*, a
  pravila dozvoljavaju citanje jednog dokumenta po id-u ali ne i listanje
  kolekcije, pa se kodovi ne mogu pokupiti iz baze.
- **HONOR** — prihvata se, ali vredi upola i ne donosi poene dok ga admin ne odobri.

### Sta pravila stvarno garantuju

Posto vise nema servera, bodovanje racuna uredjaj, a granica bezbednosti su
`firebase/firestore.rules`. Ona pouzdano sprecavaju: dodelu role samom sebi,
skidanje sopstvene blokade, izmenu tudjeg profila, moderaciju bez ADMIN role i
listanje QR kodova.

Ono sto **ne** sprecavaju: izmenjen klijent moze sebi upisati vise poena za
sopstveni check-in. Za to bi bile potrebne Cloud Functions (Blaze plan), gde bi
`CheckInSource.create` presao na server. Sve ostalo bi ostalo isto — zato je
racunica izdvojena u `shared/rules`.

## Pokretanje

### Preduslov

Potreban je **JDK 21** (ne samo JRE — Android build trazi `javac`) i Android SDK
sa platformom 36. Na Ubuntu:

```bash
sudo apt install openjdk-21-jdk
```

### Android aplikacija

```bash
./gradlew :frontend:installDebug     # na povezan uredjaj ili emulator
```

Nema lokalnog servera koji treba dizati — aplikacija ide direktno na Firebase.

### Testovi

```bash
./gradlew test
```

20 testova: pravila bodovanja, geo racun, validacija i model filtera u `shared`.

## Demo nalozi

Lozinka za sve: `coffee123`. Prijava ide korisnickim imenom ili emailom
(`korisnik@coffeeconquest.rs`).

| Korisnik | Rola |
|---|---|
| `admin` | ADMIN |
| `vlasnik` | OWNER — poseduje "Kafeterija Kralja Petra" |
| `konobar` | STAFF — osoblje istog kafica |
| `marko`, `jelena`, `stefan` | HUNTER |

## Sta 0.1.0 namerno *nema*

- **Cloud Functions** — bodovanje je na klijentu; vidi "Sta pravila stvarno garantuju".
- **Cloud Storage** — trazi Blaze plan; slike zato idu u Firestore, vidi gore.
- **Push notifikacije** — sve se za sada vidi u feedu.
- **Klasterovanje pinova** — mapa crta sve pinove direktno; primetice se tek na
  nekoliko stotina kafica u kadru.
- **Paginacija** — liste uzimaju prvih 30–50 rezultata. Nedeljna rang lista
  sabira najvise 500 poslednjih check-inova (`SocialSource.WEEKLY_SCAN_LIMIT`).
