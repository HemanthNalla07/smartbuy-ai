package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.domain.DeliveryAddress
import com.example.ui.theme.LimeNeon

@Composable
fun SmartBuyTopHeader(
  activeAddress: DeliveryAddress,
  cartCount: Int,
  isSignedIn: Boolean,
  onChangeLocation: () -> Unit,
  onOpenCart: () -> Unit,
  onOpenAccount: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Image(
            painter = painterResource(R.drawable.ic_smartbuy_foreground_1795449206408),
            contentDescription = "SmartBuy AI logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(34.dp).clip(CircleShape),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "SmartBuy AI",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "Price Comparison",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onOpenCart,
            modifier = Modifier.testTag("header_cart_button"),
          ) {
            BadgedBox(
              badge = {
                if (cartCount > 0) {
                  Badge(containerColor = MaterialTheme.colorScheme.primary) {
                    Text(
                      text = cartCount.toString(),
                      color = MaterialTheme.colorScheme.onPrimary,
                    )
                  }
                }
              }
            ) {
              Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = "Smart Cart",
                tint = MaterialTheme.colorScheme.onSurface,
              )
            }
          }

          IconButton(
            onClick = onOpenAccount,
            modifier = Modifier.testTag("header_account_button"),
          ) {
            Icon(
              imageVector = Icons.Default.AccountCircle,
              contentDescription = "Account Center",
              tint = if (isSignedIn) LimeNeon else MaterialTheme.colorScheme.secondary,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier =
          Modifier.fillMaxWidth()
            .clickable(onClick = onChangeLocation)
            .testTag("change_location_button"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Delivering to ${activeAddress.shortHeaderText}",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "Change location",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.secondary,
              fontWeight = FontWeight.SemiBold,
            )
            Icon(
              imageVector = Icons.Default.KeyboardArrowDown,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
fun LocationPickerDialog(
  activeAddress: DeliveryAddress,
  editingAddress: DeliveryAddress?,
  detectedDraftAddress: DeliveryAddress?,
  savedAddresses: List<DeliveryAddress>,
  isDetectingLocation: Boolean,
  locationError: String?,
  onUseCurrentLocationClick: () -> Unit,
  onSwitchAddress: (DeliveryAddress) -> Unit,
  onSaveAddress: (DeliveryAddress) -> Unit,
  onDismiss: () -> Unit,
) {
  val seed = detectedDraftAddress ?: editingAddress ?: activeAddress

  var label by remember(seed) { mutableStateOf(seed.label.ifBlank { "Home" }) }
  var houseNo by remember(seed) { mutableStateOf(seed.houseNo) }
  var street by remember(seed) { mutableStateOf(seed.street) }
  var area by remember(seed) { mutableStateOf(seed.area) }
  var city by remember(seed) { mutableStateOf(seed.city) }
  var state by remember(seed) { mutableStateOf(seed.state) }
  var pinCode by remember(seed) { mutableStateOf(seed.pinCode) }
  var landmark by remember(seed) { mutableStateOf(seed.landmark) }

  LaunchedEffect(detectedDraftAddress) {
    if (detectedDraftAddress != null) {
      label = detectedDraftAddress.label
      houseNo = detectedDraftAddress.houseNo
      street = detectedDraftAddress.street
      area = detectedDraftAddress.area
      city = detectedDraftAddress.city
      state = detectedDraftAddress.state
      pinCode = detectedDraftAddress.pinCode
      landmark = detectedDraftAddress.landmark
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Choose Delivery Location",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Delivery fees & quick-commerce availability depend on your Indian PIN code.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_location_dialog")) {
            Icon(Icons.Default.Close, contentDescription = "Close location dialog")
          }
        }

        // Option 1: Use Current Location (permission requested only on tap)
        OutlinedButton(
          onClick = onUseCurrentLocationClick,
          enabled = !isDetectingLocation,
          modifier = Modifier.fillMaxWidth().testTag("use_current_location_button"),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
        ) {
          if (isDetectingLocation) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Detecting current location...")
          } else {
            Icon(
              imageVector = Icons.Default.MyLocation,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Use Current Location (GPS + Confirm)",
              color = MaterialTheme.colorScheme.secondary,
            )
          }
        }

        if (!locationError.isNullOrBlank()) {
          Surface(
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
            shape = RoundedCornerShape(8.dp),
          ) {
            Text(
              text = locationError,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.fillMaxWidth().padding(10.dp),
            )
          }
        }

        // Saved Addresses quick switcher
        if (savedAddresses.isNotEmpty()) {
          Text(
            text = "Saved Addresses",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
          )
          savedAddresses.forEach { saved ->
            val isCurrent = saved.pinCode == activeAddress.pinCode && saved.area == activeAddress.area
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              border = BorderStroke(1.dp, if (isCurrent) LimeNeon else MaterialTheme.colorScheme.outline),
              modifier = Modifier.fillMaxWidth().clickable { onSwitchAddress(saved) },
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${saved.label} · ${saved.shortHeaderText}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = saved.fullFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                if (isCurrent) {
                  Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = LimeNeon)
                }
              }
            }
          }
          HorizontalDivider()
        }

        // Option 2: Manual Indian Address Entry
        Text(
          text = "Enter Delivery Address / PIN Code",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("Home", "Work", "Other").forEach { option ->
            val selected = label.equals(option, ignoreCase = true)
            OutlinedButton(
              onClick = { label = option },
              colors =
                ButtonDefaults.outlinedButtonColors(
                  containerColor =
                    if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ),
            ) {
              Text(option)
            }
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
            value = pinCode,
            onValueChange = { pinCode = it.filter { ch -> ch.isDigit() }.take(6) },
            label = { Text("PIN Code *") },
            placeholder = { Text("560103") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).testTag("address_pin_input"),
          )
          OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("City *") },
            singleLine = true,
            modifier = Modifier.weight(1.2f).testTag("address_city_input"),
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
            value = houseNo,
            onValueChange = { houseNo = it },
            label = { Text("House / Flat No.") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = state,
            onValueChange = { state = it },
            label = { Text("State") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }

        OutlinedTextField(
          value = street,
          onValueChange = { street = it },
          label = { Text("Street / Road") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
            value = area,
            onValueChange = { area = it },
            label = { Text("Area / Locality") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = landmark,
            onValueChange = { landmark = it },
            label = { Text("Landmark (Optional)") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(onClick = onDismiss) {
            Text("Keep Current")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              onSaveAddress(
                DeliveryAddress(
                  id = editingAddress?.id.orEmpty(),
                  label = label,
                  houseNo = houseNo,
                  street = street,
                  area = area,
                  city = city,
                  state = state,
                  pinCode = pinCode,
                  landmark = landmark,
                  isDefault = true,
                )
              )
            },
            modifier = Modifier.testTag("save_address_button"),
          ) {
            Text("Save & Deliver Here")
          }
        }
      }
    }
  }
}
