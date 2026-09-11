const path = require('path');
const admin = require('firebase-admin');

const KEY_PATH = process.env.GOOGLE_APPLICATION_CREDENTIALS
  || path.join(__dirname, 'serviceAccountKey.json');

let serviceAccount;
try {
  serviceAccount = require(KEY_PATH);
} catch {
  console.error(`Nije pronadjen kljuc: ${KEY_PATH}`);
  console.error('Firebase console -> Project settings -> Service accounts -> Generate new private key.');
  process.exit(1);
}

admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });

const db = admin.firestore();
const auth = admin.auth();
const now = Date.now();

const PASSWORD = 'coffee123';

const USERS = [
  { username: 'admin',   displayName: 'Administrator',  role: 'ADMIN',  city: 'Beograd' },
  { username: 'vlasnik', displayName: 'Petar Vlasnik',  role: 'OWNER',  city: 'Beograd' },
  { username: 'konobar', displayName: 'Ana Konobar',    role: 'STAFF',  city: 'Beograd' },
  { username: 'marko',   displayName: 'Marko Markovic', role: 'HUNTER', city: 'Beograd' },
  { username: 'jelena',  displayName: 'Jelena Jovanic', role: 'HUNTER', city: 'Beograd' },
  { username: 'stefan',  displayName: 'Stefan Stefanovic', role: 'HUNTER', city: 'Novi Sad' },
  // Nis: an owner with a cafe of their own, their waiter, and two local hunters.
  { username: 'vlasniknis', displayName: 'Dragan Nikolic', role: 'OWNER',  city: 'Nis' },
  { username: 'konobarnis', displayName: 'Milica Ilic',    role: 'STAFF',  city: 'Nis' },
  { username: 'nikola',     displayName: 'Nikola Pavlovic', role: 'HUNTER', city: 'Nis' },
  { username: 'tijana',     displayName: 'Tijana Ristic',   role: 'HUNTER', city: 'Nis' },
];

const CAFES = [
  {
    name: 'Kafeterija Kralja Petra',
    type: 'KAFIC',
    address: 'Kralja Petra 71, Beograd',
    city: 'Beograd',
    latitude: 44.8199,
    longitude: 20.4547,
    tags: ['specialty', 'wifi'],
    openingHours: '08-22',
    owner: 'vlasnik',
    staff: ['konobar'],
  },
  {
    name: 'Przionica D59B',
    type: 'PRZIONICA',
    address: 'Dobracina 59b, Beograd',
    city: 'Beograd',
    latitude: 44.8221,
    longitude: 20.4610,
    tags: ['specialty', 'brunch'],
    openingHours: '08-20',
  },
  {
    name: 'Koffein Dorcol',
    type: 'KAFIC',
    address: 'Strahinjica Bana 42, Beograd',
    city: 'Beograd',
    latitude: 44.8235,
    longitude: 20.4643,
    tags: ['terasa'],
    openingHours: '07-23',
  },
  {
    name: 'Aviator Coffee',
    type: 'PRZIONICA',
    address: 'Bulevar kralja Aleksandra 79, Beograd',
    city: 'Beograd',
    latitude: 44.8055,
    longitude: 20.4815,
    tags: ['wifi', 'rad'],
    openingHours: '08-21',
  },
  {
    name: 'Bar Centrala',
    type: 'BAR',
    address: 'Zmaj Jovina 22, Novi Sad',
    city: 'Novi Sad',
    latitude: 45.2551,
    longitude: 19.8452,
    tags: ['centar'],
    openingHours: '09-23',
  },
  {
    name: 'Kafeterija Obrenoviceva',
    type: 'KAFIC',
    address: 'Obrenoviceva 20, Nis',
    city: 'Nis',
    latitude: 43.3199,
    longitude: 21.8946,
    tags: ['centar', 'wifi'],
    openingHours: '08-23',
    owner: 'vlasniknis',
    staff: ['konobarnis'],
  },
  {
    name: 'Przionica Kazandzijsko Sokace',
    type: 'PRZIONICA',
    address: 'Kopitareva 6, Nis',
    city: 'Nis',
    latitude: 43.3193,
    longitude: 21.8963,
    tags: ['specialty', 'terasa'],
    openingHours: '08-21',
  },
  {
    name: 'Tvrdjava Coffee',
    type: 'KAFIC',
    address: 'Nisavska bb, Nis',
    city: 'Nis',
    latitude: 43.3253,
    longitude: 21.8927,
    tags: ['terasa', 'park'],
    openingHours: '09-22',
  },
  {
    name: 'Bar Nisava',
    type: 'BAR',
    address: 'Bulevar Nemanjica 25, Nis',
    city: 'Nis',
    latitude: 43.3170,
    longitude: 21.9210,
    tags: ['bulevar'],
    openingHours: '09-24',
  },
];

