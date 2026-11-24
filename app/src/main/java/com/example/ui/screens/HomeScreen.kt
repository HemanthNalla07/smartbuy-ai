package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.ShoppingCategory
import com.example.ui.SmartBuyUiState
import com.example.ui.components.SmartBuyTopHeader
import com.example.ui.theme.LavenderAccent
import com.example.ui.theme.LimeNeon
import com.example.ui.theme.MagentaAccent
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.TealCyan

private val exampleSearches =
  listOf(
    "1 litre stainless steel water bottle under ₹500",
    "1 kg bananas",
    "iPhone 16",
    "Nike shoes size 9",
    "milk 1 litre",
  )

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
  state: SmartBuyUiState,
  onQueryChange: (String) -> Unit,
  onClearQuery: () -> Unit,
  onSubmitSearch: (String) -> Unit,
  onVoiceSearchClick: () -> Unit,
  onSelectCategory: (ShoppingCategory) -> Unit,
  onChangeLocation: () -> Unit,
  onOpenCart: () -> Unit,
  onOpenAccount: () -> Unit,
  onOpenBuyhatke: () -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize()) {
    SmartBuyTopHeader(
      activeAddress = state.activeAddress,
      cartCount = state.basketItems.size,
      isSignedIn = state.isSignedIn,
      onChangeLocation = onChangeLocation,
      onOpenCart = onOpenCart,
      onOpenAccount = onOpenAccount,
    )

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      // Search Must Be First
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = "Compare Prices Across 12 Indian Platforms",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "Search any product or grocery item to find the lowest verified final checkout cost.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
              value = state.searchQuery,
              onValueChange = onQueryChange,
              placeholder = {
                Text(
                  text = stringResource(R.string.search_hint),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Search,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
              },
              trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  if (state.searchQuery.isNotEmpty()) {
                    IconButton(
                      onClick = onClearQuery,
                      modifier = Modifier.testTag("home_clear_search"),
                    ) {
                      Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                  }
                  IconButton(
                    onClick = onVoiceSearchClick,
                    modifier = Modifier.testTag("voice_search_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Default.Mic,
                      contentDescription = "Voice search",
                      tint = MaterialTheme.colorScheme.secondary,
                    )
                  }
                }
              },
              singleLine = true,
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
              keyboardActions = KeyboardActions(onSearch = { onSubmitSearch(state.searchQuery) }),
              colors =
                OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = MaterialTheme.colorScheme.primary,
                  unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                  focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                  unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().testTag("home_search_input"),
            )

            Button(
              onClick = { onSubmitSearch(state.searchQuery) },
              enabled = state.searchQuery.trim().isNotEmpty(),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("home_search_submit"),
            ) {
              Icon(Icons.Default.Search, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Search & Compare Prices", fontWeight = FontWeight.Bold)
            }

            Text(
              text = "Try searching:",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(exampleSearches) { sample ->
                AssistChip(
                  onClick = { onSubmitSearch(sample) },
                  label = { Text(sample, maxLines = 1) },
                  leadingIcon = {
                    Icon(
                      imageVector = Icons.Default.AutoAwesome,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(16.dp),
                    )
                  },
                )
              }
            }
          }
        }
      }

      // 11 Category Shortcuts
      item {
        Text(
          text = "Shop by Category",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ShoppingCategory.entries.forEachIndexed { idx, cat ->
            val accent =
              when (idx % 5) {
                0 -> MaterialTheme.colorScheme.primary
                1 -> TealCyan
                2 -> LimeNeon
                3 -> OrangeAccent
                4 -> MagentaAccent
                else -> LavenderAccent
              }
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              modifier =
                Modifier.clickable { onSelectCategory(cat) }
                  .testTag("category_chip_${cat.name}"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = iconForCategory(cat),
                  contentDescription = cat.label,
                  tint = accent,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = cat.label,
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }
        }
      }

      // Recent Searches (if user has searched)
      if (state.searchHistory.isNotEmpty()) {
        item {
          Text(
            text = "Recent Searches",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(8.dp))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.searchHistory.take(8)) { rec ->
              AssistChip(
                onClick = { onSubmitSearch(rec.queryText) },
                label = { Text(rec.queryText, maxLines = 1) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = TealCyan,
                    modifier = Modifier.size(16.dp),
                  )
                },
              )
            }
          }
        }
      }

      // Popular Categories & Quick Tools Footer Card
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Image(
              painter = painterResource(R.drawable.img_smartbuy_hero_1795449226685),
              contentDescription = "SmartBuy shopping tools",
              contentScale = ContentScale.Crop,
              modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "Smart Cart & Buyhatke Tracker",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Optimize grocery baskets or analyze historical prices.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                  onClick = onOpenCart,
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                ) {
                  Text("Smart Cart", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                  onClick = onOpenBuyhatke,
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                ) {
                  Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Price History", style = MaterialTheme.typography.labelMedium)
                }
              }
            }
          }
        }
      }
    }
  }
}

private fun iconForCategory(category: ShoppingCategory): ImageVector =
  when (category) {
    ShoppingCategory.GROCERIES -> Icons.Default.LocalGroceryStore
    ShoppingCategory.ELECTRONICS -> Icons.Default.Devices
    ShoppingCategory.FASHION -> Icons.Default.Checkroom
    ShoppingCategory.HOME_KITCHEN -> Icons.Default.Kitchen
    ShoppingCategory.BEAUTY -> Icons.Default.FaceRetouchingNatural
    ShoppingCategory.SPORTS -> Icons.Default.FitnessCenter
    ShoppingCategory.FOOTWEAR -> Icons.Default.DirectionsRun
    ShoppingCategory.PERSONAL_CARE -> Icons.Default.Spa
    ShoppingCategory.BOOKS_STATIONERY -> Icons.AutoMirrored.Filled.MenuBook
    ShoppingCategory.HOUSEHOLD -> Icons.Default.Home
    ShoppingCategory.OTHER -> Icons.Default.ShoppingBag
  }
