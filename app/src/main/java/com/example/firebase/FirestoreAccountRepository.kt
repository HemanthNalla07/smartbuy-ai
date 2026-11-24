package com.example.firebase

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreAccountRepository(private val db: FirebaseFirestore) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  private fun <T> observeSubcollection(
    collectionName: String,
    clazz: Class<T>,
  ): Flow<List<T>> = flow {
    val uid = requireUserId()
    val path = "users/$uid/$collectionName"
    emitAll(
      db.collection("users").document(uid).collection(collectionName)
        .snapshots()
        .map { snap ->
          snap.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val toLog = firestoreError ?: (error as? Exception)
          if (toLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(toLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun observeProfile(uid: String): Flow<UserProfileDoc?> =
    db.collection("users").document(uid)
      .snapshots()
      .map { snap ->
        if (snap.exists()) snap.toObject(UserProfileDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        else null
      }

  fun observeAddresses(): Flow<List<SavedAddressDoc>> = observeSubcollection("addresses", SavedAddressDoc::class.java)

  fun observeWishlist(): Flow<List<WishlistItemDoc>> = observeSubcollection("wishlist", WishlistItemDoc::class.java)

  fun observeSearches(): Flow<List<SearchRecordDoc>> = observeSubcollection("searches", SearchRecordDoc::class.java)

  fun observeComparisons(): Flow<List<ComparisonRecordDoc>> = observeSubcollection("comparisons", ComparisonRecordDoc::class.java)

  fun observeAlerts(): Flow<List<PriceAlertDoc>> = observeSubcollection("alerts", PriceAlertDoc::class.java)

  fun observeObservations(): Flow<List<PriceObservationDoc>> = observeSubcollection("observations", PriceObservationDoc::class.java)

  suspend fun upsertProfile(name: String, email: String, phone: String?, themePreference: String): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
    val path = "users/$uid"
    return try {
      val docRef = db.collection("users").document(uid)
      val snap = docRef.get().await()
      if (snap.exists()) {
        val updates =
          mutableMapOf<String, Any>(
            "name" to name.take(100).ifBlank { "SmartBuy User" },
            "email" to email.take(160),
            "themePreference" to themePreference,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        if (!phone.isNullOrBlank()) updates["phone"] = phone.take(20)
        docRef.update(updates).await()
      } else {
        val payload =
          mapOf(
            "userId" to uid,
            "name" to name.take(100).ifBlank { "SmartBuy User" },
            "email" to email.take(160),
            "phone" to phone?.take(20)?.takeIf { it.isNotBlank() },
            "themePreference" to themePreference,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
        docRef.set(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun saveAddress(address: SavedAddressDoc): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
    val col = db.collection("users").document(uid).collection("addresses")
    val docRef = if (address.id.isNotBlank()) col.document(address.id) else col.document()
    return try {
      if (address.id.isNotBlank()) {
        val updates =
          mutableMapOf<String, Any>(
            "label" to address.label.take(40),
            "houseNo" to address.houseNo.take(80),
            "street" to address.street.take(120),
            "area" to address.area.take(100),
            "city" to address.city.take(80),
            "state" to address.state.take(80),
            "pinCode" to address.pinCode,
            "defaultAddress" to address.defaultAddress,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        address.landmark?.take(120)?.takeIf { it.isNotBlank() }?.let { updates["landmark"] = it }
        docRef.update(updates).await()
      } else {
        val payload =
          mapOf(
            "userId" to uid,
            "label" to address.label.take(40),
            "houseNo" to address.houseNo.take(80),
            "street" to address.street.take(120),
            "area" to address.area.take(100),
            "city" to address.city.take(80),
            "state" to address.state.take(80),
            "pinCode" to address.pinCode,
            "landmark" to address.landmark?.take(120)?.takeIf { it.isNotBlank() },
            "defaultAddress" to address.defaultAddress,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ).filterValues { it != null }
        docRef.set(payload).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteDoc(subcollection: String, docId: String): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
    val docRef = db.collection("users").document(uid).collection(subcollection).document(docId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun addWishlistItem(item: WishlistItemDoc): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
    val docRef = db.collection("users").document(uid).collection("wishlist").document()
    return try {
      val payload =
        mapOf(
          "userId" to uid,
          "productName" to item.productName.take(200),
          "brand" to item.brand?.take(80)?.takeIf { it.isNotBlank() },
          "category" to item.category.take(80),
          "bestPrice" to item.bestPrice,
          "bestPlatform" to item.bestPlatform.take(80),
          "externalUrl" to item.externalUrl.take(500),
          "notes" to item.notes?.take(300)?.takeIf { it.isNotBlank() },
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun logSearch(queryText: String, parsedCategory: String, pinCode: String): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
    val docRef = db.collection("users").document(uid).collection("searches").document()
    return try {
      val payload =
        mapOf(
          "userId" to uid,
          "queryText" to queryText.take(250),
          "parsedCategory" to parsedCategory.take(80),
          "pinCode" to pinCode,
          "createdAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun logComparison(productName: String, cheapestPlatform: String, lowestCost: Double, count: Int): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
    val docRef = db.collection("users").document(uid).collection("comparisons").document()
    return try {
      val payload =
        mapOf(
          "userId" to uid,
          "productName" to productName.take(200),
          "cheapestPlatform" to cheapestPlatform.take(80),
          "lowestFinalCost" to lowestCost,
          "platformsCount" to count.coerceIn(1, 50),
          "createdAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun addPriceAlert(productName: String, targetPrice: Double, currentBestPrice: Double): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
    val docRef = db.collection("users").document(uid).collection("alerts").document()
    return try {
      val status = if (currentBestPrice in 1.0..targetPrice) "Target Met" else "Monitoring"
      val payload =
        mapOf(
          "userId" to uid,
          "productName" to productName.take(200),
          "targetPrice" to targetPrice,
          "currentBestPrice" to currentBestPrice,
          "enabled" to true,
          "status" to status,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun togglePriceAlert(alertId: String, enabled: Boolean): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
    val docRef = db.collection("users").document(uid).collection("alerts").document(alertId)
    return try {
      docRef.update(mapOf("enabled" to enabled, "updatedAt" to FieldValue.serverTimestamp())).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun addObservation(obs: PriceObservationDoc): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
    val docRef = db.collection("users").document(uid).collection("observations").document()
    return try {
      val payload =
        mapOf(
          "userId" to uid,
          "category" to obs.category.take(80),
          "productName" to obs.productName.take(200),
          "productUrl" to obs.productUrl.take(500),
          "platform" to obs.platform.take(80),
          "currentPrice" to obs.currentPrice,
          "historicalLowest" to obs.historicalLowest,
          "historicalAverage" to obs.historicalAverage,
          "historicalHighest" to obs.historicalHighest,
          "observationDate" to obs.observationDate.take(40),
          "source" to obs.source.take(120),
          "createdAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null }
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }
}
