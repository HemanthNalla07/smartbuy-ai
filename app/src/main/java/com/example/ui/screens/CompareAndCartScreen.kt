package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.SampleTestDatasets
import com.example.domain.MatchType
import com.example.domain.PlatformSearchResult
import com.example.ui.SmartBuyUiState
import com.example.ui.components.BasketComparisonCanvasChart
import com.example.ui.components.PriceHistoryCanvasChart
import com.example.ui.components.SmartBuyTopHeader
import com.example.ui.theme.LavenderAccent
import com.example.ui.theme.LimeNeon
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.TealCyan

@Composable
fun CompareAndCartScreen(
  state: SmartBuyUiState,
  onSelectSubTab: (Int) -> Unit,
  onAdjustBasketQty: (String, Double) -> Unit,
  onAddBasketItem: (String, Double, String) -> Unit,
  onRemoveBasketItem: (String) -> Unit,
  onClearBasket: () -> Unit,
  onResetBasket: () -> Unit,
  onRemoveCompareProduct: (String) -> Unit,
  onSelectSampleProduct: (Int) -> Unit,
  onParseCustomInput: (String) -> Unit,
  onSelectCategory: (String) -> Unit,
  onAddManualObservation: (String, String, String, String, Double, Double?, Double?, Double?, String) -> Unit,
  onImportCsv: (String) -> Unit,
  onAttachScreenshot: (Uri?) -> Unit,
  onBackendUrlChange: (String) -> Unit,
  onPingBackend: () -> Unit,
  onRunEngineTests: () -> Unit,
  onOpenExternalUrl: (String) -> Unit,
  onChangeLocation: () -> Unit,
  onOpenAccount: () -> Unit,
) {
  val subTabs = listOf("Smart Cart", "Compare", "Buyhatke", "Engine")

  Column(modifier = Modifier.fillMaxSize()) {
    SmartBuyTopHeader(
      activeAddress = state.activeAddress,
      cartCount = state.basketItems.size,
      isSignedIn = state.isSignedIn,
      onChangeLocation = onChangeLocation,
      onOpenCart = { onSelectSubTab(0) },
      onOpenAccount = onOpenAccount,
    )

    ScrollableTabRow(
      selectedTabIndex = state.compareSubTab.coerceIn(0, subTabs.lastIndex),
      containerColor = MaterialTheme.colorScheme.surface,
      edgePadding = 12.dp,
    ) {
      subTabs.forEachIndexed { index, title ->
        Tab(
          selected = state.compareSubTab == index,
          onClick = { onSelectSubTab(index) },
          text = { Text(title, fontWeight = FontWeight.SemiBold) },
          modifier = Modifier.testTag("compare_subtab_$index"),
        )
      }
    }

    when (state.compareSubTab) {
      0 ->
        SmartCartSubScreen(
          state = state,
          onAdjustQty = onAdjustBasketQty,
          onAddItem = onAddBasketItem,
          onRemoveItem = onRemoveBasketItem,
          onClearBasket = onClearBasket,
          onResetBasket = onResetBasket,
        )
      1 ->
        SideBySideCompareSubScreen(
          state = state,
          onRemoveCompare = onRemoveCompareProduct,
          onSelectSample = onSelectSampleProduct,
          onParseCustom = onParseCustomInput,
          onOpenExternalUrl = onOpenExternalUrl,
        )
      2 ->
        BuyhatkeSubScreen(
          state = state,
          onSelectCategory = onSelectCategory,
          onAddManualObservation = onAddManualObservation,
          onImportCsv = onImportCsv,
          onAttachScreenshot = onAttachScreenshot,
        )
      else ->
        EngineSubScreen(
          state = state,
          onBackendUrlChange = onBackendUrlChange,
          onPingBackend = onPingBackend,
          onRunTests = onRunEngineTests,
        )
    }
  }
}

