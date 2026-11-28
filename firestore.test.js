const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "firebase_uid_alice_001";
const ALICE_SUB = "108234567890123456789";
const BOB_UID = "firebase_uid_bob_002";
const BOB_SUB = "109876543210987654321";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

function validUserPayload(uid, sub, email, displayName) {
  const ts = pastDate(10000);
  return {
    userId: uid,
    googleSub: sub,
    displayName,
    email,
    photoUrl: "https://lh3.googleusercontent.com/a/example-avatar",
    provider: "google.com",
    createdAt: ts,
    updatedAt: ts,
    lastSignInAt: ts,
  };
}

function validMessagePayload(uid, sub, overrides = {}) {
  const ts = pastDate(5000);
  return {
    userId: uid,
    googleSub: sub,
    role: "USER",
    text: "Recommend 4K HDR sci-fi movies",
    modelName: "gemini-4-hydrogen",
    modeLabel: "Search AI",
    usedGoogleSearch: true,
    searchQueries: ["best 4K HDR sci-fi movies"],
    sources: [
      {
        title: "Cinema Guide",
        uri: "https://example.com/sci-fi",
        domain: "example.com",
      },
    ],
    timestampMs: Date.now() - 5000,
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied all reads and writes on users and ai_messages", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .set(validUserPayload(ALICE_UID, ALICE_SUB, "alice@gmail.com", "Alice"))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("ai_messages").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("ai_messages")
      .doc("msg-001")
      .set(validMessagePayload(ALICE_UID, ALICE_SUB))
  );
});

test("Authenticated owner: can create and partially update own Google profile without duplicating or mutating immutable fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userRef = aliceDb.collection("users").doc(ALICE_UID);

  // Create profile (no photoUrl optional field omitted)
  const createWithoutPhoto = validUserPayload(
    ALICE_UID,
    ALICE_SUB,
    "alice@gmail.com",
    "Alice Rao"
  );
  delete createWithoutPhoto.photoUrl;
  await assertSucceeds(userRef.set(createWithoutPhoto));
  await assertSucceeds(userRef.get());

  // Partial update on subsequent sign-in preserves createdAt and googleSub
  await assertSucceeds(
    userRef.update({
      displayName: "Alice R.",
      email: "alice@gmail.com",
      photoUrl: "https://lh3.googleusercontent.com/a/new-avatar",
      updatedAt: pastDate(1000),
      lastSignInAt: pastDate(1000),
    })
  );

  // Reject mutating immutable googleSub
  await assertFails(
    userRef.update({
      googleSub: BOB_SUB,
      updatedAt: pastDate(500),
      lastSignInAt: pastDate(500),
    })
  );

  // Reject mutating immutable createdAt
  await assertFails(
    userRef.update({
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
      lastSignInAt: pastDate(500),
    })
  );

  // Reject shadow/ghost field
  await assertFails(
    userRef.update({
      isAdmin: true,
      updatedAt: pastDate(500),
      lastSignInAt: pastDate(500),
    })
  );

  // Reject listing /users collection
  await assertFails(aliceDb.collection("users").get());
});

test("Multi-account PII isolation: User A cannot read, write, update, or delete User B profile or AI chat history", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb
      .collection("users")
      .doc(BOB_UID)
      .set(validUserPayload(BOB_UID, BOB_SUB, "bob@gmail.com", "Bob"));
    await adminDb
      .collection("users")
      .doc(BOB_UID)
      .collection("ai_messages")
      .doc("bob-msg-1")
      .set(validMessagePayload(BOB_UID, BOB_SUB, { text: "Bob private chat" }));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).update({
      displayName: "Hijacked",
      updatedAt: pastDate(1000),
      lastSignInAt: pastDate(1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("ai_messages").get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("ai_messages")
      .doc("bob-msg-1")
      .get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("ai_messages")
      .doc("bob-msg-1")
      .delete()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("ai_messages")
      .doc("injected-msg")
      .set(validMessagePayload(ALICE_UID, ALICE_SUB))
  );
});

test("Authenticated owner: Kotlin-aligned ai_messages create, list/orderBy, update, delete, and validation bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const messagesCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("ai_messages");

  // Empty subcollection list query succeeds (mirrors Kotlin observeUserMessages)
  await assertSucceeds(messagesCol.orderBy("timestampMs", "asc").get());

  // Valid create (no @DocumentId `id` field in map)
  await assertSucceeds(
    messagesCol.doc("msg-uuid-1").set(validMessagePayload(ALICE_UID, ALICE_SUB))
  );

  // Ordered list query succeeds
  await assertSucceeds(messagesCol.orderBy("timestampMs", "asc").get());

  // Valid partial update
  await assertSucceeds(
    messagesCol.doc("msg-uuid-1").update({
      text: "Updated response text",
      updatedAt: pastDate(500),
    })
  );

  // Reject @DocumentId `id` key in write payload
  await assertFails(
    messagesCol
      .doc("msg-uuid-2")
      .set({ id: "msg-uuid-2", ...validMessagePayload(ALICE_UID, ALICE_SUB) })
  );

  // Reject future tampered timestamp
  await assertFails(
    messagesCol.doc("msg-uuid-3").set(
      validMessagePayload(ALICE_UID, ALICE_SUB, {
        createdAt: new Date(Date.now() + 60000),
      })
    )
  );

  // Reject spoofed userId inside own subcollection
  await assertFails(
    messagesCol
      .doc("msg-uuid-4")
      .set(validMessagePayload(BOB_UID, ALICE_SUB))
  );

  // Reject invalid role enum
  await assertFails(
    messagesCol
      .doc("msg-uuid-5")
      .set(validMessagePayload(ALICE_UID, ALICE_SUB, { role: "SYSTEM" }))
  );

  // Valid delete by owner
  await assertSucceeds(messagesCol.doc("msg-uuid-1").delete());
});
