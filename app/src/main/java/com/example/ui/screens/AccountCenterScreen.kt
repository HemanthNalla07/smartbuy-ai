package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import com.example.domain.DeliveryAddress
import com.example.firebase.GoogleSignInButton
import com.example.firebase.signOutGoogle
import com.example.ui.SmartBuyUiState
import com.example.ui.components.SmartBuyTopHeader
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LimeNeon
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.TealCyan

@Composable
fun AccountCenterScreen(
  state: SmartBuyUiState,
  onOpenLocationModal: (DeliveryAddress?) -> Unit,
  onSwitchAddress: (DeliveryAddress) -> Unit,
  onDeleteAddress: (String) -> Unit,
  onUpdateProfile: (String, String) -> Unit,
  onRunSearch: (String) -> Unit,
  onClearSearches: () -> Unit,
  onOpenWishlistTab: () -> Unit,
  onAddPriceAlert: (String, Double, Double) -> Unit,
  onToggleAlert: (String, Boolean) -> Unit,
  onDeleteAlert: (String) -> Unit,
  onChangeTheme: (AppThemeMode) -> Unit,
  onClearAppData: () -> Unit,
  onStatusMessage: (String) -> Unit,
  onOpenCart: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var selectedSection by rememberSaveable { mutableIntStateOf(0) }
  val sections =
    listOf(
      "My Profile",
      "My Addresses",
      "My Searches",
      "My Wishlist",
      "My Price Alerts",
      "My Comparison History",
      "Settings",
    )

  Column(modifier = Modifier.fillMaxSize()) {
    SmartBuyTopHeader(
      activeAddress = state.activeAddress,
      cartCount = state.basketItems.size,
      isSignedIn = state.isSignedIn,
      onChangeLocation = { onOpenLocationModal(null) },
      onOpenCart = onOpenCart,
      onOpenAccount = {},
    )

    // Google Sign-In / Guest Mode Banner
    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, if (state.isSignedIn) LimeNeon else MaterialTheme.colorScheme.primary),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
              imageVector = if (state.isSignedIn) Icons.Default.CloudDone else Icons.Default.Person,
              contentDescription = null,
              tint = if (state.isSignedIn) LimeNeon else MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = if (state.isSignedIn) state.profileName else "Guest Mode (Local Storage)",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
          }
          Text(
            text =
              if (state.isSignedIn) "Cloud Sync Active · ${state.profileEmail}"
              else "Sign in with Google to sync addresses, wishlist, alerts & search history.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (state.isSignedIn) {
          OutlinedButton(
            onClick = {
              signOutGoogle(credentialManager, scope) {
                onStatusMessage("Signed out — switched to local Guest Mode")
              }
            },
            modifier = Modifier.testTag("sign_out_button"),
          ) {
            Text("Sign Out")
          }
        } else {
          GoogleSignInButton(
            onAuthSuccess = { onStatusMessage("Signed in with Google — cloud sync active") },
            onAuthError = { msg -> onStatusMessage(msg) },
          )
        }
      }
    }

    ScrollableTabRow(
      selectedTabIndex = selectedSection,
      containerColor = MaterialTheme.colorScheme.surface,
      edgePadding = 12.dp,
    ) {
      sections.forEachIndexed { index, title ->
        Tab(
          selected = selectedSection == index,
          onClick = { selectedSection = index },
          text = { Text(title) },
          modifier = Modifier.testTag("account_section_$index"),
        )
      }
    }

    when (selectedSection) {
      // 1. My Profile
      0 -> {
        var nameInput by remember(state.profileName) { mutableStateOf(state.profileName) }
        var phoneInput by remember(state.profilePhone) { mutableStateOf(state.profilePhone) }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("My Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Display Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.profileEmail.ifBlank { "Guest session (not signed in)" }, onValueChange = {}, readOnly = true, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phoneInput, onValueChange = { phoneInput = it }, label = { Text("Phone (Optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = { onUpdateProfile(nameInput, phoneInput) }, modifier = Modifier.fillMaxWidth()) {
                  Text("Save Profile")
                }
              }
            }
          }
        }
      }

      // 2. My Addresses
      1 -> {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          item {
            Button(
              onClick = { onOpenLocationModal(null) },
              modifier = Modifier.fillMaxWidth().testTag("account_add_address_button"),
            ) {
              Icon(Icons.Default.Add, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Add New Address / Detect Location")
            }
          }
          if (state.savedAddresses.isEmpty()) {
            item {
              Text(
                text = "Current Active Default: ${state.activeAddress.shortHeaderText}. Add a full address above to save multiple locations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          items(state.savedAddresses, key = { it.id }) { addr ->
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, if (addr.isDefault) LimeNeon else MaterialTheme.colorScheme.outline),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = if (addr.isDefault) LimeNeon else TealCyan, modifier = Modifier.size(18.dp))
                    Text("${addr.label} · ${addr.shortHeaderText}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                  }
                  if (addr.isDefault) SpecBadge("Default", LimeNeon)
                }
                Text(addr.fullFormatted, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  if (!addr.isDefault) {
                    OutlinedButton(onClick = { onSwitchAddress(addr) }) {
                      Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Set Default")
                    }
                  }
                  OutlinedButton(onClick = { onOpenLocationModal(addr) }) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                  }
                  IconButton(onClick = { onDeleteAddress(addr.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete address", tint = MaterialTheme.colorScheme.error)
                  }
                }
              }
            }
          }
        }
      }

      // 3. My Searches
      2 -> {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Text("My Searches (${state.searchHistory.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              if (state.searchHistory.isNotEmpty()) {
                OutlinedButton(onClick = onClearSearches) { Text("Clear History") }
              }
            }
          }
          items(state.searchHistory, key = { it.id }) { item ->
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(item.queryText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                  Text("${item.parsedCategory} · PIN ${item.pinCode}", style = MaterialTheme.typography.bodySmall, color = TealCyan)
                }
                OutlinedButton(onClick = { onRunSearch(item.queryText) }) { Text("Search") }
              }
            }
          }
        }
      }

      // 4. My Wishlist
      3 -> {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Saved Wishlist Items: ${state.wishlist.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Manage all saved items, set price alerts, or re-compare offers in the Wishlist tab.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = onOpenWishlistTab, modifier = Modifier.fillMaxWidth()) { Text("Open Full Wishlist") }
              }
            }
          }
        }
      }

      // 5. My Price Alerts
      4 -> {
        var prodInput by remember { mutableStateOf("") }
        var targetInput by remember { mutableStateOf("") }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add Price Alert", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = prodInput, onValueChange = { prodInput = it }, label = { Text("Product Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = targetInput, onValueChange = { targetInput = it.filter { c -> c.isDigit() } }, label = { Text("Target Price (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(
                  onClick = {
                    val target = targetInput.toDoubleOrNull() ?: return@Button
                    onAddPriceAlert(prodInput, target, target * 1.15)
                    prodInput = ""
                    targetInput = ""
                  },
                  enabled = prodInput.isNotBlank() && (targetInput.toDoubleOrNull() ?: 0.0) > 0.0,
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Text("Create Price Alert")
                }
              }
            }
          }
          items(state.priceAlerts, key = { it.id }) { alert ->
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(alert.productName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                  }
                  Text("Target ₹${formatInr(alert.targetPrice)} · Status: ${alert.status}", style = MaterialTheme.typography.bodySmall, color = TealCyan)
                }
                Switch(checked = alert.enabled, onCheckedChange = { onToggleAlert(alert.id, it) })
                IconButton(onClick = { onDeleteAlert(alert.id) }) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete alert", tint = MaterialTheme.colorScheme.error)
                }
              }
            }
          }
        }
      }

      // 6. My Comparison History
      5 -> {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          item {
            Text("Comparison History (${state.comparisonHistory.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
          items(state.comparisonHistory, key = { it.id }) { comp ->
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(comp.productName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                  Text("Best: ${comp.cheapestPlatform} (${comp.platformsCount} platforms checked)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("₹${formatInr(comp.lowestFinalCost)}", style = MaterialTheme.typography.titleSmall, color = LimeNeon, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // 7. Settings
      else -> {
        val hasFine =
          ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse =
          ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val locationStatus =
          when {
            hasFine -> "Granted (Precise)"
            hasCoarse -> "Granted (Approximate)"
            else -> "Not granted (Optional — PIN entry always available)"
          }

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Theme Preference", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  AppThemeMode.entries.forEach { mode ->
                    OutlinedButton(
                      onClick = { onChangeTheme(mode) },
                      border = BorderStroke(1.dp, if (state.themeMode == mode) LimeNeon else MaterialTheme.colorScheme.outline),
                    ) {
                      Text(mode.label)
                    }
                  }
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
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Location Permission Status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(locationStatus, style = MaterialTheme.typography.bodySmall, color = if (hasFine || hasCoarse) LimeNeon else OrangeAccent)
                OutlinedButton(onClick = { onOpenLocationModal(null) }) { Text("Change / Detect Delivery Location") }
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
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Icon(Icons.Default.Security, contentDescription = null, tint = TealCyan, modifier = Modifier.size(18.dp))
                  Text("Privacy & External Services", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Text(
                  text =
                    "• SmartBuy AI uses Firebase AI Logic (Gemini) for query parsing and product usage insights.\n" +
                      "• Google Sign-In & Firestore sync are optional; guest searches stay on your device.\n" +
                      "• Orders open the official external shopping platform directly — SmartBuy AI never collects card, UPI, or marketplace passwords.\n" +
                      "• Demo catalog entries are labeled DEMO DATA; platforms without live APIs show EXTERNAL SEARCH links.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                  onClick = onClearAppData,
                  modifier = Modifier.fillMaxWidth().testTag("clear_app_data_button"),
                ) {
                  Text("Clear Local App History & Cache", color = MaterialTheme.colorScheme.error)
                }
              }
            }
          }
        }
      }
    }
  }
}
