package com.example.domain

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class ShoppingCategory(val label: String) {
  GROCERIES("Groceries"),
  ELECTRONICS("Electronics"),
  FASHION("Fashion"),
  HOME_KITCHEN("Home & Kitchen"),
  BEAUTY("Beauty"),
  SPORTS("Sports"),
  FOOTWEAR("Footwear"),
  PERSONAL_CARE("Personal Care"),
  BOOKS_STATIONERY("Books & Stationery"),
  HOUSEHOLD("Household"),
  OTHER("Other Products");

  companion object {
    fun fromLabel(raw: String): ShoppingCategory {
      val norm = raw.trim().lowercase()
      return entries.firstOrNull { it.label.lowercase() == norm }
        ?: when {
          norm.contains("grocer") || norm.contains("food") || norm.contains("fruit") || norm.contains("dairy") -> GROCERIES
          norm.contains("electr") || norm.contains("phone") || norm.contains("laptop") || norm.contains("tv") || norm.contains("audio") || norm.contains("headphone") || norm.contains("watch") -> ELECTRONICS
          norm.contains("foot") || norm.contains("shoe") || norm.contains("sneaker") -> FOOTWEAR
          norm.contains("fashion") || norm.contains("cloth") || norm.contains("apparel") || norm.contains("shirt") -> FASHION
          norm.contains("home") || norm.contains("kitchen") || norm.contains("bottle") -> HOME_KITCHEN
          norm.contains("beauty") || norm.contains("cosmetic") -> BEAUTY
          norm.contains("sport") || norm.contains("fitness") -> SPORTS
          norm.contains("personal") || norm.contains("hygiene") -> PERSONAL_CARE
          norm.contains("book") || norm.contains("stationery") -> BOOKS_STATIONERY
          norm.contains("house") || norm.contains("clean") -> HOUSEHOLD
          else -> OTHER
        }
    }
  }
}

enum class ShoppingPlatform(
  val displayName: String,
  val isQuickCommerce: Boolean,
  val supportedCategories: Set<ShoppingCategory>,
) {
  AMAZON(
    "Amazon India",
    false,
    setOf(
      ShoppingCategory.ELECTRONICS,
      ShoppingCategory.HOME_KITCHEN,
      ShoppingCategory.FASHION,
      ShoppingCategory.FOOTWEAR,
      ShoppingCategory.BEAUTY,
      ShoppingCategory.SPORTS,
      ShoppingCategory.PERSONAL_CARE,
      ShoppingCategory.BOOKS_STATIONERY,
      ShoppingCategory.HOUSEHOLD,
      ShoppingCategory.GROCERIES,
      ShoppingCategory.OTHER,
    ),
  ),
  FLIPKART(
    "Flipkart",
    false,
    setOf(
      ShoppingCategory.ELECTRONICS,
      ShoppingCategory.HOME_KITCHEN,
      ShoppingCategory.FASHION,
      ShoppingCategory.FOOTWEAR,
      ShoppingCategory.BEAUTY,
      ShoppingCategory.SPORTS,
      ShoppingCategory.PERSONAL_CARE,
      ShoppingCategory.BOOKS_STATIONERY,
      ShoppingCategory.HOUSEHOLD,
      ShoppingCategory.GROCERIES,
      ShoppingCategory.OTHER,
    ),
  ),
  MYNTRA(
    "Myntra",
    false,
    setOf(ShoppingCategory.FASHION, ShoppingCategory.FOOTWEAR, ShoppingCategory.BEAUTY, ShoppingCategory.PERSONAL_CARE),
  ),
  AJIO(
    "AJIO",
    false,
    setOf(ShoppingCategory.FASHION, ShoppingCategory.FOOTWEAR),
  ),
  SHOPSY(
    "Shopsy",
    false,
    setOf(ShoppingCategory.FASHION, ShoppingCategory.FOOTWEAR, ShoppingCategory.HOME_KITCHEN, ShoppingCategory.HOUSEHOLD),
  ),
  BLINKIT(
    "Blinkit",
    true,
    setOf(ShoppingCategory.GROCERIES, ShoppingCategory.PERSONAL_CARE, ShoppingCategory.HOUSEHOLD, ShoppingCategory.HOME_KITCHEN, ShoppingCategory.BEAUTY),
  ),
  SWIGGY_INSTAMART(
    "Swiggy Instamart",
    true,
    setOf(ShoppingCategory.GROCERIES, ShoppingCategory.PERSONAL_CARE, ShoppingCategory.HOUSEHOLD, ShoppingCategory.HOME_KITCHEN),
  ),
  ZEPTO(
    "Zepto",
    true,
    setOf(ShoppingCategory.GROCERIES, ShoppingCategory.PERSONAL_CARE, ShoppingCategory.HOUSEHOLD, ShoppingCategory.ELECTRONICS),
  ),
  BIGBASKET(
    "BigBasket",
    true,
    setOf(ShoppingCategory.GROCERIES, ShoppingCategory.HOUSEHOLD, ShoppingCategory.PERSONAL_CARE, ShoppingCategory.HOME_KITCHEN),
  ),
  JIOMART(
    "JioMart",
    false,
    setOf(ShoppingCategory.GROCERIES, ShoppingCategory.HOME_KITCHEN, ShoppingCategory.ELECTRONICS, ShoppingCategory.HOUSEHOLD, ShoppingCategory.FASHION),
  ),
  FLIPKART_MINUTES(
    "Flipkart Minutes",
    true,
    setOf(ShoppingCategory.GROCERIES, ShoppingCategory.ELECTRONICS, ShoppingCategory.PERSONAL_CARE, ShoppingCategory.HOUSEHOLD),
  ),
  BUYHATKE(
    "Buyhatke",
    false,
    ShoppingCategory.entries.toSet(),
  );

  fun buildSearchUrl(query: String): String {
    val q = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.toString())
    return when (this) {
      AMAZON -> "https://www.amazon.in/s?k=$q"
      FLIPKART -> "https://www.flipkart.com/search?q=$q"
      MYNTRA -> "https://www.myntra.com/${query.trim().replace(Regex("\\s+"), "-").lowercase()}"
      AJIO -> "https://www.ajio.com/search/?text=$q"
      SHOPSY -> "https://www.shopsy.in/search?q=$q"
      BLINKIT -> "https://blinkit.com/s/?q=$q"
      SWIGGY_INSTAMART -> "https://www.swiggy.com/instamart/search?query=$q"
      ZEPTO -> "https://www.zeptonow.com/search?query=$q"
      BIGBASKET -> "https://www.bigbasket.com/ps/?q=$q"
      JIOMART -> "https://www.jiomart.com/search/$q"
      FLIPKART_MINUTES -> "https://www.flipkart.com/search?q=$q"
      BUYHATKE -> "https://buyhatke.com/"
    }
  }
}

