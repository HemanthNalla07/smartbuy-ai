package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.AccountWishlistItem
import com.example.ui.SmartBuyUiState
import com.example.ui.components.SmartBuyTopHeader
import com.example.ui.theme.LimeNeon
import com.example.ui.theme.TealCyan

@Composable
fun WishlistScreen(
  state: SmartBuyUiState,
  onSearchProduct: (String) -> Unit,
  onRemoveWishlist: (String) -> Unit,
  onSetAlert: (String, Double, Double) -> Unit,
  onOpenExternalUrl: (String) -> Unit,
  onGoToSearch: () -> Unit,
  onChangeLocation: () -> Unit,
  onOpenCart: () -> Unit,
  onOpenAccount: () -> Unit,
) {
  var alertTargetItem by remember { mutableStateOf<AccountWishlistItem?>(null) }
  var targetInput by remember { mutableStateOf("") }

  Column(modifier = Modifier.fillMaxSize()) {
    SmartBuyTopHeader(
      activeAddress = state.activeAddress,
      cartCount = state.basketItems.size,
      isSignedIn = state.isSignedIn,
      onChangeLocation = onChangeLocation,
      onOpenCart = onOpenCart,
      onOpenAccount = onOpenAccount,
    )

    if (state.wishlist.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            imageVector = Icons.Default.FavoriteBorder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
          )
          Text("Your Wishlist is Empty", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text(
            text = "Save products from search results to track prices and compare platforms anytime.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Button(onClick = onGoToSearch, modifier = Modifier.testTag("wishlist_search_cta")) {
            Text("Search Products")
          }
        }
      }
      return
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Text(
          text = "Saved Wishlist (${state.wishlist.size})",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
      }
      items(state.wishlist, key = { it.id }) { item ->
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
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SpecBadge(item.category, TealCyan)
                SpecBadge(item.bestPlatform, MaterialTheme.colorScheme.primary)
              }
              IconButton(onClick = { onRemoveWishlist(item.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Remove wishlist item", tint = MaterialTheme.colorScheme.error)
              }
            }
            Text(item.productName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (item.bestPrice > 0.0) {
              Text(
                text = "Saved Best Price: ₹${formatInr(item.bestPrice)}",
                style = MaterialTheme.typography.titleMedium,
                color = LimeNeon,
                fontWeight = FontWeight.Bold,
              )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
              Button(
                onClick = { onOpenExternalUrl(item.externalUrl) },
                modifier = Modifier.weight(1f),
              ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("View")
              }
              OutlinedButton(onClick = { onSearchProduct(item.productName) }) {
                Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Re-compare")
              }
              OutlinedButton(
                onClick = {
                  alertTargetItem = item
                  targetInput = (item.bestPrice * 0.9).toInt().coerceAtLeast(10).toString()
                }
              ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = "Set Price Alert", modifier = Modifier.size(16.dp))
              }
            }
          }
        }
      }
    }
  }

  alertTargetItem?.let { item ->
    AlertDialog(
      onDismissRequest = { alertTargetItem = null },
      title = { Text("Set Price Alert") },
      text = {
        OutlinedTextField(
          value = targetInput,
          onValueChange = { targetInput = it.filter { c -> c.isDigit() } },
          label = { Text("Target Price for ${item.productName} (₹)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
        )
      },
      confirmButton = {
        Button(
          onClick = {
            val target = targetInput.toDoubleOrNull() ?: 0.0
            if (target > 0.0) onSetAlert(item.productName, target, item.bestPrice)
            alertTargetItem = null
          }
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { alertTargetItem = null }) { Text("Cancel") }
      },
    )
  }
}
