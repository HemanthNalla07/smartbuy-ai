package com.example.data

import com.example.domain.MatchType
import com.example.domain.ParsedSearchQuery
import com.example.domain.PlatformSearchResult
import com.example.domain.ShoppingCategory
import com.example.domain.ShoppingPlatform
import com.example.domain.SmartBuyEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates platform search results for the active query & PIN code:
 * - Demo catalog rows are explicitly labeled `DEMO DATA (Sample Catalog)`.
 * - Platforms without an authorized live pricing API return honest `EXTERNAL PLATFORM SEARCH`
 *   links that open the actual platform search page with zero invented prices.
 */
object PlatformSearchCatalog {
  private data class DemoTemplate(
    val platform: ShoppingPlatform,
    val title: String,
    val brand: String,
    val category: ShoppingCategory,
    val material: String,
    val sizeOrCapacity: String,
    val colour: String,
    val description: String,
    val variants: List<String>,
    val pros: List<String>,
    val cons: List<String>,
    val price: Double,
    val mrp: Double,
    val deliveryFee: Double?,
    val platformFee: Double?,
    val discount: Double,
    val deliveryMinutes: Int?,
    val inStock: Boolean = true,
  )

  private val demoTemplates =
    listOf(
      // Groceries: Bananas
      DemoTemplate(
        ShoppingPlatform.BLINKIT, "Fresh Yelakki / Robusta Bananas (1 kg)", "Fresh Farm", ShoppingCategory.GROCERIES,
        "Fresh Produce", "1 kg", "Yellow", "Hand-picked fresh bananas packed in breathable food-grade bag.",
        listOf("500 g", "1 kg", "2 kg"), listOf("Quick delivery in serviceable metros", "No middlemen markup"),
        listOf("Perishable — consume within 3 days"), 58.0, 72.0, 15.0, 4.0, 5.0, 12,
      ),
      DemoTemplate(
        ShoppingPlatform.ZEPTO, "Robusta Bananas (1 kg)", "Zepto Fresh", ShoppingCategory.GROCERIES,
        "Fresh Produce", "1 kg", "Yellow", "Naturally ripened bananas sourced daily from local farms.",
        listOf("500 g", "1 kg"), listOf("Low handling fee", "Fast doorstep delivery"),
        listOf("Limited availability in non-metro PINs"), 56.0, 70.0, 20.0, 2.0, 0.0, 14,
      ),
      DemoTemplate(
        ShoppingPlatform.SWIGGY_INSTAMART, "Fresh Bananas (1 kg)", "Instamart", ShoppingCategory.GROCERIES,
        "Fresh Produce", "1 kg", "Yellow", "Daily harvest bananas quality-checked at dark stores.",
        listOf("500 g", "1 kg"), listOf("Consistent grading"), listOf("Higher delivery fee below ₹199"),
        60.0, 75.0, 25.0, 5.0, 5.0, 16,
      ),
      DemoTemplate(
        ShoppingPlatform.BIGBASKET, "Fresho Banana - Robusta (1 kg)", "Fresho", ShoppingCategory.GROCERIES,
        "Fresh Produce", "1 kg", "Yellow", "Quality-graded bananas packed carefully for scheduled delivery.",
        listOf("500 g", "1 kg", "2 kg"), listOf("Reliable stock availability"), listOf("Scheduled slot delivery"),
        54.0, 68.0, 0.0, 0.0, 0.0, 120,
      ),
      DemoTemplate(
        ShoppingPlatform.JIOMART, "Fresh Banana Robusta 1 kg", "JioMart Fresh", ShoppingCategory.GROCERIES,
        "Fresh Produce", "1 kg", "Yellow", "Everyday fresh produce delivered via nearest fulfillment hub.",
        listOf("1 kg"), listOf("Zero platform fee"), listOf("Longer delivery window"),
        52.0, 66.0, null, null, 0.0, null,
      ),
      // Groceries: Milk
      DemoTemplate(
        ShoppingPlatform.BLINKIT, "Nandini / Amul Toned Milk (1 Litre)", "Amul", ShoppingCategory.GROCERIES,
        "Dairy", "1 litre", "White", "Pasteurized toned milk pouch, chilled cold-chain delivery.",
        listOf("500 ml", "1 litre"), listOf("Chilled cold-chain"), listOf("Short shelf life"),
        56.0, 58.0, 15.0, 4.0, 0.0, 11,
      ),
      DemoTemplate(
        ShoppingPlatform.ZEPTO, "Amul Taaza Toned Milk (1 Litre)", "Amul", ShoppingCategory.GROCERIES,
        "Dairy", "1 litre", "White", "UHT / fresh toned milk suitable for tea, coffee, and daily cooking.",
        listOf("500 ml", "1 litre"), listOf("Lowest quick-commerce fee"), listOf("Metro PINs only"),
        55.0, 58.0, 15.0, 2.0, 0.0, 13,
      ),
      // Home & Kitchen: 1L Stainless Steel Water Bottle (plus a plastic alternative to test exact vs similar separation!)
      DemoTemplate(
        ShoppingPlatform.FLIPKART, "Milton Thermosteel 1 Litre Stainless Steel Water Bottle", "Milton", ShoppingCategory.HOME_KITCHEN,
        "Stainless Steel", "1 litre", "Silver", "18/8 rust-proof food-grade stainless steel vacuum bottle, keeps beverages hot/cold.",
        listOf("500 ml", "750 ml", "1 litre"), listOf("304 food-grade stainless steel", "Leak-proof cap"),
        listOf("Hand wash recommended"), 349.0, 599.0, 30.0, 5.0, 0.0, 2880,
      ),
      DemoTemplate(
        ShoppingPlatform.AMAZON, "Cello Swift 1 Litre Stainless Steel Water Bottle", "Cello", ShoppingCategory.HOME_KITCHEN,
        "Stainless Steel", "1 litre", "Black", "Single-wall durable stainless steel fridge & office bottle (BPA-free).",
        listOf("750 ml", "1 litre"), listOf("Lightweight stainless steel", "Rust-resistant"),
        listOf("Not vacuum insulated"), 399.0, 549.0, 40.0, 0.0, 0.0, 1440,
      ),
      DemoTemplate(
        ShoppingPlatform.AMAZON, "Signoraware 1 Litre BPA-Free Plastic Water Bottle", "Signoraware", ShoppingCategory.HOME_KITCHEN,
        "Plastic", "1 litre", "Blue", "Lightweight virgin BPA-free plastic sports & fridge bottle.",
        listOf("1 litre"), listOf("Very lightweight", "Budget-friendly"),
        listOf("Plastic material (not stainless steel)"), 219.0, 320.0, 40.0, 0.0, 0.0, 1440,
      ),
      // Electronics: iPhone 16
      DemoTemplate(
        ShoppingPlatform.FLIPKART, "Apple iPhone 16 (128 GB, Ultramarine)", "Apple", ShoppingCategory.ELECTRONICS,
        "Aluminium & Glass", "128GB", "Ultramarine", "A18 chip, 48MP Fusion camera, Camera Control, Super Retina XDR display.",
        listOf("128GB", "256GB", "512GB"), listOf("A18 chip performance", "Long iOS software support"),
        listOf("60Hz display refresh rate"), 74900.0, 79900.0, 0.0, 99.0, 2000.0, 1440,
      ),
      DemoTemplate(
        ShoppingPlatform.AMAZON, "Apple iPhone 16 (128 GB) - Black", "Apple", ShoppingCategory.ELECTRONICS,
        "Aluminium & Glass", "128GB", "Black", "Apple iPhone 16 with A18 chip and USB-C connectivity.",
        listOf("128GB", "256GB"), listOf("No delivery fee", "Wide bank EMI support"),
        listOf("Charger sold separately"), 75900.0, 79900.0, 0.0, 0.0, 1500.0, 1440,
      ),
      // Footwear: Nike Shoes Size 9
      DemoTemplate(
        ShoppingPlatform.MYNTRA, "Nike Revolution 7 Running Shoes (UK 9)", "Nike", ShoppingCategory.FOOTWEAR,
        "Breathable Mesh", "UK 9", "Black/White", "Soft foam midsole cushioning with durable traction outsole.",
        listOf("UK 7", "UK 8", "UK 9", "UK 10"), listOf("Breathable mesh upper", "Easy 14-day size exchange"),
        listOf("Platform convenience fee applies"), 3195.0, 3695.0, 0.0, 20.0, 200.0, 2880,
      ),
      DemoTemplate(
        ShoppingPlatform.AJIO, "Nike Downshifter 13 Running Shoes (UK 9)", "Nike", ShoppingCategory.FOOTWEAR,
        "Mesh & Rubber", "UK 9", "Slate", "Supportive fit band for road running and daily gym sessions.",
        listOf("UK 8", "UK 9", "UK 10"), listOf("Good arch support"), listOf("Slightly narrow toe box"),
        3495.0, 4295.0, 0.0, 29.0, 300.0, 4320,
      ),
      DemoTemplate(
        ShoppingPlatform.FLIPKART, "Nike Court Vision Low Sneakers (UK 9)", "Nike", ShoppingCategory.FOOTWEAR,
        "Synthetic Leather", "UK 9", "White", "Classic 80s basketball-inspired low-top silhouette.",
        listOf("UK 8", "UK 9", "UK 10"), listOf("Versatile casual styling"), listOf("Heavier than mesh runners"),
        3895.0, 4995.0, 0.0, 49.0, 0.0, 2880,
      ),
    )