enum class SearchSortOption(val label: String) {
  LOWEST_FINAL_PRICE("Lowest Final Price"),
  HIGHEST_PRICE("Highest Price"),
  FASTEST_DELIVERY("Fastest Delivery"),
  BEST_MATCH("Best Match"),
}

enum class MatchType(val label: String) {
  EXACT("Exact Match"),
  SIMILAR("Similar Alternative"),
  NONE("No Match"),
}

data class ParsedSearchQuery(
  val rawQuery: String,
  val productName: String,
  val brand: String = "",
  val category: ShoppingCategory = ShoppingCategory.OTHER,
  val material: String = "",
  val size: String = "",
  val capacity: String = "",
  val weight: String = "",
  val colour: String = "",
  val model: String = "",
  val quantity: Double = 1.0,
  val unit: String = "unit",
  val budgetInr: Double? = null,
  val requiredSpecs: List<String> = emptyList(),
  val parsedByGemini: Boolean = false,
)

data class PlatformSearchResult(
  val id: String,
  val platform: ShoppingPlatform,
  val productTitle: String,
  val brand: String,
  val category: ShoppingCategory,
  val material: String,
  val sizeOrCapacity: String,
  val colour: String,
  val description: String,
  val variants: List<String>,
  val pros: List<String>,
  val cons: List<String>,
  val matchType: MatchType,
  val confidenceScore: Double,
  val productPrice: Double?,
  val mrp: Double?,
  val deliveryFee: Double?,
  val platformFee: Double?,
  val discountAmount: Double,
  val finalCost: Double?,
  val isFeesComplete: Boolean,
  val isDemoData: Boolean,
  val isExternalSearchOnly: Boolean,
  val inStock: Boolean,
  val deliveryVerifiedForPin: Boolean,
  val deliveryEstimate: String,
  val deliveryMinutes: Int?,
  val sourceLabel: String,
  val lastUpdated: String,
  val externalUrl: String,
) {
  val isVerifiedCheapestEligible: Boolean
    get() = !isExternalSearchOnly && isFeesComplete && inStock && deliveryVerifiedForPin && finalCost != null
}

data class ProductUsageInfo(
  val title: String,
  val summary: String,
  val commonUses: List<String>,
  val whoShouldBuy: String,
  val keyFeatures: List<String>,
  val advantages: List<String>,
  val limitations: List<String>,
  val buyingConsiderations: List<String>,
  val generatedByGemini: Boolean,
)

