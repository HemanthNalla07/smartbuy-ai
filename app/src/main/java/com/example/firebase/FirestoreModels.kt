package com.example.firebase

import android.util.Log
import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
  CREATE("create"),
  UPDATE("update"),
  DELETE("delete"),
  LIST("list"),
  GET("get"),
  WRITE("write"),
}

fun handleFirestoreError(exception: Throwable, operationType: OperationType, path: String?): String {
  val causes = generateSequence(exception) { it.cause }
  val rootError =
    causes.firstOrNull { it is FirebaseFirestoreException }
      ?: causes.firstOrNull { it !is CancellationException }
      ?: exception

  val currentUser = FirebaseAuth.getInstance().currentUser
  val providerInfoList =
    currentUser?.providerData?.map { provider ->
      JSONObject().apply {
        put("providerId", provider.providerId)
        put("email", provider.email)
      }
    } ?: emptyList()

  val authInfoJson =
    JSONObject().apply {
      put("userId", currentUser?.uid)
      put("email", currentUser?.email)
      put("emailVerified", currentUser?.isEmailVerified)
      put("tenantId", currentUser?.tenantId)
      put("providerInfo", JSONArray(providerInfoList))
    }

  val errorInfoJson =
    JSONObject().apply {
      put("error", rootError.message ?: rootError.toString())
      put("operationType", operationType.value)
      put("path", path)
      put("authInfo", authInfoJson)
    }

  val jsonString = errorInfoJson.toString()
  Log.e("FirestoreError", "Firestore Error: $jsonString")
  return jsonString
}

@Keep
@IgnoreExtraProperties
data class UserProfileDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val email: String = "",
  val phone: String? = null,
  val themePreference: String = "DARK",
  val defaultAddressId: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SavedAddressDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val label: String = "Home",
  val houseNo: String = "",
  val street: String = "",
  val area: String = "",
  val city: String = "",
  val state: String = "",
  val pinCode: String = "560103",
  val landmark: String? = null,
  val defaultAddress: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class WishlistItemDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val productName: String = "",
  val brand: String? = null,
  val category: String = "",
  val bestPrice: Double = 0.0,
  val bestPlatform: String = "",
  val externalUrl: String = "",
  val notes: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SearchRecordDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val queryText: String = "",
  val parsedCategory: String = "",
  val pinCode: String = "560103",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ComparisonRecordDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val productName: String = "",
  val cheapestPlatform: String = "",
  val lowestFinalCost: Double = 0.0,
  val platformsCount: Long = 1L,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class PriceAlertDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val productName: String = "",
  val targetPrice: Double = 0.0,
  val currentBestPrice: Double = 0.0,
  val enabled: Boolean = true,
  val status: String = "Monitoring",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class PriceObservationDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val category: String = "",
  val productName: String = "",
  val productUrl: String = "",
  val platform: String = "",
  val currentPrice: Double = 0.0,
  val historicalLowest: Double? = null,
  val historicalAverage: Double? = null,
  val historicalHighest: Double? = null,
  val observationDate: String = "",
  val source: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)