@Composable
private fun SmartCartSubScreen(
  state: SmartBuyUiState,
  onAdjustQty: (String, Double) -> Unit,
  onAddItem: (String, Double, String) -> Unit,
  onRemoveItem: (String) -> Unit,
  onClearBasket: () -> Unit,
  onResetBasket: () -> Unit,
) {
  var newItemName by remember { mutableStateOf("") }
  var newItemQty by remember { mutableStateOf("1") }
  var newItemUnit by remember { mutableStateOf("kg") }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Smart Cart Basket (${state.basketItems.size} items)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row {
              OutlinedButton(onClick = onResetBasket, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Demo")
              }
              Spacer(modifier = Modifier.width(6.dp))
              OutlinedButton(onClick = onClearBasket, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                Text("Clear")
              }
            }
          }

          state.basketItems.forEach { item ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(item.itemName.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelLarge)
                Text("${item.quantity} ${item.unit}", style = MaterialTheme.typography.bodySmall, color = TealCyan)
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onAdjustQty(item.id, -1.0) }) {
                  Icon(Icons.Default.Remove, contentDescription = "Decrease quantity")
                }
                IconButton(onClick = { onAdjustQty(item.id, 1.0) }) {
                  Icon(Icons.Default.Add, contentDescription = "Increase quantity")
                }
                IconButton(onClick = { onRemoveItem(item.id) }) {
                  Icon(Icons.Default.Delete, contentDescription = "Remove item", tint = MaterialTheme.colorScheme.error)
                }
              }
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
              value = newItemName,
              onValueChange = { newItemName = it },
              label = { Text("Item (e.g. rice)") },
              singleLine = true,
              modifier = Modifier.weight(1.4f),
            )
            OutlinedTextField(
              value = newItemQty,
              onValueChange = { newItemQty = it },
              label = { Text("Qty") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(0.7f),
            )
            OutlinedTextField(
              value = newItemUnit,
              onValueChange = { newItemUnit = it },
              label = { Text("Unit") },
              singleLine = true,
              modifier = Modifier.weight(0.8f),
            )
          }
          Button(
            onClick = {
              onAddItem(newItemName, newItemQty.toDoubleOrNull() ?: 1.0, newItemUnit)
              newItemName = ""
            },
            enabled = newItemName.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Add Item to Smart Cart")
          }
        }
      }
    }

    val rec = state.basketRecommendation
    if (rec != null && state.basketItems.isNotEmpty()) {
      item {
        rec.recommendedComplete?.let { winner ->
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, LimeNeon),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LimeNeon)
                Text("Recommended Complete Basket", style = MaterialTheme.typography.labelLarge, color = LimeNeon, fontWeight = FontWeight.Bold)
              }
              Text(
                text = "${winner.platform} — ₹${formatInr(winner.finalPayableCost)} (${winner.deliveryTimeMins} mins)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Subtotal ₹${formatInr(winner.itemSubtotal)} + Delivery ₹${formatInr(winner.deliveryCharge)} + Fee ₹${formatInr(winner.platformFee)} - Discount ₹${formatInr(winner.basketDiscount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Total Basket Cost Comparison (DEMO DATA)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            BasketComparisonCanvasChart(baskets = rec.completeRanked + rec.incompleteRanked)
            rec.incompleteRanked.forEach { inc ->
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(16.dp))
                Text(
                  text = "${inc.platform}: Incomplete basket (missing ${inc.missingItems.joinToString(", ")})",
                  style = MaterialTheme.typography.bodySmall,
                  color = OrangeAccent,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SideBySideCompareSubScreen(
  state: SmartBuyUiState,
  onRemoveCompare: (String) -> Unit,
  onSelectSample: (Int) -> Unit,
  onParseCustom: (String) -> Unit,
  onOpenExternalUrl: (String) -> Unit,
) {
  var customInput by remember { mutableStateOf(state.customProductInput) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (state.comparedProducts.isNotEmpty()) {
      item {
        Text("Selected Products to Compare (${state.comparedProducts.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      }
      items(state.comparedProducts, key = { it.id }) { item ->
        CompareProductSummaryCard(
          item = item,
          onRemove = { onRemoveCompare(item.id) },
          onOrder = { onOpenExternalUrl(item.externalUrl) },
        )
      }
    }

    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Catalog Product Matcher (Modules 1–3)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.sampleProducts.indices.toList()) { idx ->
              FilterChip(
                selected = state.selectedProductIndex == idx,
                onClick = { onSelectSample(idx) },
                label = { Text(state.sampleProducts[idx].productName) },
              )
            }
          }
          state.matchResults.forEach { m ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("${m.platform} · ${m.candidateProduct}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
              Text(
                text = "${m.matchType.label} ${m.confidenceScore.toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = if (m.matchType == MatchType.EXACT) LimeNeon else OrangeAccent,
              )
            }
          }
          state.priceComparison?.let { report ->
            Text(
              text = "Best Offer: ${report.bestPlatform ?: "N/A"} · Lowest ₹${formatInr(report.lowestPrice)} · Diff ₹${formatInr(report.priceDifference)}",
              style = MaterialTheme.typography.labelLarge,
              color = TealCyan,
            )
          }
          OutlinedTextField(
            value = customInput,
            onValueChange = { customInput = it },
            label = { Text("Paste JSON or CSV product row to normalize") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
          )
          state.parseError?.let { err -> Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
          Button(onClick = { onParseCustom(customInput) }, modifier = Modifier.fillMaxWidth()) {
            Text("Normalize & Match Input")
          }
        }
      }
    }
  }
}

@Composable
private fun CompareProductSummaryCard(
  item: PlatformSearchResult,
  onRemove: () -> Unit,
  onOrder: () -> Unit,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("${item.platform.displayName} · ${item.productTitle}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = "Remove from compare") }
      }
      Text(
        text = "Final Cost: ₹${formatInr(item.finalCost)} · ${item.deliveryEstimate}",
        style = MaterialTheme.typography.labelLarge,
        color = LimeNeon,
      )
      Button(onClick = onOrder, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Order on ${item.platform.displayName}")
      }
    }
  }
}

