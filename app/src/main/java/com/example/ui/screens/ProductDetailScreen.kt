package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.DeliveryAddress
import com.example.domain.PlatformSearchResult
import com.example.domain.ProductUsageInfo
import com.example.ui.SmartBuyUiState
import com.example.ui.UiState
import com.example.ui.theme.LavenderAccent
import com.example.ui.theme.LimeNeon
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.TealCyan

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
  product: PlatformSearchResult,
  state: SmartBuyUiState,
  activeAddress: DeliveryAddress,
  onBack: () -> Unit,
  onOrderOnPlatform: (String) -> Unit,
  onComparePrices: (PlatformSearchResult) -> Unit,
  onToggleWishlist: (PlatformSearchResult) -> Unit,
  onSetPriceAlert: (String, Double, Double) -> Unit,
  onLoadUsageInfo: (PlatformSearchResult) -> Unit,
) {
  BackHandler(onBack = onBack)

  val isWishlisted = state.wishlist.any { it.productName.equals(product.productTitle, ignoreCase = true) }
  var showAlertDialog by remember { mutableStateOf(false) }
  var alertTargetInput by remember(product.id) {
    mutableStateOf(((product.finalCost ?: product.productPrice ?: 500.0) * 0.9).toInt().toString())
  }

  val relatedPlatformOffers =
    state.rawSearchResults.filter { !it.isExternalSearchOnly && it.id != product.id }.take(5)

  val buyhatkePreview =
    state.categoryAnalyses.firstOrNull {
      it.productName.contains(product.brand, ignoreCase = true) ||
        product.category.label.contains(it.category, ignoreCase = true)
    }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = { Text("Product Details", maxLines = 1) },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
      )
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Main Product Overview Card
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              SpecBadge(product.platform.displayName, MaterialTheme.colorScheme.primary)
              SpecBadge(product.category.label, TealCyan)
              SpecBadge(product.sourceLabel, LavenderAccent)
            }

            Text(
              text = product.productTitle,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )

            if (product.brand.isNotBlank()) {
              Text(
                text = "Brand: ${product.brand}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
              )
            }

            if (product.finalCost != null) {
              Text(
                text = "Final Payable: ₹${formatInr(product.finalCost)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = LimeNeon,
              )
              Text(
                text =
                  "Product ₹${formatInr(product.productPrice)} + Delivery ₹${formatInr(product.deliveryFee)} + Platform Fee ₹${formatInr(product.platformFee)} - Discount ₹${formatInr(product.discountAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            } else if (product.productPrice != null) {
              Text(
                text = "Listed Price: ₹${formatInr(product.productPrice)} (Fees incomplete)",
                style = MaterialTheme.typography.titleLarge,
                color = OrangeAccent,
                fontWeight = FontWeight.Bold,
              )
            } else {
              Text(
                text = "Live Price Available on ${product.platform.displayName}",
                style = MaterialTheme.typography.titleMedium,
                color = TealCyan,
              )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.LocalShipping, contentDescription = null, tint = TealCyan, modifier = Modifier.size(18.dp))
              Text(
                text = "${product.deliveryEstimate} (Active PIN: ${activeAddress.pinCode})",
                style = MaterialTheme.typography.bodySmall,
                color = if (product.deliveryVerifiedForPin) TealCyan else OrangeAccent,
              )
            }

            Text(
              text = product.description,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )

            if (product.variants.isNotEmpty()) {
              Text("Available Variants:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
              FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                product.variants.forEach { variant -> SpecBadge(variant, MaterialTheme.colorScheme.primary) }
              }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              if (product.material.isNotBlank()) SpecBadge("Material: ${product.material}", LavenderAccent)
              if (product.sizeOrCapacity.isNotBlank()) SpecBadge("Size/Cap: ${product.sizeOrCapacity}", TealCyan)
              if (product.colour.isNotBlank()) SpecBadge("Colour: ${product.colour}", MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }

      // Action Buttons Row
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = { onOrderOnPlatform(product.externalUrl) },
            colors = ButtonDefaults.buttonColors(containerColor = LimeNeon, contentColor = MaterialTheme.colorScheme.onTertiary),
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("detail_order_button"),
          ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.order_on_platform), fontWeight = FontWeight.Bold)
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
              onClick = { onComparePrices(product) },
              modifier = Modifier.weight(1f),
            ) {
              Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Compare")
            }
            OutlinedButton(
              onClick = { onToggleWishlist(product) },
              modifier = Modifier.weight(1f),
            ) {
              Icon(
                if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (isWishlisted) "Saved" else "Wishlist")
            }
            OutlinedButton(
              onClick = { showAlertDialog = true },
              modifier = Modifier.weight(1f),
            ) {
              Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Alert")
            }
          }
        }
      }

      // Gemini AI: "What is this product useful for?"
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Text(
                text = "Product Intelligence",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
            }

            Button(
              onClick = { onLoadUsageInfo(product) },
              enabled = state.usageInfoState !is UiState.Loading,
              modifier = Modifier.fillMaxWidth().testTag("what_is_useful_for_button"),
            ) {
              Text(stringResource(R.string.what_is_this_useful_for), fontWeight = FontWeight.Bold)
            }

            when (val ui = state.usageInfoState) {
              UiState.Idle -> {
                Text(
                  text = "Tap the button above for an objective, product-specific guide on uses, features, pros/cons, and buying tips.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              UiState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                  CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                  Text("Generating consumer insights via Gemini AI...", style = MaterialTheme.typography.bodySmall)
                }
              }
              is UiState.Error -> {
                Text(text = ui.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { onLoadUsageInfo(product) }) { Text("Retry") }
              }
              is UiState.Success -> {
                ProductUsageCardContent(ui.data)
              }
            }
          }
        }
      }

      // Pros & Cons
      if (product.pros.isNotEmpty() || product.cons.isNotEmpty()) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text("Pros & Cons", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              product.pros.forEach { pro ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Icon(Icons.Default.ThumbUp, contentDescription = null, tint = LimeNeon, modifier = Modifier.size(16.dp))
                  Text(pro, style = MaterialTheme.typography.bodySmall)
                }
              }
              product.cons.forEach { con ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Icon(Icons.Default.ThumbDown, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(16.dp))
                  Text(con, style = MaterialTheme.typography.bodySmall)
                }
              }
            }
          }
        }
      }

      // Cross-Platform Price Comparison Preview
      if (relatedPlatformOffers.isNotEmpty()) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text("Other Platforms", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              relatedPlatformOffers.forEach { other ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(other.platform.displayName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(other.deliveryEstimate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                  Text(
                    text = if (other.finalCost != null) "₹${formatInr(other.finalCost)}" else "Verify",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TealCyan,
                  )
                }
              }
            }
          }
        }
      }

      // Buyhatke Historical Preview
      if (buyhatkePreview != null) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, LavenderAccent.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "Buyhatke Historical Snapshot (${buyhatkePreview.category})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = LavenderAccent,
              )
              Text("${buyhatkePreview.productName} · ${buyhatkePreview.dealRecommendation}", style = MaterialTheme.typography.bodySmall)
              Text(
                text = "Observed Min ₹${formatInr(buyhatkePreview.observedMin)} · Avg ₹${formatInr(buyhatkePreview.observedAvg)} · Trend: ${buyhatkePreview.priceTrend}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }

  if (showAlertDialog) {
    AlertDialog(
      onDismissRequest = { showAlertDialog = false },
      title = { Text("Set Price Alert") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Notify when ${product.productTitle} drops to:", style = MaterialTheme.typography.bodySmall)
          OutlinedTextField(
            value = alertTargetInput,
            onValueChange = { alertTargetInput = it.filter { c -> c.isDigit() } },
            label = { Text("Target Price (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val target = alertTargetInput.toDoubleOrNull() ?: 0.0
            if (target > 0.0) {
              onSetPriceAlert(product.productTitle, target, product.finalCost ?: product.productPrice ?: target)
            }
            showAlertDialog = false
          }
        ) {
          Text("Save Alert")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAlertDialog = false }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun ProductUsageCardContent(info: ProductUsageInfo) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(text = info.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = LimeNeon)
    Text(text = info.summary, style = MaterialTheme.typography.bodySmall)
    if (info.whoShouldBuy.isNotBlank()) {
      Text(
        text = "Who should consider it: ${info.whoShouldBuy}",
        style = MaterialTheme.typography.bodySmall,
        color = TealCyan,
      )
    }
    BulletSection("Common Uses", info.commonUses)
    BulletSection("Key Features & Highlights", info.keyFeatures)
    BulletSection("Advantages", info.advantages)
    BulletSection("Limitations", info.limitations)
    BulletSection("Buying Considerations", info.buyingConsiderations)
  }
}

@Composable
private fun BulletSection(title: String, items: List<String>) {
  if (items.isEmpty()) return
  Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
    Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    items.forEach { bullet ->
      Text(text = "• $bullet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
  }
}
