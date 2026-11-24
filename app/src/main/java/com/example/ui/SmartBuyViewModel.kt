package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BackendPingResult
import com.example.data.ColabBackendClient
import com.example.data.LocationHelper
import com.example.data.PlatformSearchCatalog
import com.example.data.SampleTestDatasets
import com.example.data.local.GuestPreferencesStore
import com.example.data.local.LocalAddressEntity
import com.example.data.local.LocalAlertEntity
import com.example.data.local.LocalComparisonEntity
import com.example.data.local.LocalSearchEntity
import com.example.data.local.LocalWishlistEntity
import com.example.data.local.SmartBuyLocalDatabase
import com.example.domain.BuyhatkeCategoryAnalysis
import com.example.domain.BuyhatkeObservation
import com.example.domain.DeliveryAddress
import com.example.domain.EngineTestResult
import com.example.domain.GroceryBasketItem
import com.example.domain.MatchResult
import com.example.domain.NormalizedProduct
import com.example.domain.ParsedSearchQuery
import com.example.domain.PlatformSearchResult
import com.example.domain.PriceComparisonReport
import com.example.domain.ProductUsageInfo
import com.example.domain.RawProductInput
import com.example.domain.RecommendationResult
import com.example.domain.SearchSortOption
import com.example.domain.ShoppingCategory
import com.example.domain.SmartBuyEngine
import com.example.firebase.ComparisonRecordDoc
import com.example.firebase.FirestoreAccountRepository
import com.example.firebase.GeminiShoppingService
import com.example.firebase.PriceAlertDoc
import com.example.firebase.PriceObservationDoc
import com.example.firebase.SavedAddressDoc
import com.example.firebase.SearchRecordDoc
import com.example.firebase.UserProfileDoc
import com.example.firebase.WishlistItemDoc
import com.example.ui.theme.AppThemeMode
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Idle : UiState<Nothing>

  data object Loading : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(val message: String) : UiState<Nothing>
}

data class AccountWishlistItem(
  val id: String,
  val productName: String,
  val brand: String,
  val category: String,
  val bestPrice: Double,
  val bestPlatform: String,
  val externalUrl: String,
)

data class AccountSearchRecord(
  val id: String,
  val queryText: String,
  val parsedCategory: String,
  val pinCode: String,
)

data class AccountComparisonRecord(
  val id: String,
  val productName: String,
  val cheapestPlatform: String,
  val lowestFinalCost: Double,
  val platformsCount: Int,
)

data class AccountPriceAlert(
  val id: String,
  val productName: String,
  val targetPrice: Double,
  val currentBestPrice: Double,
  val enabled: Boolean,
  val status: String,
)

