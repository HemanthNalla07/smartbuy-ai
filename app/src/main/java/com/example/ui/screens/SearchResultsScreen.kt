package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.MatchType
import com.example.domain.PlatformSearchResult
import com.example.domain.SearchSortOption
import com.example.ui.SmartBuyUiState
import com.example.ui.components.SmartBuyTopHeader
import com.example.ui.theme.LavenderAccent
import com.example.ui.theme.LimeNeon
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.TealCyan
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchResultsScreen(
  state: SmartBuyUiState,
  onQueryChange: (String) -> Unit,
  onClearQuery: () -> Unit,
  onSubmitSearch: (String) -> Unit,
  onVoiceSearchClick: () -> Unit,
  onSortChange: (SearchSortOption) -> Unit,
  onToggleInStock: () -> Unit,
  onToggleUnderBudget: () -> Unit,
  onOpenProductDetail: (PlatformSearchResult) -> Unit,
  onAddToCompare: (PlatformSearchResult) -> Unit,
  onToggleWishlist: (PlatformSearchResult) -> Unit,
  onOpenExternalUrl: (String) -> Unit,
  onChangeLocation: () -> Unit,
  onOpenCart: () -> Unit,
  onOpenAccount: () -> Unit,
) {
  val results = state.searchResults
  val cheapestVerified = results.firstOrNull { it.isVerifiedCheapestEligible }
  val exactMatches = results.filter { !it.isExternalSearchOnly && it.matchType == MatchType.EXACT }
  val similarAlternatives = results.filter { !it.isExternalSearchOnly && it.matchType != MatchType.EXACT }
  val externalLinks = results.filter { it.isExternalSearchOnly }

  Column(modifier = Modifier.fillMaxSize()) {
    SmartBuyTopHeader(
      activeAddress = state.activeAddress,
      cartCount = state.basketItems.size,
      isSignedIn = state.isSignedIn,
      onChangeLocation = onChangeLocation,
      onOpenCart = onOpenCart,
      onOpenAccount = onOpenAccount,
    )

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = state.searchQuery,
          onValueChange = onQueryChange,
          placeholder = { Text(stringResource(R.string.search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
          singleLine = true,
          trailingIcon = {
            Row {
              if (state.searchQuery.isNotEmpty()) {
                IconButton(onClick = onClearQuery) {
                  Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
              }
              IconButton(onClick = onVoiceSearchClick) {
                Icon(Icons.Default.Mic, contentDescription = "Voice search", tint = TealCyan)
              }
            }
          },
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(onSearch = { onSubmitSearch(state.searchQuery) }),
          modifier = Modifier.weight(1f).testTag("results_search_input"),
        )
        Button(
          onClick = { onSubmitSearch(state.searchQuery) },
          enabled = state.searchQuery.trim().isNotEmpty(),
          modifier = Modifier.height(54.dp).testTag("results_search_submit"),
        ) {
          Icon(Icons.Default.Search, contentDescription = "Search")
        }
      }
    }

    if (state.isSearching) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
          CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
          Text(
            text = "Parsing product specs with Gemini AI & comparing platforms...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      return
    }

    if (state.parsedQuery == null) {
      Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
          text = "Enter any product name above (e.g. \"1 kg bananas\", \"iPhone 16\", \"1 litre stainless steel water bottle under ₹500\") to compare prices.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      return
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Parsed Query Specs Summary
      item {
        val pq = state.parsedQuery
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = if (pq.parsedByGemini) LimeNeon else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = state.geminiParseNotice ?: "Parsed Query",
                style = MaterialTheme.typography.labelLarge,
                color = if (pq.parsedByGemini) LimeNeon else MaterialTheme.colorScheme.primary,
              )
            }
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              SpecBadge("Category: ${pq.category.label}", TealCyan)
              if (pq.brand.isNotBlank()) SpecBadge("Brand: ${pq.brand}", MaterialTheme.colorScheme.primary)
              if (pq.material.isNotBlank()) SpecBadge("Material: ${pq.material}", LavenderAccent)
              if (pq.capacity.isNotBlank()) SpecBadge("Capacity: ${pq.capacity}", OrangeAccent)
              if (pq.weight.isNotBlank()) SpecBadge("Weight: ${pq.weight}", OrangeAccent)
              if (pq.size.isNotBlank()) SpecBadge("Size: ${pq.size}", OrangeAccent)
              if (pq.budgetInr != null) SpecBadge("Budget: ₹${pq.budgetInr.toInt()}", LimeNeon)
            }
          }
        }
      }

      // Sort & Filter Controls
      item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(SearchSortOption.entries) { option ->
            FilterChip(
              selected = state.sortOption == option,
              onClick = { onSortChange(option) },
              label = { Text(option.label) },
            )
          }
          item {
            FilterChip(
              selected = state.inStockOnly,
              onClick = onToggleInStock,
              label = { Text("In Stock Only") },
            )
          }
          if (state.parsedQuery.budgetInr != null) {
            item {
              FilterChip(
                selected = state.underBudgetOnly,
                onClick = onToggleUnderBudget,
                label = { Text("Under Budget") },
              )
            }
          }
        }
      }

      // Cheapest Verified Option Highlight
      item {
        if (cheapestVerified != null && cheapestVerified.finalCost != null) {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, LimeNeon),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("cheapest_verified_card"),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Icon(Icons.Default.Verified, contentDescription = null, tint = LimeNeon, modifier = Modifier.size(18.dp))
                  Text(
                    text = "LOWEST VERIFIED FINAL COST",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = LimeNeon,
                  )
                }
                SpecBadge(cheapestVerified.sourceLabel, MaterialTheme.colorScheme.primary)
              }

              Text(
                text = "${cheapestVerified.platform.displayName} — ${cheapestVerified.productTitle}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )

              Text(
                text = "Final Payable: ₹${formatInr(cheapestVerified.finalCost)}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = LimeNeon,
              )

              Text(
                text =
                  "Item ₹${formatInr(cheapestVerified.productPrice)} + Delivery ₹${formatInr(cheapestVerified.deliveryFee)} + Platform Fee ₹${formatInr(cheapestVerified.platformFee)} - Discount ₹${formatInr(cheapestVerified.discountAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )

              Text(
                text = cheapestVerified.deliveryEstimate,
                style = MaterialTheme.typography.labelMedium,
                color = TealCyan,
              )

              Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                  onClick = { onOpenExternalUrl(cheapestVerified.externalUrl) },
                  colors = ButtonDefaults.buttonColors(containerColor = LimeNeon, contentColor = MaterialTheme.colorScheme.onTertiary),
                  modifier = Modifier.weight(1f).testTag("cheapest_order_button"),
                ) {
                  Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(stringResource(R.string.order_on_platform), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = { onOpenProductDetail(cheapestVerified) }) {
                  Text("Details")
                }
              }
            }
          }
        } else {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, OrangeAccent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Info, contentDescription = null, tint = OrangeAccent)
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text =
                  "No complete verified demo pricing is available for this query/PIN (${state.activeAddress.pinCode}). Open the external platform search links below to check live checkout totals.",
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
        }
      }

      // Exact Matches Section
      if (exactMatches.isNotEmpty()) {
        item {
          Text(
            text = "Exact Matches (${exactMatches.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = LimeNeon,
          )
        }
        items(exactMatches, key = { it.id }) { item ->
          PlatformResultCard(
            item = item,
            isWishlisted = state.wishlist.any { it.productName.equals(item.productTitle, ignoreCase = true) },
            onOpenDetail = { onOpenProductDetail(item) },
            onCompare = { onAddToCompare(item) },
            onWishlist = { onToggleWishlist(item) },
            onOrder = { onOpenExternalUrl(item.externalUrl) },
          )
        }
      }

      // Similar Alternatives Section
      if (similarAlternatives.isNotEmpty()) {
        item {
          Text(
            text = "Similar Alternatives (${similarAlternatives.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OrangeAccent,
          )
          Text(
            text = "Differs in material, capacity, size, or brand — shown separately from exact matches.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        items(similarAlternatives, key = { it.id }) { item ->
          PlatformResultCard(
            item = item,
            isWishlisted = state.wishlist.any { it.productName.equals(item.productTitle, ignoreCase = true) },
            onOpenDetail = { onOpenProductDetail(item) },
            onCompare = { onAddToCompare(item) },
            onWishlist = { onToggleWishlist(item) },
            onOrder = { onOpenExternalUrl(item.externalUrl) },
          )
        }
      }

      // External Platform Search Links (Honest No-Live-API section)
      if (externalLinks.isNotEmpty()) {
        item {
          Text(
            text = "External Platform Search Links (${externalLinks.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TealCyan,
          )
          Text(
            text = "Live API pricing is not available for these platforms. Tap to search live prices directly on the platform.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        items(externalLinks, key = { it.id }) { item ->
          PlatformResultCard(
            item = item,
            isWishlisted = false,
            onOpenDetail = { onOpenProductDetail(item) },
            onCompare = { onAddToCompare(item) },
            onWishlist = { onToggleWishlist(item) },
            onOrder = { onOpenExternalUrl(item.externalUrl) },
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlatformResultCard(
  item: PlatformSearchResult,
  isWishlisted: Boolean,
  onOpenDetail: () -> Unit,
  onCompare: () -> Unit,
  onWishlist: () -> Unit,
  onOrder: () -> Unit,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border =
      BorderStroke(
        1.dp,
        when {
          item.isExternalSearchOnly -> MaterialTheme.colorScheme.outline
          item.matchType == MatchType.EXACT -> TealCyan.copy(alpha = 0.55f)
          else -> OrangeAccent.copy(alpha = 0.5f)
        },
      ),
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenDetail).testTag("result_card_${item.platform.name}"),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
          SpecBadge(item.platform.displayName, MaterialTheme.colorScheme.primary)
          SpecBadge(
            text = if (item.isDemoData) stringResource(R.string.demo_data_badge) else stringResource(R.string.external_link_badge),
            color = if (item.isDemoData) LavenderAccent else TealCyan,
          )
          if (!item.isExternalSearchOnly) {
            SpecBadge(
              text = "${item.matchType.label} (${item.confidenceScore.toInt()}%)",
              color = if (item.matchType == MatchType.EXACT) LimeNeon else OrangeAccent,
            )
          }
        }
        if (!item.isExternalSearchOnly) {
          IconButton(onClick = onWishlist, modifier = Modifier.size(36.dp)) {
            Icon(
              imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
              contentDescription = "Save to wishlist",
              tint = if (isWishlisted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      Text(
        text = item.productTitle,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
      )

      if (item.isExternalSearchOnly) {
        Text(
          text = item.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "${item.deliveryEstimate} · Updated ${item.lastUpdated}",
          style = MaterialTheme.typography.labelSmall,
          color = TealCyan,
        )
        Button(
          onClick = onOrder,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Search on ${item.platform.displayName}")
        }
      } else {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (item.material.isNotBlank()) SpecBadge(item.material, LavenderAccent)
          if (item.sizeOrCapacity.isNotBlank()) SpecBadge(item.sizeOrCapacity, TealCyan)
          if (item.colour.isNotBlank()) SpecBadge(item.colour, MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Bottom,
        ) {
          Column {
            Text(
              text =
                if (item.finalCost != null) "Final Cost: ₹${formatInr(item.finalCost)}"
                else "Listed: ₹${formatInr(item.productPrice)} (Final cost incomplete)",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              color = if (item.isFeesComplete) LimeNeon else OrangeAccent,
            )
            Text(
              text =
                if (item.isFeesComplete) {
                  "Price ₹${formatInr(item.productPrice)} · Delivery ₹${formatInr(item.deliveryFee)} · Fee ₹${formatInr(item.platformFee)} · Disc -₹${formatInr(item.discountAmount)}"
                } else {
                  "Delivery/platform fees unknown — verify checkout price on ${item.platform.displayName}"
                },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(
            imageVector = Icons.Default.LocalShipping,
            contentDescription = null,
            tint = if (item.deliveryVerifiedForPin) TealCyan else OrangeAccent,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = "${item.deliveryEstimate} · ${item.sourceLabel} · ${item.lastUpdated}",
            style = MaterialTheme.typography.labelSmall,
            color = if (item.deliveryVerifiedForPin) TealCyan else OrangeAccent,
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          Button(
            onClick = onOrder,
            modifier = Modifier.weight(1f),
          ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Order", fontWeight = FontWeight.Bold)
          }
          OutlinedButton(onClick = onOpenDetail) {
            Text("Details")
          }
          OutlinedButton(onClick = onCompare) {
            Icon(Icons.Default.CompareArrows, contentDescription = "Compare", modifier = Modifier.size(16.dp))
          }
        }
      }
    }
  }
}

@Composable
fun SpecBadge(text: String, color: androidx.compose.ui.graphics.Color) {
  Surface(
    color = color.copy(alpha = 0.14f),
    shape = RoundedCornerShape(6.dp),
    border = BorderStroke(0.8.dp, color.copy(alpha = 0.45f)),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      color = color,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
    )
  }
}

fun formatInr(value: Double?): String {
  if (value == null) return "N/A"
  return if (value % 1.0 == 0.0) String.format(Locale.US, "%,d", value.toLong())
  else String.format(Locale.US, "%,.2f", value)
}
