package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.example.domain.DeliveryAddress
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object LocationHelper {
  private val indianPinRegex = Regex("^[1-9][0-9]{5}$")

  fun isValidIndianPinCode(pin: String): Boolean = indianPinRegex.matches(pin.trim())

  /**
   * Quick-commerce serviceability check based on verified Indian metro PIN prefixes.
   * For unknown/non-metro PINs, quick-commerce apps are marked unverified/unserviceable
   * instead of claiming instant delivery.
   */
  fun isQuickCommerceServiceable(pinCode: String): Boolean {
    val clean = pinCode.trim()
    if (!isValidIndianPinCode(clean)) return false
    val metroPrefixes = listOf("560", "400", "401", "110", "122", "201", "500", "600", "411", "700", "380", "302")
    return metroPrefixes.any { clean.startsWith(it) }
  }

  fun metroNameForPin(pinCode: String): String? {
    val clean = pinCode.trim()
    return when {
      clean.startsWith("560") -> "Bengaluru"
      clean.startsWith("400") || clean.startsWith("401") -> "Mumbai"
      clean.startsWith("110") || clean.startsWith("122") || clean.startsWith("201") -> "Delhi NCR"
      clean.startsWith("500") -> "Hyderabad"
      clean.startsWith("600") -> "Chennai"
      clean.startsWith("411") -> "Pune"
      clean.startsWith("700") -> "Kolkata"
      clean.startsWith("380") -> "Ahmedabad"
      clean.startsWith("302") -> "Jaipur"
      else -> null
    }
  }

  @SuppressLint("MissingPermission")
  suspend fun detectCurrentAddress(context: Context): Result<DeliveryAddress> {
    return try {
      val fusedClient = LocationServices.getFusedLocationProviderClient(context)
      val cts = CancellationTokenSource()
      val location =
        fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token).await()
          ?: fusedClient.lastLocation.await()
          ?: return Result.failure(
            IllegalStateException("Unable to detect GPS coordinates right now. Please enter your PIN code manually.")
          )

      reverseGeocode(context, location.latitude, location.longitude)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private suspend fun reverseGeocode(context: Context, lat: Double, lon: Double): Result<DeliveryAddress> =
    withContext(Dispatchers.IO) {
      try {
        val geocoder = Geocoder(context, Locale("en", "IN"))
        val addresses =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
              geocoder.getFromLocation(lat, lon, 1) { list -> cont.resumeWith(Result.success(list)) }
            }
          } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(lat, lon, 1).orEmpty()
          }

        val first =
          addresses.firstOrNull()
            ?: return@withContext Result.failure(
              IllegalStateException("Address lookup unavailable. Please confirm your PIN code manually.")
            )
        val pin = first.postalCode?.filter { it.isDigit() }?.takeIf { isValidIndianPinCode(it) } ?: "560103"
        val city = first.locality ?: first.subAdminArea ?: metroNameForPin(pin) ?: "Bengaluru"
        val state = first.adminArea ?: "Karnataka"
        val area = first.subLocality ?: first.thoroughfare ?: "Current Area"
        val street = first.thoroughfare ?: first.featureName ?: "Main Road"
        Result.success(
          DeliveryAddress(
            id = "",
            label = "Current Location",
            houseNo = first.subThoroughfare ?: "1",
            street = street,
            area = area,
            city = city,
            state = state,
            pinCode = pin,
            landmark = "",
            isDefault = true,
          )
        )
      } catch (e: Exception) {
        Result.failure(e)
      }
    }
}
