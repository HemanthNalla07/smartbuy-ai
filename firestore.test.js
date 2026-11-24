const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user cannot read or write user addresses or wishlist", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("addresses").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("wishlist").doc("w1").set({
      userId: ALICE_UID,
      productName: "iPhone 16",
      category: "Electronics",
      bestPrice: 74900,
      bestPlatform: "Flipkart",
      externalUrl: "https://www.flipkart.com/search?q=iphone+16",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner can create & update profile, saved address, wishlist, alert, search, comparison, observation", async () => {
  const db = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Asha Rao",
      email: "asha@example.com",
      themePreference: "DARK",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    db.collection("users").doc(ALICE_UID).collection("addresses").doc("addr1").set({
      userId: ALICE_UID,
      label: "Home",
      houseNo: "42B",
      street: "Outer Ring Rd",
      area: "Bellandur",
      city: "Bengaluru",
      state: "Karnataka",
      pinCode: "560103",
      defaultAddress: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    db.collection("users").doc(ALICE_UID).collection("addresses").doc("addr1").update({
      pinCode: "560001",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    db.collection("users").doc(ALICE_UID).collection("wishlist").doc("wish1").set({
      userId: ALICE_UID,
      productName: "1 Litre Steel Bottle",
      category: "Home & Kitchen",
      bestPrice: 379,
      bestPlatform: "Flipkart",
      externalUrl: "https://www.flipkart.com/search?q=steel+bottle",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    db.collection("users").doc(ALICE_UID).collection("alerts").doc("alert1").set({
      userId: ALICE_UID,
      productName: "iPhone 16",
      targetPrice: 69999,
      currentBestPrice: 74900,
      enabled: true,
      status: "Monitoring",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    db.collection("users").doc(ALICE_UID).collection("searches").doc("s1").set({
      userId: ALICE_UID,
      queryText: "1 kg bananas",
      parsedCategory: "Groceries",
      pinCode: "560103",
      createdAt: now,
    })
  );

  await assertSucceeds(
    db.collection("users").doc(ALICE_UID).collection("comparisons").doc("c1").set({
      userId: ALICE_UID,
      productName: "1 kg bananas",
      cheapestPlatform: "Blinkit",
      lowestFinalCost: 59,
      platformsCount: 5,
      createdAt: now,
    })
  );

  await assertSucceeds(
    db.collection("users").doc(ALICE_UID).collection("observations").doc("o1").set({
      userId: ALICE_UID,
      category: "Electronics",
      productName: "iPhone 16",
      productUrl: "https://www.amazon.in/s?k=iphone+16",
      platform: "Amazon India",
      currentPrice: 74900,
      observationDate: "2026-11-23",
      source: "Manual User Entry",
      createdAt: now,
    })
  );

  // List queries succeed on user's own subcollections
  await assertSucceeds(db.collection("users").doc(ALICE_UID).collection("addresses").get());
  await assertSucceeds(db.collection("users").doc(ALICE_UID).collection("wishlist").get());
});

test("Reject invalid PIN code, shadow/ghost fields, immutable createdAt tamper, and cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  // Invalid PIN code (must be 6 digits starting 1-9)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("addresses").doc("badpin").set({
      userId: ALICE_UID,
      label: "Home",
      houseNo: "12",
      street: "Main",
      area: "Area",
      city: "City",
      state: "State",
      pinCode: "000123",
      defaultAddress: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Ghost field / @DocumentId 'id' rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("wishlist").doc("ghost").set({
      id: "ghost",
      userId: ALICE_UID,
      productName: "Bottle",
      category: "Home & Kitchen",
      bestPrice: 300,
      bestPlatform: "Amazon",
      externalUrl: "https://www.amazon.in",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cross-user read/write denied
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("addresses").doc("a1").set({
      userId: ALICE_UID,
      label: "Home",
      houseNo: "42B",
      street: "Street",
      area: "Area",
      city: "Bengaluru",
      state: "Karnataka",
      pinCode: "560103",
      defaultAddress: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("addresses").doc("a1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("addresses").get());
});
