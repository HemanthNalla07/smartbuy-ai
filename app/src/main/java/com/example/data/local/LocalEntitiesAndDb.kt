package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.smartBuyPrefs by preferencesDataStore(name = "smartbuy_prefs")

object PrefKeys {
  val LOCATION_CONFIRMED = booleanPreferencesKey("location_confirmed")
  val ACTIVE_ADDRESS_ID = stringPreferencesKey("active_address_id")
  val THEME_MODE = stringPreferencesKey("theme_mode")
  val GUEST_NAME = stringPreferencesKey("guest_name")
  val GUEST_EMAIL = stringPreferencesKey("guest_email")
  val GUEST_PHONE = stringPreferencesKey("guest_phone")
}

@Entity(tableName = "local_addresses")
data class LocalAddressEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val label: String,
  val houseNo: String,
  val street: String,
  val area: String,
  val city: String,
  val state: String,
  val pinCode: String,
  val landmark: String = "",
  val defaultAddress: Boolean = false,
)

@Entity(tableName = "local_wishlist")
data class LocalWishlistEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val productName: String,
  val brand: String = "",
  val category: String,
  val bestPrice: Double,
  val bestPlatform: String,
  val externalUrl: String,
  val createdAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "local_searches")
data class LocalSearchEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val queryText: String,
  val parsedCategory: String,
  val pinCode: String,
  val createdAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "local_comparisons")
data class LocalComparisonEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val productName: String,
  val cheapestPlatform: String,
  val lowestFinalCost: Double,
  val platformsCount: Int,
  val createdAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "local_alerts")
data class LocalAlertEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val productName: String,
  val targetPrice: Double,
  val currentBestPrice: Double,
  val enabled: Boolean = true,
  val status: String = "Monitoring",
)

@Dao
interface SmartBuyLocalDao {
  @Query("SELECT * FROM local_addresses ORDER BY defaultAddress DESC, id ASC")
  fun observeAddresses(): Flow<List<LocalAddressEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAddress(address: LocalAddressEntity): Long

  @Query("UPDATE local_addresses SET defaultAddress = 0")
  suspend fun clearDefaultAddress()

  @Query("UPDATE local_addresses SET defaultAddress = 1 WHERE id = :id")
  suspend fun markDefaultAddress(id: Long)

  @Query("DELETE FROM local_addresses WHERE id = :id")
  suspend fun deleteAddress(id: Long)

  @Query("SELECT * FROM local_wishlist ORDER BY createdAtMillis DESC")
  fun observeWishlist(): Flow<List<LocalWishlistEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertWishlist(item: LocalWishlistEntity)

  @Query("DELETE FROM local_wishlist WHERE id = :id")
  suspend fun deleteWishlist(id: Long)

  @Query("SELECT * FROM local_searches ORDER BY createdAtMillis DESC LIMIT 25")
  fun observeSearches(): Flow<List<LocalSearchEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSearch(search: LocalSearchEntity)

  @Query("DELETE FROM local_searches")
  suspend fun clearSearches()

  @Query("SELECT * FROM local_comparisons ORDER BY createdAtMillis DESC LIMIT 25")
  fun observeComparisons(): Flow<List<LocalComparisonEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertComparison(comp: LocalComparisonEntity)

  @Query("SELECT * FROM local_alerts ORDER BY id DESC")
  fun observeAlerts(): Flow<List<LocalAlertEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAlert(alert: LocalAlertEntity)

  @Query("UPDATE local_alerts SET enabled = :enabled WHERE id = :id")
  suspend fun setAlertEnabled(id: Long, enabled: Boolean)

  @Query("DELETE FROM local_alerts WHERE id = :id")
  suspend fun deleteAlert(id: Long)

  @Query("DELETE FROM local_wishlist")
  suspend fun clearWishlist()

  @Query("DELETE FROM local_comparisons")
  suspend fun clearComparisons()

  @Query("DELETE FROM local_alerts")
  suspend fun clearAlerts()
}

@Database(
  entities = [
    LocalAddressEntity::class,
    LocalWishlistEntity::class,
    LocalSearchEntity::class,
    LocalComparisonEntity::class,
    LocalAlertEntity::class,
  ],
  version = 1,
  exportSchema = false,
)
abstract class SmartBuyLocalDatabase : RoomDatabase() {
  abstract fun dao(): SmartBuyLocalDao

  companion object {
    @Volatile private var INSTANCE: SmartBuyLocalDatabase? = null

    fun getInstance(context: Context): SmartBuyLocalDatabase {
      return INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                SmartBuyLocalDatabase::class.java,
                "smartbuy_local.db",
              )
              .build()
              .also { INSTANCE = it }
        }
    }
  }
}

class GuestPreferencesStore(private val context: Context) {
  val locationConfirmed: Flow<Boolean> =
    context.smartBuyPrefs.data.map { it[PrefKeys.LOCATION_CONFIRMED] ?: false }

  val themeMode: Flow<String> =
    context.smartBuyPrefs.data.map { it[PrefKeys.THEME_MODE] ?: "DARK" }

  val guestProfile: Flow<Triple<String, String, String>> =
    context.smartBuyPrefs.data.map {
      Triple(
        it[PrefKeys.GUEST_NAME] ?: "Guest Shopper",
        it[PrefKeys.GUEST_EMAIL] ?: "",
        it[PrefKeys.GUEST_PHONE] ?: "",
      )
    }

  suspend fun setLocationConfirmed(confirmed: Boolean) {
    context.smartBuyPrefs.edit { it[PrefKeys.LOCATION_CONFIRMED] = confirmed }
  }

  suspend fun setThemeMode(mode: String) {
    context.smartBuyPrefs.edit { it[PrefKeys.THEME_MODE] = mode }
  }

  suspend fun saveGuestProfile(name: String, email: String, phone: String) {
    context.smartBuyPrefs.edit {
      it[PrefKeys.GUEST_NAME] = name
      it[PrefKeys.GUEST_EMAIL] = email
      it[PrefKeys.GUEST_PHONE] = phone
    }
  }

  suspend fun clearAllPrefs() {
    context.smartBuyPrefs.edit { it.clear() }
  }
}
