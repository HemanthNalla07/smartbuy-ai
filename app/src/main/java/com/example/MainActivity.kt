package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firebase.configureAppCheck
import com.example.ui.SmartBuyViewModel
import com.example.ui.components.LocationPickerDialog
import com.example.ui.screens.AccountCenterScreen
import com.example.ui.screens.CompareAndCartScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.SearchResultsScreen
import com.example.ui.screens.WishlistScreen
import com.example.ui.theme.SmartBuyTheme
import java.util.Locale

private data class NavTab(
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // CRITICAL: Initialize Firebase App Check FIRST before setContent or any Firebase calls
    configureAppCheck(this, intent)
    enableEdgeToEdge()
    setContent {
      val context = LocalContext.current
      val vm: SmartBuyViewModel = viewModel(factory = SmartBuyViewModel.factory(context))
      val uiState by vm.uiState.collectAsStateWithLifecycle()

      SmartBuyTheme(themeMode = uiState.themeMode) {
        SmartBuyApp(viewModel = vm)
      }
    }
  }
}

@Composable
fun SmartBuyApp(viewModel: SmartBuyViewModel) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(uiState.statusMessage) {
    val msg = uiState.statusMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearStatusMessage()
    }
  }

  val openExternalUrl: (String) -> Unit = { url ->
    try {
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
    } catch (_: Exception) {
      // Safe fallback message
    }
  }

  // Runtime location permission launcher (triggered ONLY when user taps "Use Current Location")
  val locationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
      val granted =
        grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
          grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
      if (granted) {
        viewModel.detectCurrentLocationAddress()
      } else {
        viewModel.saveDeliveryAddress(uiState.activeAddress)
      }
    }

  // Android Voice Search launcher
  val voiceLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        val spoken =
          result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
          viewModel.executeSearch(spoken)
        }
      }
    }

  val launchVoiceSearch: () -> Unit = {
    try {
      val intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
          putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
          putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("en", "IN"))
          putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a product name to compare prices")
        }
      voiceLauncher.launch(intent)
    } catch (_: Exception) {
      // Speech recognizer unavailable on device
    }
  }

  // If Product Detail is active, render it full-screen with BackHandler support
  val detailProduct = uiState.selectedDetailProduct
  if (detailProduct != null) {
    ProductDetailScreen(
      product = detailProduct,
      state = uiState,
      activeAddress = uiState.activeAddress,
      onBack = viewModel::closeProductDetail,
      onOrderOnPlatform = openExternalUrl,
      onComparePrices = viewModel::addToCompareAndOpen,
      onToggleWishlist = viewModel::toggleWishlist,
      onSetPriceAlert = viewModel::addPriceAlert,
      onLoadUsageInfo = viewModel::loadProductUsageGuide,
    )
    return
  }

  val tabs =
    listOf(
      NavTab(R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
      NavTab(R.string.nav_search, Icons.Filled.Search, Icons.Outlined.Search, "nav_search"),
      NavTab(R.string.nav_compare, Icons.Filled.CompareArrows, Icons.Outlined.CompareArrows, "nav_compare"),
      NavTab(R.string.nav_wishlist, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, "nav_wishlist"),
      NavTab(R.string.nav_account, Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle, "nav_account"),
    )

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp

    Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(snackbarHostState) },
      bottomBar = {
        if (!useRail) {
          NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            tabs.forEachIndexed { index, tab ->
              val label = stringResource(tab.labelRes)
              NavigationBarItem(
                selected = uiState.selectedTab == index,
                onClick = { viewModel.selectTab(index) },
                icon = {
                  Icon(
                    imageVector = if (uiState.selectedTab == index) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = label,
                  )
                },
                label = { Text(label) },
                modifier = Modifier.testTag(tab.testTag),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (useRail) {
          NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
            tabs.forEachIndexed { index, tab ->
              val label = stringResource(tab.labelRes)
              NavigationRailItem(
                selected = uiState.selectedTab == index,
                onClick = { viewModel.selectTab(index) },
                icon = {
                  Icon(
                    imageVector = if (uiState.selectedTab == index) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = label,
                  )
                },
                label = { Text(label) },
                modifier = Modifier.testTag(tab.testTag),
              )
            }
          }
        }

        Box(
          modifier = Modifier.weight(1f).fillMaxSize(),
          contentAlignment = Alignment.TopCenter,
        ) {
          Box(modifier = Modifier.widthIn(max = 760.dp).fillMaxSize()) {
            when (uiState.selectedTab) {
              0 ->
                HomeScreen(
                  state = uiState,
                  onQueryChange = viewModel::updateSearchQuery,
                  onClearQuery = viewModel::clearSearchQuery,
                  onSubmitSearch = { q -> viewModel.executeSearch(q) },
                  onVoiceSearchClick = launchVoiceSearch,
                  onSelectCategory = viewModel::searchCategoryShortcut,
                  onChangeLocation = { viewModel.openLocationDialog() },
                  onOpenCart = { viewModel.selectCompareSubTab(0) },
                  onOpenAccount = { viewModel.selectTab(4) },
                  onOpenBuyhatke = { viewModel.selectCompareSubTab(2) },
                )
              1 ->
                SearchResultsScreen(
                  state = uiState,
                  onQueryChange = viewModel::updateSearchQuery,
                  onClearQuery = viewModel::clearSearchQuery,
                  onSubmitSearch = { q -> viewModel.executeSearch(q) },
                  onVoiceSearchClick = launchVoiceSearch,
                  onSortChange = viewModel::updateSearchSort,
                  onToggleInStock = viewModel::toggleInStockOnly,
                  onToggleUnderBudget = viewModel::toggleUnderBudgetOnly,
                  onOpenProductDetail = viewModel::openProductDetail,
                  onAddToCompare = viewModel::addToCompareAndOpen,
                  onToggleWishlist = viewModel::toggleWishlist,
                  onOpenExternalUrl = openExternalUrl,
                  onChangeLocation = { viewModel.openLocationDialog() },
                  onOpenCart = { viewModel.selectCompareSubTab(0) },
                  onOpenAccount = { viewModel.selectTab(4) },
                )
              2 ->
                CompareAndCartScreen(
                  state = uiState,
                  onSelectSubTab = viewModel::selectCompareSubTab,
                  onAdjustBasketQty = viewModel::updateBasketQuantity,
                  onAddBasketItem = viewModel::addBasketItem,
                  onRemoveBasketItem = viewModel::removeBasketItem,
                  onClearBasket = viewModel::clearBasket,
                  onResetBasket = viewModel::resetTestGroceryBasket,
                  onRemoveCompareProduct = viewModel::removeFromCompare,
                  onSelectSampleProduct = viewModel::selectSampleProduct,
                  onParseCustomInput = viewModel::analyzeCustomJsonOrCsv,
                  onSelectCategory = viewModel::selectCategory,
                  onAddManualObservation = viewModel::addManualObservation,
                  onImportCsv = viewModel::importBuyhatkeCsv,
                  onAttachScreenshot = viewModel::attachObservationScreenshot,
                  onBackendUrlChange = viewModel::updateBackendUrl,
                  onPingBackend = viewModel::pingBackend,
                  onRunEngineTests = viewModel::runEngineTests,
                  onOpenExternalUrl = openExternalUrl,
                  onChangeLocation = { viewModel.openLocationDialog() },
                  onOpenAccount = { viewModel.selectTab(4) },
                )
              3 ->
                WishlistScreen(
                  state = uiState,
                  onSearchProduct = { q -> viewModel.executeSearch(q) },
                  onRemoveWishlist = viewModel::removeWishlistItem,
                  onSetAlert = viewModel::addPriceAlert,
                  onOpenExternalUrl = openExternalUrl,
                  onGoToSearch = { viewModel.selectTab(0) },
                  onChangeLocation = { viewModel.openLocationDialog() },
                  onOpenCart = { viewModel.selectCompareSubTab(0) },
                  onOpenAccount = { viewModel.selectTab(4) },
                )
              else ->
                AccountCenterScreen(
                  state = uiState,
                  onOpenLocationModal = viewModel::openLocationDialog,
                  onSwitchAddress = viewModel::switchActiveAddress,
                  onDeleteAddress = viewModel::deleteAddress,
                  onUpdateProfile = viewModel::updateUserProfile,
                  onRunSearch = { q -> viewModel.executeSearch(q) },
                  onClearSearches = viewModel::clearSearchHistory,
                  onOpenWishlistTab = { viewModel.selectTab(3) },
                  onAddPriceAlert = viewModel::addPriceAlert,
                  onToggleAlert = viewModel::toggleAlertEnabled,
                  onDeleteAlert = viewModel::deleteAlert,
                  onChangeTheme = viewModel::setThemeMode,
                  onClearAppData = viewModel::clearLocalAppData,
                  onStatusMessage = viewModel::postStatusMessage,
                  onOpenCart = { viewModel.selectCompareSubTab(0) },
                )
            }
          }
        }
      }
    }
  }

  if (uiState.showLocationDialog) {
    LocationPickerDialog(
      activeAddress = uiState.activeAddress,
      editingAddress = uiState.editingAddress,
      detectedDraftAddress = uiState.detectedDraftAddress,
      savedAddresses = uiState.savedAddresses,
      isDetectingLocation = uiState.isDetectingLocation,
      locationError = uiState.locationError,
      onUseCurrentLocationClick = {
        locationPermissionLauncher.launch(
          arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
          )
        )
      },
      onSwitchAddress = viewModel::switchActiveAddress,
      onSaveAddress = viewModel::saveDeliveryAddress,
      onDismiss = viewModel::dismissLocationDialog,
    )
  }
}