data class DeliveryAddress(
  val id: String,
  val label: String,
  val houseNo: String,
  val street: String,
  val area: String,
  val city: String,
  val state: String,
  val pinCode: String,
  val landmark: String = "",
  val isDefault: Boolean = false,
) {
  val shortHeaderText: String
    get() = "$city – $pinCode"

  val fullFormatted: String
    get() =
      listOf(houseNo, street, area, city, state, pinCode)
        .filter { it.isNotBlank() }
        .joinToString(", ")
}

data class RawProductInput(
  val productName: String,
  val brand: String = "",
  val category: String = "general",
  val model: String = "",
  val size: String = "",
  val colour: String = "",
  val quantity: Double = 1.0,
  val unit: String = "unit",
  val budgetRaw: String = "",
  val dataLabel: String = "[DEMO DATA]",
)

data class NormalizedProduct(
  val productName: String,
  val normalizedName: String,
  val brand: String,
  val category: String,
  val model: String,
  val size: String,
  val colour: String,
  val quantity: Double,
  val unit: String,
  val canonicalQuantity: Double,
  val canonicalUnit: String,
  val budget: Double?,
  val dataLabel: String,
)

data class MatchResult(
  val queryProduct: String,
  val candidateProduct: String,
  val platform: String,
  val confidenceScore: Double,
  val matchType: MatchType,
  val attributeScores: Map<String, Double>,
  val dataLabel: String = "[DEMO DATA]",
)

data class PlatformOffer(
  val productName: String,
  val platform: String,
  val mrp: Double,
  val listedPrice: Double,
  val promoDiscount: Double = 0.0,
  val deliveryCharge: Double = 0.0,
  val platformCharge: Double = 0.0,
  val inStock: Boolean = true,
  val deliveryTimeMins: Int? = null,
  val dataLabel: String = "[DEMO DATA]",
)

data class EvaluatedOffer(
  val offer: PlatformOffer,
  val discountPct: Double,
  val finalPayablePrice: Double,
)

data class PriceComparisonReport(
  val lowestPrice: Double?,
  val highestPrice: Double?,
  val averagePrice: Double?,
  val priceDifference: Double,
  val bestPlatform: String?,
  val rankedOffers: List<EvaluatedOffer>,
)

data class BuyhatkeObservation(
  val category: String,
  val productName: String,
  val productUrl: String,
  val platform: String,
  val currentPrice: Double,
  val historicalLowestPrice: Double?,
  val historicalAveragePrice: Double?,
  val historicalHighestPrice: Double?,
  val observationDate: String,
  val source: String,
)

data class BuyhatkeCategoryAnalysis(
  val category: String,
  val productName: String,
  val platform: String,
  val observations: List<BuyhatkeObservation>,
  val currentPrice: Double,
  val observedMin: Double,
  val observedAvg: Double,
  val observedMax: Double,
  val pctAboveHistLow: Double?,
  val volatilityStd: Double,
  val volatilityCvPct: Double,
  val trendSlope: Double,
  val priceTrend: String,
  val dealRecommendation: String,
)

data class GroceryBasketItem(
  val id: String,
  val itemName: String,
  val quantity: Double,
  val unit: String,
)

data class GroceryStoreListing(
  val platform: String,
  val itemName: String,
  val packQuantity: Double,
  val packUnit: String,
  val packPrice: Double,
  val inStock: Boolean = true,
)

data class GroceryStoreMeta(
  val platform: String,
  val deliveryCharge: Double,
  val freeDeliveryThreshold: Double?,
  val platformFee: Double,
  val basketDiscount: Double,
  val deliveryTimeMins: Int,
)

data class GroceryLineEvaluation(
  val itemName: String,
  val requestedDisplay: String,
  val normalizedDisplay: String,
  val available: Boolean,
  val packsNeeded: Double?,
  val lineCost: Double?,
)

data class BasketEvaluation(
  val platform: String,
  val isCompleteBasket: Boolean,
  val availableItems: Int,
  val totalRequestedItems: Int,
  val missingItems: List<String>,
  val itemSubtotal: Double,
  val deliveryCharge: Double,
  val platformFee: Double,
  val basketDiscount: Double,
  val finalPayableCost: Double,
  val deliveryTimeMins: Int,
  val lineItems: List<GroceryLineEvaluation>,
)

data class RecommendationResult(
  val recommendedComplete: BasketEvaluation?,
  val completeRanked: List<BasketEvaluation>,
  val incompleteRanked: List<BasketEvaluation>,
)

data class EngineTestResult(
  val testName: String,
  val passed: Boolean,
  val details: String,
)