  fun searchAcrossPlatforms(
    parsed: ParsedSearchQuery,
    pinCode: String,
  ): List<PlatformSearchResult> {
    val nowLabel = SimpleDateFormat("dd MMM, HH:mm", Locale.US).format(Date())
    val qcServiceable = LocationHelper.isQuickCommerceServiceable(pinCode)
    val relevantPlatforms =
      ShoppingPlatform.entries.filter {
        it != ShoppingPlatform.BUYHATKE && parsed.category in it.supportedCategories
      }

    val results = mutableListOf<PlatformSearchResult>()

    // 1. Score matching demo catalog entries (explicitly labeled DEMO DATA)
    for (tpl in demoTemplates) {
      if (tpl.platform !in relevantPlatforms) continue
      val textOverlap = SmartBuyEngine.tokenSetRatio(parsed.productName, tpl.title)
      val rawOverlap = SmartBuyEngine.tokenSetRatio(parsed.rawQuery, tpl.title)
      val bestTextScore = maxOf(textOverlap, rawOverlap)
      if (bestTextScore < 38.0) continue

      val materialConflict =
        parsed.material.isNotBlank() &&
          tpl.material.isNotBlank() &&
          !tpl.material.contains(parsed.material, ignoreCase = true) &&
          !parsed.material.contains(tpl.material, ignoreCase = true)

      val capacityConflict =
        parsed.capacity.isNotBlank() &&
          tpl.sizeOrCapacity.isNotBlank() &&
          !tpl.sizeOrCapacity.contains(parsed.capacity, ignoreCase = true)

      val sizeConflict =
        parsed.size.isNotBlank() &&
          tpl.sizeOrCapacity.isNotBlank() &&
          !tpl.sizeOrCapacity.contains(parsed.size, ignoreCase = true) &&
          !tpl.sizeOrCapacity.contains(parsed.size.replace("size ", "", ignoreCase = true), ignoreCase = true)

      val brandConflict =
        parsed.brand.isNotBlank() &&
          !tpl.brand.equals(parsed.brand, ignoreCase = true)

      val matchType =
        if (!materialConflict && !capacityConflict && !sizeConflict && !brandConflict && bestTextScore >= 52.0) {
          MatchType.EXACT
        } else {
          MatchType.SIMILAR
        }

      val feesComplete = tpl.deliveryFee != null && tpl.platformFee != null
      val finalCost =
        if (feesComplete) {
          (tpl.price - tpl.discount).coerceAtLeast(0.0) + (tpl.deliveryFee ?: 0.0) + (tpl.platformFee ?: 0.0)
        } else null

      val deliveryVerified = if (tpl.platform.isQuickCommerce) qcServiceable else LocationHelper.isValidIndianPinCode(pinCode)
      val deliveryEstimate =
        when {
          tpl.platform.isQuickCommerce && !qcServiceable ->
            "Unverified / Not serviceable at PIN $pinCode"
          tpl.deliveryMinutes == null ->
            "Delivery estimate unavailable (verify on platform)"
          tpl.deliveryMinutes < 60 ->
            "Verified ~${tpl.deliveryMinutes} mins to $pinCode"
          else ->
            "Verified ~${tpl.deliveryMinutes / 1440} day(s) to $pinCode"
        }

      val penalty = if (materialConflict) 35.0 else if (sizeConflict || capacityConflict || brandConflict) 18.0 else 0.0
      val conf = (bestTextScore - penalty).coerceIn(25.0, 99.0)

      results.add(
        PlatformSearchResult(
          id = "${tpl.platform.name}_${tpl.title.hashCode()}",
          platform = tpl.platform,
          productTitle = tpl.title,
          brand = tpl.brand,
          category = tpl.category,
          material = tpl.material,
          sizeOrCapacity = tpl.sizeOrCapacity,
          colour = tpl.colour,
          description = tpl.description,
          variants = tpl.variants,
          pros = tpl.pros,
          cons = tpl.cons,
          matchType = matchType,
          confidenceScore = conf,
          productPrice = tpl.price,
          mrp = tpl.mrp,
          deliveryFee = tpl.deliveryFee,
          platformFee = tpl.platformFee,
          discountAmount = tpl.discount,
          finalCost = finalCost,
          isFeesComplete = feesComplete,
          isDemoData = true,
          isExternalSearchOnly = false,
          inStock = tpl.inStock && deliveryVerified,
          deliveryVerifiedForPin = deliveryVerified,
          deliveryEstimate = deliveryEstimate,
          deliveryMinutes = if (deliveryVerified) tpl.deliveryMinutes else null,
          sourceLabel = if (feesComplete) "DEMO DATA (Sample Catalog)" else "DEMO DATA (Incomplete Fees)",
          lastUpdated = nowLabel,
          externalUrl = tpl.platform.buildSearchUrl(parsed.rawQuery.ifBlank { tpl.title }),
        )
      )
    }

    // 2. For every relevant platform without live API data in this query, provide an honest External Search Link card
    val coveredPlatforms = results.map { it.platform }.toSet()
    for (platform in relevantPlatforms) {
      if (platform in coveredPlatforms) continue
      val deliveryVerified = if (platform.isQuickCommerce) qcServiceable else LocationHelper.isValidIndianPinCode(pinCode)
      val estText =
        if (platform.isQuickCommerce && !qcServiceable) "Quick-commerce unverified at PIN $pinCode"
        else "Live price & delivery available on ${platform.displayName}"
      results.add(
        PlatformSearchResult(
          id = "ext_${platform.name}_${parsed.rawQuery.hashCode()}",
          platform = platform,
          productTitle = "Search \"${parsed.rawQuery}\" on ${platform.displayName}",
          brand = parsed.brand.ifBlank { "Various" },
          category = parsed.category,
          material = parsed.material,
          sizeOrCapacity = parsed.capacity.ifBlank { parsed.size.ifBlank { parsed.weight } },
          colour = parsed.colour,
          description = "Live API integration is not authorized for ${platform.displayName}. Open the verified search page directly to view current stock, price, and checkout fees.",
          variants = emptyList(),
          pros = listOf("Opens official ${platform.displayName} search directly"),
          cons = listOf("Live price cannot be fetched automatically without platform API"),
          matchType = MatchType.SIMILAR,
          confidenceScore = 70.0,
          productPrice = null,
          mrp = null,
          deliveryFee = null,
          platformFee = null,
          discountAmount = 0.0,
          finalCost = null,
          isFeesComplete = false,
          isDemoData = false,
          isExternalSearchOnly = true,
          inStock = true,
          deliveryVerifiedForPin = deliveryVerified,
          deliveryEstimate = estText,
          deliveryMinutes = null,
          sourceLabel = "EXTERNAL PLATFORM SEARCH (No Live Price API)",
          lastUpdated = nowLabel,
          externalUrl = platform.buildSearchUrl(parsed.rawQuery),
        )
      )
    }

    return results
  }
}