/** Creates the Auth user if missing, then writes the profile and username index. */
async function upsertUser(spec) {
  const email = `${spec.username}@coffeeconquest.rs`;

  let record;
  try {
    record = await auth.getUserByEmail(email);
    await auth.updateUser(record.uid, { password: PASSWORD });
  } catch {
    record = await auth.createUser({
      email,
      password: PASSWORD,
      displayName: spec.displayName,
    });
  }

  await db.collection('users').doc(record.uid).set({
    username: spec.username,
    usernameLower: spec.username.toLowerCase(),
    email,
    displayName: spec.displayName,
    role: spec.role,
    city: spec.city,
    cityLower: spec.city.toLowerCase(),
    avatarPhotoId: null,
    points: 0,
    checkInCount: 0,
    reviewCount: 0,
    conqueredCafes: 0,
    currentStreakDays: 0,
    longestStreakDays: 0,
    lastCheckInDay: null,
    followerCount: 0,
    followingCount: 0,
    isBanned: false,
    banReason: null,
    createdAt: now,
  }, { merge: true });

  await db.collection('usernames').doc(spec.username.toLowerCase()).set({
    uid: record.uid,
    email,
  });

  console.log(`  ${spec.role.padEnd(6)} ${spec.username} -> ${record.uid}`);
  return record.uid;
}

async function upsertCafe(spec, uidByUsername) {
  const existing = await db.collection('cafes')
    .where('nameLower', '==', spec.name.toLowerCase())
    .limit(1)
    .get();

  const payload = {
    name: spec.name,
    nameLower: spec.name.toLowerCase(),
    description: null,
    address: spec.address,
    city: spec.city,
    cityLower: spec.city.toLowerCase(),
    latitude: spec.latitude,
    longitude: spec.longitude,
    status: 'APPROVED',
    type: spec.type,
    openingHours: spec.openingHours,
    photoId: null,
    tags: spec.tags,
    ownerId: spec.owner ? uidByUsername[spec.owner] : null,
    proposedById: uidByUsername.admin,
    staff: (spec.staff || []).map((name) => uidByUsername[name]),
    moderationNote: null,
    checkInCount: 0,
    reviewCount: 0,
    ratingSum: 0,
    createdAt: now,
  };

  const ref = existing.empty ? db.collection('cafes').doc() : existing.docs[0].ref;
  await ref.set(payload, { merge: true });
  console.log(`  ${spec.name} -> ${ref.id}`);
}

async function main() {
  console.log('Korisnici:');
  const uidByUsername = {};
  for (const spec of USERS) {
    uidByUsername[spec.username] = await upsertUser(spec);
  }

  console.log('\nKafici:');
  for (const spec of CAFES) {
    await upsertCafe(spec, uidByUsername);
  }

  console.log(`\nGotovo. Lozinka za sve naloge: ${PASSWORD}`);
  console.log('Prijava ide korisnickim imenom (npr. "marko") ili emailom.');
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