@Composable
private fun BuyhatkeSubScreen(
  state: SmartBuyUiState,
  onSelectCategory: (String) -> Unit,
  onAddManualObservation: (String, String, String, String, Double, Double?, Double?, Double?, String) -> Unit,
  onImportCsv: (String) -> Unit,
  onAttachScreenshot: (Uri?) -> Unit,
) {
  var productName by remember { mutableStateOf("") }
  var productUrl by remember { mutableStateOf("") }
  var platform by remember { mutableStateOf("Amazon India") }
  var currentPrice by remember { mutableStateOf("") }
  var histLow by remember { mutableStateOf("") }
  var histAvg by remember { mutableStateOf("") }
  var csvInput by remember { mutableStateOf("") }

  // Zero-permission Android Photo Picker per Google Play Policy
  val photoPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      onAttachScreenshot(uri)
    }

  val analyses = state.categoryAnalyses.filter { it.category.equals(state.selectedCategory, ignoreCase = true) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(SampleTestDatasets.buyhatkeCategories) { cat ->
          FilterChip(
            selected = state.selectedCategory == cat,
            onClick = { onSelectCategory(cat) },
            label = { Text(cat) },
          )
        }
      }
    }

    items(analyses) { analysis ->
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, LavenderAccent.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("${analysis.productName} (${analysis.platform})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          Text(analysis.dealRecommendation, style = MaterialTheme.typography.labelLarge, color = LimeNeon)
          Text(
            text = "Current ₹${formatInr(analysis.currentPrice)} · Min ₹${formatInr(analysis.observedMin)} · Avg ₹${formatInr(analysis.observedAvg)} · Max ₹${formatInr(analysis.observedMax)} · Trend: ${analysis.priceTrend}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          PriceHistoryCanvasChart(observations = analysis.observations)
        }
      }
    }

    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Log Buyhatke / Product URL Observation (No Invented History)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          OutlinedTextField(value = productName, onValueChange = { productName = it }, label = { Text("Product Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(value = productUrl, onValueChange = { productUrl = it }, label = { Text("Product / Buyhatke URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = platform, onValueChange = { platform = it }, label = { Text("Platform") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(value = currentPrice, onValueChange = { currentPrice = it }, label = { Text("Current Price *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = histLow, onValueChange = { histLow = it }, label = { Text("Observed Low") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(value = histAvg, onValueChange = { histAvg = it }, label = { Text("Observed Avg") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
          }
          OutlinedButton(
            onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (state.attachedScreenshotUri == null) "Attach Price Screenshot (Photo Picker)" else "Screenshot Attached")
          }
          Button(
            onClick = {
              val cur = currentPrice.toDoubleOrNull() ?: return@Button
              onAddManualObservation(
                state.selectedCategory,
                productName,
                productUrl,
                platform,
                cur,
                histLow.toDoubleOrNull(),
                histAvg.toDoubleOrNull(),
                null,
                "2026-11-23",
              )
              productName = ""
              currentPrice = ""
            },
            enabled = productName.isNotBlank() && currentPrice.toDoubleOrNull() != null,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Save Observation")
          }

          OutlinedTextField(value = csvInput, onValueChange = { csvInput = it }, label = { Text("Or Paste Buyhatke CSV") }, minLines = 2, modifier = Modifier.fillMaxWidth())
          state.csvImportMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = TealCyan) }
          OutlinedButton(onClick = { onImportCsv(csvInput) }, modifier = Modifier.fillMaxWidth()) {
            Text("Import CSV Observations")
          }
        }
      }
    }
  }
}

@Composable
private fun EngineSubScreen(
  state: SmartBuyUiState,
  onBackendUrlChange: (String) -> Unit,
  onPingBackend: () -> Unit,
  onRunTests: () -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Colab FastAPI Analytical Bridge", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          OutlinedTextField(
            value = state.backendUrl,
            onValueChange = onBackendUrlChange,
            label = { Text("FastAPI Server URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          Button(onClick = onPingBackend, enabled = !state.isPingingBackend, modifier = Modifier.fillMaxWidth()) {
            if (state.isPingingBackend) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            else Text("Ping /health Endpoint")
          }
          state.pingResult?.let { ping ->
            Text(
              text = ping.message,
              style = MaterialTheme.typography.bodySmall,
              color = if (ping.success) LimeNeon else MaterialTheme.colorScheme.error,
            )
          }
        }
      }
    }

    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Module 8 Verification (${state.engineTestResults.count { it.passed }}/${state.engineTestResults.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onRunTests) {
              Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
              Text("Re-run")
            }
          }
          state.engineTestResults.forEach { res ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Column(modifier = Modifier.weight(1f)) {
                Text(res.testName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(res.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Text(
                text = if (res.passed) "PASS" else "FAIL",
                color = if (res.passed) LimeNeon else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }
  }
}