data class SmartBuyUiState(
  // Navigation & overlays
  val selectedTab: Int = 0, // 0=Home, 1=Search, 2=Compare, 3=Wishlist, 4=Account
  val compareSubTab: Int = 0, // 0=Smart Cart, 1=Compare Products, 2=Buyhatke, 3=Engine
  val selectedDetailProduct: PlatformSearchResult? = null,
  val showLocationDialog: Boolean = false,
  val editingAddress: DeliveryAddress? = null,

  // Theme & User Auth
  val themeMode: AppThemeMode = AppThemeMode.DARK,
  val isSignedIn: Boolean = false,
  val userUid: String? = null,
  val profileName: String = "Guest Shopper",
  val profileEmail: String = "",
  val profilePhone: String = "",
  val statusMessage: String? = null,

  // Location & Addresses
  val locationConfirmed: Boolean = true,
  val savedAddresses: List<DeliveryAddress> = emptyList(),
  val activeAddress: DeliveryAddress =
    DeliveryAddress(
      id = "default_blr",
      label = "Default",
      houseNo = "",
      street = "",
      area = "HSR Layout",
      city = "Bengaluru",
      state = "Karnataka",
      pinCode = "560103",
      landmark = "",
      isDefault = true,
    ),
  val isDetectingLocation: Boolean = false,
  val detectedDraftAddress: DeliveryAddress? = null,
  val locationError: String? = null,

  // Primary Product Search & Comparison
  val searchQuery: String = "",
  val isSearching: Boolean = false,
  val parsedQuery: ParsedSearchQuery? = null,
  val geminiParseNotice: String? = null,
  val rawSearchResults: List<PlatformSearchResult> = emptyList(),
  val searchResults: List<PlatformSearchResult> = emptyList(),
  val sortOption: SearchSortOption = SearchSortOption.LOWEST_FINAL_PRICE,
  val inStockOnly: Boolean = false,
  val underBudgetOnly: Boolean = false,

  // Product Detail -> Gemini "What is this product useful for?"
  val usageInfoState: UiState<ProductUsageInfo> = UiState.Idle,

  // Side-by-side Compare & Module 1-3 Analyzer
  val comparedProducts: List<PlatformSearchResult> = emptyList(),
  val sampleProducts: List<RawProductInput> = SampleTestDatasets.sampleProducts,
  val selectedProductIndex: Int = 0,
  val customProductInput: String = "",
  val normalizedProducts: List<NormalizedProduct> = emptyList(),
  val matchResults: List<MatchResult> = emptyList(),
  val priceComparison: PriceComparisonReport? = null,
  val parseError: String? = null,

  // Account Collections
  val wishlist: List<AccountWishlistItem> = emptyList(),
  val searchHistory: List<AccountSearchRecord> = emptyList(),
  val comparisonHistory: List<AccountComparisonRecord> = emptyList(),
  val priceAlerts: List<AccountPriceAlert> = emptyList(),

  // Buyhatke Price Intelligence (Module 4)
  val selectedCategory: String = "Headphones",
  val allObservations: List<BuyhatkeObservation> = SampleTestDatasets.sampleBuyhatkeObservations,
  val categoryAnalyses: List<BuyhatkeCategoryAnalysis> = emptyList(),
  val csvImportMessage: String? = null,
  val attachedScreenshotUri: Uri? = null,

  // Smart Cart / Grocery Basket (Modules 5-6)
  val basketItems: List<GroceryBasketItem> = SampleTestDatasets.testGroceryBasket,
  val basketRecommendation: RecommendationResult? = null,

  // Colab Backend & 8-Point Tests (Modules 7-8)
  val backendUrl: String = "https://your-colab-or-cloudrun-url.run.app",
  val isPingingBackend: Boolean = false,
  val pingResult: BackendPingResult? = null,
  val engineTestResults: List<EngineTestResult> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class SmartBuyViewModel(
  private val appContext: Context,
  private val localDb: SmartBuyLocalDatabase,
  private val prefsStore: GuestPreferencesStore,
  private val firestoreRepo: FirestoreAccountRepository,
) : ViewModel() {

  private val dao = localDb.dao()
  private val _uiState = MutableStateFlow(SmartBuyUiState())
  val uiState: StateFlow<SmartBuyUiState> = _uiState.asStateFlow()

  private val authUserFlow: Flow<Pair<String?, UserProfileHint>> = callbackFlow {
    val auth = Firebase.auth
    fun currentPair(): Pair<String?, UserProfileHint> {
      val u = auth.currentUser
      return u?.uid to UserProfileHint(u?.displayName.orEmpty(), u?.email.orEmpty())
    }
    trySend(currentPair())
    val listener = FirebaseAuth.AuthStateListener { trySend(currentPair()) }
    auth.addAuthStateListener(listener)
    awaitClose { auth.removeAuthStateListener(listener) }
  }

  private data class UserProfileHint(val name: String, val email: String)

  init {
    recomputeProductAnalysis(0)
    recomputeBuyhatke()
    recomputeBasket()
    runEngineTests()
    observePreferencesAndData()
  }

  private fun observePreferencesAndData() {
    viewModelScope.launch {
      combine(
        prefsStore.locationConfirmed,
        prefsStore.themeMode,
        prefsStore.guestProfile,
      ) { confirmed, themeName, guestProf ->
        Triple(confirmed, themeName, guestProf)
      }.collectLatest { (confirmed, themeName, guestProf) ->
        val mode = AppThemeMode.entries.firstOrNull { it.name == themeName } ?: AppThemeMode.DARK
        _uiState.update { state ->
          state.copy(
            locationConfirmed = confirmed,
            showLocationDialog = if (!confirmed && state.selectedDetailProduct == null) true else state.showLocationDialog,
            themeMode = mode,
            profileName = if (!state.isSignedIn) guestProf.first else state.profileName,
            profileEmail = if (!state.isSignedIn) guestProf.second else state.profileEmail,
            profilePhone = if (!state.isSignedIn) guestProf.third else state.profilePhone,
          )
        }
      }
    }

    // Observe Auth + either Firestore (signed in) or Room (guest mode)
    viewModelScope.launch {
      authUserFlow.collectLatest { (uid, hint) ->
        _uiState.update {
          it.copy(
            isSignedIn = uid != null,
            userUid = uid,
            profileName = if (uid != null && hint.name.isNotBlank()) hint.name else it.profileName,
            profileEmail = if (uid != null && hint.email.isNotBlank()) hint.email else it.profileEmail,
          )
        }
        if (uid != null) {
          firestoreRepo.upsertProfile(
            name = hint.name.ifBlank { "SmartBuy User" },
            email = hint.email,
            phone = null,
            themePreference = _uiState.value.themeMode.name,
          )
        }
      }
    }

    // Profile (Firestore when signed in)
    viewModelScope.launch {
      authUserFlow.flatMapLatest { (uid, _) ->
        if (uid == null) flowOf(null)
        else firestoreRepo.observeProfile(uid).catch { emit(null) }
      }.collectLatest { doc: UserProfileDoc? ->
        if (doc != null) {
          val mode = AppThemeMode.entries.firstOrNull { it.name == doc.themePreference }
          _uiState.update {
            it.copy(
              profileName = doc.name.ifBlank { it.profileName },
              profileEmail = doc.email.ifBlank { it.profileEmail },
              profilePhone = doc.phone.orEmpty(),
              themeMode = mode ?: it.themeMode,
            )
          }
        }
      }
    }

    // Addresses
    viewModelScope.launch {
      authUserFlow.flatMapLatest { (uid, _) ->
        if (uid == null) {
          dao.observeAddresses().flatMapLatest { list ->
            flowOf(
              list.map {
                DeliveryAddress(
                  id = it.id.toString(),
                  label = it.label,
                  houseNo = it.houseNo,
                  street = it.street,
                  area = it.area,
                  city = it.city,
                  state = it.state,
                  pinCode = it.pinCode,
                  landmark = it.landmark,
                  isDefault = it.defaultAddress,
                )
              }
            )
          }
        } else {
          firestoreRepo.observeAddresses().catch { emit(emptyList()) }.flatMapLatest { list ->
            flowOf(
              list.map {
                DeliveryAddress(
                  id = it.id,
                  label = it.label,
                  houseNo = it.houseNo,
                  street = it.street,
                  area = it.area,
                  city = it.city,
                  state = it.state,
                  pinCode = it.pinCode,
                  landmark = it.landmark.orEmpty(),
                  isDefault = it.defaultAddress,
                )
              }
            )
          }
        }
      }.collectLatest { addresses ->
        val nextActive =
          addresses.firstOrNull { it.isDefault }
            ?: addresses.firstOrNull()
            ?: _uiState.value.activeAddress
        val pinChanged = nextActive.pinCode != _uiState.value.activeAddress.pinCode
        _uiState.update { it.copy(savedAddresses = addresses, activeAddress = nextActive) }
        if (pinChanged && _uiState.value.parsedQuery != null) {
          refreshSearchResultsForActivePin()
        }
      }
    }

    // Wishlist
    viewModelScope.launch {
      authUserFlow.flatMapLatest { (uid, _) ->
        if (uid == null) {
          dao.observeWishlist().flatMapLatest { list ->
            flowOf(
              list.map {
                AccountWishlistItem(
                  id = it.id.toString(),
                  productName = it.productName,
                  brand = it.brand,
                  category = it.category,
                  bestPrice = it.bestPrice,
                  bestPlatform = it.bestPlatform,
                  externalUrl = it.externalUrl,
                )
              }
            )
          }
        } else {
          firestoreRepo.observeWishlist().catch { emit(emptyList()) }.flatMapLatest { list ->
            flowOf(
              list.map {
                AccountWishlistItem(
                  id = it.id,
                  productName = it.productName,
                  brand = it.brand.orEmpty(),
                  category = it.category,
                  bestPrice = it.bestPrice,
                  bestPlatform = it.bestPlatform,
                  externalUrl = it.externalUrl,
                )
              }
            )
          }
        }
      }.collectLatest { items -> _uiState.update { it.copy(wishlist = items) } }
    }

    // Search history
    viewModelScope.launch {
      authUserFlow.flatMapLatest { (uid, _) ->
        if (uid == null) {
          dao.observeSearches().flatMapLatest { list ->
            flowOf(list.map { AccountSearchRecord(it.id.toString(), it.queryText, it.parsedCategory, it.pinCode) })
          }
        } else {
          firestoreRepo.observeSearches().catch { emit(emptyList()) }.flatMapLatest { list ->
            flowOf(list.map { AccountSearchRecord(it.id, it.queryText, it.parsedCategory, it.pinCode) })
          }
        }
      }.collectLatest { items -> _uiState.update { it.copy(searchHistory = items) } }
    }

    // Comparison history
    viewModelScope.launch {
      authUserFlow.flatMapLatest { (uid, _) ->
        if (uid == null) {
          dao.observeComparisons().flatMapLatest { list ->
            flowOf(
              list.map {
                AccountComparisonRecord(it.id.toString(), it.productName, it.cheapestPlatform, it.lowestFinalCost, it.platformsCount)
              }
            )
          }
        } else {
          firestoreRepo.observeComparisons().catch { emit(emptyList()) }.flatMapLatest { list ->
            flowOf(
              list.map {
                AccountComparisonRecord(it.id, it.productName, it.cheapestPlatform, it.lowestFinalCost, it.platformsCount.toInt())
              }
            )
          }
        }
      }.collectLatest { items -> _uiState.update { it.copy(comparisonHistory = items) } }
    }

    // Price Alerts
    viewModelScope.launch {
      authUserFlow.flatMapLatest { (uid, _) ->
        if (uid == null) {
          dao.observeAlerts().flatMapLatest { list ->
            flowOf(
              list.map {
                AccountPriceAlert(it.id.toString(), it.productName, it.targetPrice, it.currentBestPrice, it.enabled, it.status)
              }
            )
          }
        } else {
          firestoreRepo.observeAlerts().catch { emit(emptyList()) }.flatMapLatest { list ->
            flowOf(
              list.map {
                AccountPriceAlert(it.id, it.productName, it.targetPrice, it.currentBestPrice, it.enabled, it.status)
              }
            )
          }
        }
      }.collectLatest { items -> _uiState.update { it.copy(priceAlerts = items) } }
    }

    // Cloud Buyhatke Observations merged when signed in
    viewModelScope.launch {
      authUserFlow.flatMapLatest { (uid, _) ->
        if (uid == null) flowOf(emptyList())
        else firestoreRepo.observeObservations().catch { emit(emptyList()) }
      }.collectLatest { docs: List<PriceObservationDoc> ->
        if (docs.isNotEmpty()) {
          val converted =
            docs.map {
              BuyhatkeObservation(
                category = it.category,
                productName = it.productName,
                productUrl = it.productUrl,
                platform = it.platform,
                currentPrice = it.currentPrice,
                historicalLowestPrice = it.historicalLowest,
                historicalAveragePrice = it.historicalAverage,
                historicalHighestPrice = it.historicalHighest,
                observationDate = it.observationDate,
                source = it.source,
              )
            }
          _uiState.update { state ->
            state.copy(allObservations = (SampleTestDatasets.sampleBuyhatkeObservations + converted).distinct())
          }
          recomputeBuyhatke()
        }
      }
    }
  }

  // Navigation & UI Controls
  fun selectTab(tab: Int) {
    _uiState.update { it.copy(selectedTab = tab, selectedDetailProduct = null) }
  }

  fun selectCompareSubTab(subTab: Int) {
    _uiState.update { it.copy(selectedTab = 2, compareSubTab = subTab, selectedDetailProduct = null) }
  }

  fun openProductDetail(product: PlatformSearchResult) {
    _uiState.update { it.copy(selectedDetailProduct = product, usageInfoState = UiState.Idle) }
  }

  fun closeProductDetail() {
    _uiState.update { it.copy(selectedDetailProduct = null, usageInfoState = UiState.Idle) }
  }

  fun clearStatusMessage() {
    _uiState.update { it.copy(statusMessage = null) }
  }

  fun postStatusMessage(message: String) {
    _uiState.update { it.copy(statusMessage = message) }
  }

  fun setThemeMode(mode: AppThemeMode) {
    _uiState.update { it.copy(themeMode = mode) }
    viewModelScope.launch {
      prefsStore.setThemeMode(mode.name)
      if (_uiState.value.isSignedIn) {
        firestoreRepo.upsertProfile(
          name = _uiState.value.profileName,
          email = _uiState.value.profileEmail,
          phone = _uiState.value.profilePhone.takeIf { it.isNotBlank() },
          themePreference = mode.name,
        )
      }
    }
  }

  // Location & Addresses
  fun openLocationDialog(addressToEdit: DeliveryAddress? = null) {
    _uiState.update {
      it.copy(
        showLocationDialog = true,
        editingAddress = addressToEdit,
        detectedDraftAddress = null,
        locationError = null,
      )
    }
  }

  fun dismissLocationDialog() {
    viewModelScope.launch {
      prefsStore.setLocationConfirmed(true)
    }
    _uiState.update {
      it.copy(
        showLocationDialog = false,
        editingAddress = null,
        detectedDraftAddress = null,
        locationError = null,
        locationConfirmed = true,
      )
    }
  }

  fun detectCurrentLocationAddress() {
    _uiState.update { it.copy(isDetectingLocation = true, locationError = null) }
    viewModelScope.launch {
      val result = LocationHelper.detectCurrentAddress(appContext)
      result.onSuccess { addr ->
        _uiState.update {
          it.copy(
            isDetectingLocation = false,
            detectedDraftAddress = addr,
            locationError = null,
          )
        }
      }.onFailure { err ->
        _uiState.update {
          it.copy(
            isDetectingLocation = false,
            locationError = err.localizedMessage ?: "Unable to detect location. Please enter PIN manually.",
          )
        }
      }
    }
  }

  fun saveDeliveryAddress(address: DeliveryAddress) {
    val cleanPin = address.pinCode.trim()
    if (!LocationHelper.isValidIndianPinCode(cleanPin)) {
      _uiState.update { it.copy(locationError = "Please enter a valid 6-digit Indian PIN code (e.g. 560103).") }
      return
    }
    if (address.city.trim().isEmpty()) {
      _uiState.update { it.copy(locationError = "City name is required.") }
      return
    }

    val normalized = address.copy(pinCode = cleanPin, city = address.city.trim(), isDefault = true)
    viewModelScope.launch {
      prefsStore.setLocationConfirmed(true)
      if (_uiState.value.isSignedIn) {
        // Demote previous defaults cleanly if needed, then save
        _uiState.value.savedAddresses
          .filter { it.isDefault && it.id != normalized.id && it.id.isNotBlank() }
          .forEach { prev ->
            firestoreRepo.saveAddress(
              SavedAddressDoc(
                id = prev.id,
                label = prev.label,
                houseNo = prev.houseNo,
                street = prev.street,
                area = prev.area,
                city = prev.city,
                state = prev.state,
                pinCode = prev.pinCode,
                landmark = prev.landmark.takeIf { it.isNotBlank() },
                defaultAddress = false,
              )
            )
          }
        firestoreRepo.saveAddress(
          SavedAddressDoc(
            id = if (normalized.id.startsWith("default_")) "" else normalized.id,
            label = normalized.label.ifBlank { "Home" },
            houseNo = normalized.houseNo.ifBlank { "-" },
            street = normalized.street.ifBlank { "-" },
            area = normalized.area.ifBlank { "-" },
            city = normalized.city,
            state = normalized.state.ifBlank { "India" },
            pinCode = cleanPin,
            landmark = normalized.landmark.takeIf { it.isNotBlank() },
            defaultAddress = true,
          )
        )
      } else {
        dao.clearDefaultAddress()
        val idLong = normalized.id.toLongOrNull() ?: 0L
        dao.upsertAddress(
          LocalAddressEntity(
            id = idLong,
            label = normalized.label.ifBlank { "Home" },
            houseNo = normalized.houseNo.ifBlank { "-" },
            street = normalized.street.ifBlank { "-" },
            area = normalized.area.ifBlank { "-" },
            city = normalized.city,
            state = normalized.state.ifBlank { "India" },
            pinCode = cleanPin,
            landmark = normalized.landmark,
            defaultAddress = true,
          )
        )
      }
      _uiState.update {
        it.copy(
          activeAddress = normalized,
          locationConfirmed = true,
          showLocationDialog = false,
          editingAddress = null,
          detectedDraftAddress = null,
          locationError = null,
          statusMessage = "Delivering to ${normalized.shortHeaderText}",
        )
      }
      if (_uiState.value.parsedQuery != null) {
        refreshSearchResultsForActivePin()
      }
    }
  }

  fun switchActiveAddress(address: DeliveryAddress) {
    viewModelScope.launch {
      prefsStore.setLocationConfirmed(true)
      if (_uiState.value.isSignedIn && address.id.isNotBlank()) {
        _uiState.value.savedAddresses
          .filter { it.isDefault && it.id != address.id }
          .forEach { prev ->
            firestoreRepo.saveAddress(
              SavedAddressDoc(
                id = prev.id,
                label = prev.label,
                houseNo = prev.houseNo,
                street = prev.street,
                area = prev.area,
                city = prev.city,
                state = prev.state,
                pinCode = prev.pinCode,
                landmark = prev.landmark.takeIf { it.isNotBlank() },
                defaultAddress = false,
              )
            )
          }
        firestoreRepo.saveAddress(
          SavedAddressDoc(
            id = address.id,
            label = address.label,
            houseNo = address.houseNo,
            street = address.street,
            area = address.area,
            city = address.city,
            state = address.state,
            pinCode = address.pinCode,
            landmark = address.landmark.takeIf { it.isNotBlank() },
            defaultAddress = true,
          )
        )
      } else {
        address.id.toLongOrNull()?.let { idLong ->
          dao.clearDefaultAddress()
          dao.markDefaultAddress(idLong)
        }
      }
      _uiState.update {
        it.copy(
          activeAddress = address.copy(isDefault = true),
          locationConfirmed = true,
          showLocationDialog = false,
          statusMessage = "Switched delivery PIN to ${address.shortHeaderText}",
        )
      }
      if (_uiState.value.parsedQuery != null) {
        refreshSearchResultsForActivePin()
      }
    }
  }

  fun deleteAddress(addressId: String) {
    viewModelScope.launch {
      if (_uiState.value.isSignedIn) {
        firestoreRepo.deleteDoc("addresses", addressId)
      } else {
        addressId.toLongOrNull()?.let { dao.deleteAddress(it) }
      }
    }
  }

  // Primary Product Search + Gemini Query Parsing
  fun updateSearchQuery(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
  }

  fun clearSearchQuery() {
    _uiState.update { it.copy(searchQuery = "") }
  }

  fun searchCategoryShortcut(category: ShoppingCategory) {
    val defaultQuery =
      when (category) {
        ShoppingCategory.GROCERIES -> "1 kg bananas"
        ShoppingCategory.ELECTRONICS -> "iPhone 16"
        ShoppingCategory.FOOTWEAR -> "Nike shoes size 9"
        ShoppingCategory.HOME_KITCHEN -> "1 litre stainless steel water bottle under ₹500"
        else -> category.label
      }
    executeSearch(defaultQuery, forcedCategory = category)
  }

  fun executeSearch(queryInput: String = _uiState.value.searchQuery, forcedCategory: ShoppingCategory? = null) {
    val cleanQuery = queryInput.trim()
    if (cleanQuery.isEmpty()) return

    _uiState.update {
      it.copy(
        searchQuery = cleanQuery,
        selectedTab = 1,
        selectedDetailProduct = null,
        isSearching = true,
        geminiParseNotice = null,
      )
    }

    viewModelScope.launch {
      val geminiRes = GeminiShoppingService.parseSearchQueryWithGemini(cleanQuery)
      val baseParsed =
        geminiRes.getOrElse {
          SmartBuyEngine.fallbackParseSearchQuery(cleanQuery)
        }
      val parsed =
        if (forcedCategory != null && baseParsed.category == ShoppingCategory.OTHER) {
          baseParsed.copy(category = forcedCategory)
        } else baseParsed

      val notice =
        if (geminiRes.isSuccess) "Parsed intelligently with Gemini AI"
        else "Parsed with local rule matcher (Gemini offline fallback)"

      val pin = _uiState.value.activeAddress.pinCode
      val rawResults = PlatformSearchCatalog.searchAcrossPlatforms(parsed, pin)
      val sorted =
        SmartBuyEngine.sortAndFilterSearchResults(
          results = rawResults,
          sortOption = _uiState.value.sortOption,
          inStockOnly = _uiState.value.inStockOnly,
          underBudgetOnly = _uiState.value.underBudgetOnly,
          budget = parsed.budgetInr,
        )

      _uiState.update {
        it.copy(
          isSearching = false,
          parsedQuery = parsed,
          geminiParseNotice = notice,
          rawSearchResults = rawResults,
          searchResults = sorted,
        )
      }

      // Persist search & cheapest comparison log
      if (_uiState.value.isSignedIn) {
        firestoreRepo.logSearch(cleanQuery, parsed.category.label, pin)
        val cheapest = sorted.firstOrNull { it.isVerifiedCheapestEligible }
        if (cheapest != null && cheapest.finalCost != null) {
          firestoreRepo.logComparison(
            productName = parsed.productName,
            cheapestPlatform = cheapest.platform.displayName,
            lowestCost = cheapest.finalCost,
            count = rawResults.size,
          )
        }
      } else {
        dao.insertSearch(
          LocalSearchEntity(
            queryText = cleanQuery,
            parsedCategory = parsed.category.label,
            pinCode = pin,
          )
        )
        val cheapest = sorted.firstOrNull { it.isVerifiedCheapestEligible }
        if (cheapest != null && cheapest.finalCost != null) {
          dao.insertComparison(
            LocalComparisonEntity(
              productName = parsed.productName,
              cheapestPlatform = cheapest.platform.displayName,
              lowestFinalCost = cheapest.finalCost,
              platformsCount = rawResults.size,
            )
          )
        }
      }
    }
  }

  private fun refreshSearchResultsForActivePin() {
    val parsed = _uiState.value.parsedQuery ?: return
    val pin = _uiState.value.activeAddress.pinCode
    val rawResults = PlatformSearchCatalog.searchAcrossPlatforms(parsed, pin)
    val sorted =
      SmartBuyEngine.sortAndFilterSearchResults(
        results = rawResults,
        sortOption = _uiState.value.sortOption,
        inStockOnly = _uiState.value.inStockOnly,
        underBudgetOnly = _uiState.value.underBudgetOnly,
        budget = parsed.budgetInr,
      )
    _uiState.update { it.copy(rawSearchResults = rawResults, searchResults = sorted) }
  }

  fun updateSearchSort(sort: SearchSortOption) {
    _uiState.update { state ->
      val sorted =
        SmartBuyEngine.sortAndFilterSearchResults(
          results = state.rawSearchResults,
          sortOption = sort,
          inStockOnly = state.inStockOnly,
          underBudgetOnly = state.underBudgetOnly,
          budget = state.parsedQuery?.budgetInr,
        )
      state.copy(sortOption = sort, searchResults = sorted)
    }
  }

  fun toggleInStockOnly() {
    _uiState.update { state ->
      val next = !state.inStockOnly
      val sorted =
        SmartBuyEngine.sortAndFilterSearchResults(
          results = state.rawSearchResults,
          sortOption = state.sortOption,
          inStockOnly = next,
          underBudgetOnly = state.underBudgetOnly,
          budget = state.parsedQuery?.budgetInr,
        )
      state.copy(inStockOnly = next, searchResults = sorted)
    }
  }

  fun toggleUnderBudgetOnly() {
    _uiState.update { state ->
      val next = !state.underBudgetOnly
      val sorted =
        SmartBuyEngine.sortAndFilterSearchResults(
          results = state.rawSearchResults,
          sortOption = state.sortOption,
          inStockOnly = state.inStockOnly,
          underBudgetOnly = next,
          budget = state.parsedQuery?.budgetInr,
        )
      state.copy(underBudgetOnly = next, searchResults = sorted)
    }
  }

  // Gemini "What is this product useful for?"
  fun loadProductUsageGuide(product: PlatformSearchResult) {
    _uiState.update { it.copy(usageInfoState = UiState.Loading) }
    viewModelScope.launch {
      val specs =
        listOf(product.material, product.sizeOrCapacity, product.colour)
          .filter { it.isNotBlank() }
          .joinToString(", ")
      val res =
        GeminiShoppingService.explainProductUsage(
          productName = product.productTitle,
          brand = product.brand,
          category = product.category.label,
          specs = specs,
        )
      res.onSuccess { info ->
        _uiState.update { it.copy(usageInfoState = UiState.Success(info)) }
      }.onFailure { err ->
        _uiState.update {
          it.copy(
            usageInfoState =
              UiState.Error(
                err.localizedMessage ?: "Unable to fetch Gemini product insights right now. Please retry."
              )
          )
        }
      }
    }
  }

  // Compare Products Pair
  fun addToCompareAndOpen(product: PlatformSearchResult) {
    _uiState.update { state ->
      val existing = state.comparedProducts.filterNot { it.id == product.id }
      val nextList = (listOf(product) + existing).take(4)
      state.copy(
        comparedProducts = nextList,
        selectedDetailProduct = null,
        selectedTab = 2,
        compareSubTab = 1,
        statusMessage = "Added ${product.platform.displayName} listing to Compare",
      )
    }
  }

  fun removeFromCompare(productId: String) {
    _uiState.update { state ->
      state.copy(comparedProducts = state.comparedProducts.filterNot { it.id == productId })
    }
  }

  // Wishlist, Alerts & History
  fun toggleWishlist(product: PlatformSearchResult) {
    val existing = _uiState.value.wishlist.firstOrNull { it.productName.equals(product.productTitle, ignoreCase = true) }
    viewModelScope.launch {
      if (existing != null) {
        if (_uiState.value.isSignedIn) firestoreRepo.deleteDoc("wishlist", existing.id)
        else existing.id.toLongOrNull()?.let { dao.deleteWishlist(it) }
        _uiState.update { it.copy(statusMessage = "Removed from Wishlist") }
      } else {
        val price = product.finalCost ?: product.productPrice ?: 0.0
        if (_uiState.value.isSignedIn) {
          firestoreRepo.addWishlistItem(
            WishlistItemDoc(
              productName = product.productTitle,
              brand = product.brand,
              category = product.category.label,
              bestPrice = price,
              bestPlatform = product.platform.displayName,
              externalUrl = product.externalUrl,
            )
          )
        } else {
          dao.insertWishlist(
            LocalWishlistEntity(
              productName = product.productTitle,
              brand = product.brand,
              category = product.category.label,
              bestPrice = price,
              bestPlatform = product.platform.displayName,
              externalUrl = product.externalUrl,
            )
          )
        }
        _uiState.update { it.copy(statusMessage = "Saved to Wishlist") }
      }
    }
  }

  fun removeWishlistItem(id: String) {
    viewModelScope.launch {
      if (_uiState.value.isSignedIn) firestoreRepo.deleteDoc("wishlist", id)
      else id.toLongOrNull()?.let { dao.deleteWishlist(it) }
    }
  }

  fun addPriceAlert(productName: String, targetPrice: Double, currentBestPrice: Double) {
    if (productName.isBlank() || targetPrice <= 0.0) return
    viewModelScope.launch {
      if (_uiState.value.isSignedIn) {
        firestoreRepo.addPriceAlert(productName, targetPrice, currentBestPrice)
      } else {
        val status = if (currentBestPrice in 1.0..targetPrice) "Target Met" else "Monitoring"
        dao.insertAlert(
          LocalAlertEntity(
            productName = productName,
            targetPrice = targetPrice,
            currentBestPrice = currentBestPrice,
            enabled = true,
            status = status,
          )
        )
      }
      _uiState.update { it.copy(statusMessage = "Price alert set for $productName at ₹${targetPrice.toInt()}") }
    }
  }

  fun toggleAlertEnabled(alertId: String, enabled: Boolean) {
    viewModelScope.launch {
      if (_uiState.value.isSignedIn) {
        firestoreRepo.togglePriceAlert(alertId, enabled)
      } else {
        alertId.toLongOrNull()?.let { dao.setAlertEnabled(it, enabled) }
      }
    }
  }

  fun deleteAlert(alertId: String) {
    viewModelScope.launch {
      if (_uiState.value.isSignedIn) firestoreRepo.deleteDoc("alerts", alertId)
      else alertId.toLongOrNull()?.let { dao.deleteAlert(it) }
    }
  }

  fun clearSearchHistory() {
    viewModelScope.launch {
      if (_uiState.value.isSignedIn) {
        _uiState.value.searchHistory.forEach { firestoreRepo.deleteDoc("searches", it.id) }
      } else {
        dao.clearSearches()
      }
      _uiState.update { it.copy(statusMessage = "Search history cleared") }
    }
  }

  fun updateUserProfile(name: String, phone: String) {
    viewModelScope.launch {
      val cleanName = name.trim().ifBlank { "SmartBuy Shopper" }
      val cleanPhone = phone.trim()
      if (_uiState.value.isSignedIn) {
        firestoreRepo.upsertProfile(cleanName, _uiState.value.profileEmail, cleanPhone.takeIf { it.isNotBlank() }, _uiState.value.themeMode.name)
      } else {
        prefsStore.saveGuestProfile(cleanName, _uiState.value.profileEmail, cleanPhone)
      }
      _uiState.update { it.copy(profileName = cleanName, profilePhone = cleanPhone, statusMessage = "Profile updated") }
    }
  }

  fun clearLocalAppData() {
    viewModelScope.launch {
      dao.clearSearches()
      dao.clearWishlist()
      dao.clearComparisons()
      dao.clearAlerts()
      prefsStore.clearAllPrefs()
      _uiState.update {
        it.copy(
          comparedProducts = emptyList(),
          statusMessage = "Local app history and preferences cleared",
        )
      }
    }
  }

  // Preserved Modules 1-8 Functionality
  fun selectSampleProduct(index: Int) {
    recomputeProductAnalysis(index)
  }

  private fun recomputeProductAnalysis(index: Int) {
    val samples = _uiState.value.sampleProducts
    val safeIndex = index.coerceIn(0, (samples.size - 1).coerceAtLeast(0))
    val selected = samples[safeIndex]
    val normalized = SmartBuyEngine.normalizeProduct(selected)
    val cands =
      SampleTestDatasets.catalogCandidatesFor(normalized.productName).map {
        it.first to SmartBuyEngine.normalizeProduct(it.second)
      }
    val matches = SmartBuyEngine.matchProducts(normalized, cands)
    val offers = SampleTestDatasets.sampleOffersFor(normalized.productName)
    val priceReport = SmartBuyEngine.evaluateOffers(offers)

    _uiState.update {
      it.copy(
        selectedProductIndex = safeIndex,
        normalizedProducts = listOf(normalized),
        matchResults = matches,
        priceComparison = priceReport,
        parseError = null,
      )
    }
  }

  fun analyzeCustomJsonOrCsv(rawInput: String) {
    try {
      val parsed = SmartBuyEngine.parseProductsFromJsonOrCsv(rawInput)
      if (parsed.isEmpty()) {
        _uiState.update { it.copy(parseError = "Could not parse any valid product rows from JSON/CSV.") }
        return
      }
      val primary = parsed.first()
      val cands =
        SampleTestDatasets.catalogCandidatesFor(primary.productName).map {
          it.first to SmartBuyEngine.normalizeProduct(it.second)
        }
      val matches = SmartBuyEngine.matchProducts(primary, cands)
      val priceReport = SmartBuyEngine.evaluateOffers(SampleTestDatasets.sampleOffersFor(primary.productName))
      _uiState.update {
        it.copy(
          customProductInput = rawInput,
          normalizedProducts = parsed,
          matchResults = matches,
          priceComparison = priceReport,
          parseError = null,
        )
      }
    } catch (e: Exception) {
      _uiState.update { it.copy(parseError = "Parse error: ${e.localizedMessage ?: "Invalid format"}") }
    }
  }

  fun selectCategory(category: String) {
    _uiState.update { it.copy(selectedCategory = category) }
  }

  fun attachObservationScreenshot(uri: Uri?) {
    _uiState.update { it.copy(attachedScreenshotUri = uri) }
  }

  fun addManualObservation(
    category: String,
    productName: String,
    productUrl: String,
    platform: String,
    currentPrice: Double,
    histLow: Double?,
    histAvg: Double?,
    histHigh: Double?,
    date: String,
  ) {
    val obs =
      BuyhatkeObservation(
        category = category,
        productName = productName.trim(),
        productUrl = productUrl.trim().ifEmpty { "https://buyhatke.com/" },
        platform = platform.trim().ifEmpty { "Manual Entry" },
        currentPrice = currentPrice,
        historicalLowestPrice = histLow,
        historicalAveragePrice = histAvg,
        historicalHighestPrice = histHigh,
        observationDate = date.trim().ifEmpty { "2026-11-23" },
        source = "Manual User Observation",
      )
    _uiState.update { state -> state.copy(allObservations = state.allObservations + obs, csvImportMessage = null) }
    recomputeBuyhatke()
    if (_uiState.value.isSignedIn) {
      viewModelScope.launch {
        firestoreRepo.addObservation(
          PriceObservationDoc(
            category = obs.category,
            productName = obs.productName,
            productUrl = obs.productUrl,
            platform = obs.platform,
            currentPrice = obs.currentPrice,
            historicalLowest = obs.historicalLowestPrice,
            historicalAverage = obs.historicalAveragePrice,
            historicalHighest = obs.historicalHighestPrice,
            observationDate = obs.observationDate,
            source = obs.source,
          )
        )
      }
    }
  }

  fun importBuyhatkeCsv(csvText: String) {
    val parsed = SmartBuyEngine.parseBuyhatkeCsv(csvText)
    if (parsed.isEmpty()) {
      _uiState.update {
        it.copy(csvImportMessage = "No valid Buyhatke CSV rows found. Include header: product_name,current_price,...")
      }
      return
    }
    _uiState.update { state ->
      state.copy(
        allObservations = state.allObservations + parsed,
        selectedCategory = parsed.first().category,
        csvImportMessage = "Imported ${parsed.size} observation(s) via CSV.",
      )
    }
    recomputeBuyhatke()
  }

  private fun recomputeBuyhatke() {
    val analyses = SmartBuyEngine.analyzeBuyhatkeByCategory(_uiState.value.allObservations)
    _uiState.update { it.copy(categoryAnalyses = analyses) }
  }

  fun updateBasketQuantity(itemId: String, delta: Double) {
    _uiState.update { state ->
      val updated =
        state.basketItems.map { item ->
          if (item.id == itemId) {
            val step = if (item.unit.startsWith("g", ignoreCase = true)) 250.0 else if (item.unit == "kg") 0.5 else 1.0
            val nextQty = (item.quantity + delta * step).coerceAtLeast(step)
            item.copy(quantity = nextQty)
          } else item
        }
      state.copy(basketItems = updated)
    }
    recomputeBasket()
  }

  fun addBasketItem(name: String, qty: Double, unit: String) {
    val clean = name.trim()
    if (clean.isEmpty() || qty <= 0.0) return
    _uiState.update { state ->
      val nextItem =
        GroceryBasketItem(
          id = "${clean}_${System.currentTimeMillis()}",
          itemName = clean,
          quantity = qty,
          unit = unit.trim().ifBlank { "unit" },
        )
      state.copy(basketItems = state.basketItems + nextItem)
    }
    recomputeBasket()
  }

  fun removeBasketItem(itemId: String) {
    _uiState.update { state ->
      state.copy(basketItems = state.basketItems.filterNot { it.id == itemId })
    }
    recomputeBasket()
  }

  fun clearBasket() {
    _uiState.update { it.copy(basketItems = emptyList()) }
    recomputeBasket()
  }

  fun resetTestGroceryBasket() {
    _uiState.update { it.copy(basketItems = SampleTestDatasets.testGroceryBasket) }
    recomputeBasket()
  }

  private fun recomputeBasket() {
    val rec =
      SmartBuyEngine.evaluateAndRecommendBaskets(
        _uiState.value.basketItems,
        SampleTestDatasets.sampleGroceryListings,
        SampleTestDatasets.sampleGroceryStoreMeta,
      )
    _uiState.update { it.copy(basketRecommendation = rec) }
  }

  fun updateBackendUrl(url: String) {
    _uiState.update { it.copy(backendUrl = url) }
  }

  fun pingBackend() {
    val url = _uiState.value.backendUrl
    _uiState.update { it.copy(isPingingBackend = true, pingResult = null) }
    viewModelScope.launch {
      val result = ColabBackendClient.pingHealth(url)
      _uiState.update { it.copy(isPingingBackend = false, pingResult = result) }
    }
  }

  fun runEngineTests() {
    val results = SmartBuyEngine.runAutomatedEngineTests()
    _uiState.update { it.copy(engineTestResults = results) }
  }

  companion object {
    fun factory(context: Context): ViewModelProvider.Factory {
      val appCtx = context.applicationContext
      return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          return SmartBuyViewModel(
            appContext = appCtx,
            localDb = SmartBuyLocalDatabase.getInstance(appCtx),
            prefsStore = GuestPreferencesStore(appCtx),
            firestoreRepo = FirestoreAccountRepository(appCtx),
          ) as T
        }
      }
    }
  }
}
