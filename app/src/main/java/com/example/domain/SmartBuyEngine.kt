package com.example.domain

import com.example.data.SampleTestDatasets
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import org.json.JSONArray
import org.json.JSONObject

object SmartBuyEngine {
  fun fallbackParseSearchQuery(rawQuery: String): ParsedSearchQuery {
    val q = cleanText(rawQuery)
    val lower = q.lowercase(Locale.US)

    val budgetMatch = Regex("(?:under|below|max|₹|rs\\.?|inr)\\s*([0-9,]+)").find(lower)
    val budget = budgetMatch?.groupValues?.getOrNull(1)?.replace(",", "")?.toDoubleOrNull()

    val material =
      when {
        lower.contains("stainless steel") || lower.contains("steel") -> "Stainless Steel"
        lower.contains("plastic") -> "Plastic"
        lower.contains("glass") -> "Glass"
        lower.contains("cotton") -> "Cotton"
        lower.contains("leather") -> "Leather"
        else -> ""
      }

    val capacityMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*(litre|litres|liter|liters|ltr|l|ml|gb|tb)\\b").find(lower)
    val capacity = capacityMatch?.value ?: ""

    val weightMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*(kg|g|gm|gms|grams|gram)\\b").find(lower)
    val weight = weightMatch?.value ?: ""

    val sizeMatch = Regex("(?:size\\s*|uk\\s*)(\\d+|[xsml]{1,3})\\b").find(lower)
    val size =
      when {
        lower.contains("size 9") || lower.contains("uk 9") -> "UK 9"
        sizeMatch != null -> sizeMatch.value.uppercase(Locale.US)
        else -> ""
      }

    val brand =
      listOf("Apple", "Samsung", "Sony", "Nike", "Adidas", "Milton", "Cello", "Amul", "Uniqlo", "Boat")
        .firstOrNull { lower.contains(it.lowercase(Locale.US)) } ?: ""

    val category =
      when {
        listOf("banana", "milk", "tomato", "apple", "bread", "rice", "oil", "atta", "egg", "dal").any { lower.contains(it) && !lower.contains("iphone") && !lower.contains("watch") } ->
          ShoppingCategory.GROCERIES
        listOf("iphone", "samsung", "tv", "television", "laptop", "headphone", "earbud", "watch", "phone", "washing machine").any { lower.contains(it) } ->
          ShoppingCategory.ELECTRONICS
        listOf("shoe", "sneaker", "sandal", "boot", "footwear").any { lower.contains(it) } ->
          ShoppingCategory.FOOTWEAR
        listOf("shirt", "t-shirt", "jeans", "kurta", "dress", "jacket").any { lower.contains(it) } ->
          ShoppingCategory.FASHION
        listOf("bottle", "flask", "pan", "cooker", "kettle", "container").any { lower.contains(it) } ->
          ShoppingCategory.HOME_KITCHEN
        listOf("shampoo", "soap", "cream", "lipstick", "serum", "face wash").any { lower.contains(it) } ->
          ShoppingCategory.BEAUTY
        listOf("book", "notebook", "pen", "diary").any { lower.contains(it) } ->
          ShoppingCategory.BOOKS_STATIONERY
        else -> ShoppingCategory.OTHER
      }

    val specs = listOfNotNull(
      material.takeIf { it.isNotBlank() },
      capacity.takeIf { it.isNotBlank() },
      weight.takeIf { it.isNotBlank() },
      size.takeIf { it.isNotBlank() },
      budget?.let { "Under ₹${it.toInt()}" },
    )

    val cleanedName =
      q.replace(Regex("(?:under|below)\\s*(?:₹|rs\\.?)?\\s*[0-9,]+", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s+"), " ")
        .trim()
        .ifBlank { q }

    return ParsedSearchQuery(
      rawQuery = q,
      productName = cleanedName,
      brand = brand,
      category = category,
      material = material,
      size = size,
      capacity = capacity,
      weight = weight,
      quantity = weightMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: capacityMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 1.0,
      unit = weightMatch?.groupValues?.getOrNull(2) ?: capacityMatch?.groupValues?.getOrNull(2) ?: "unit",
      budgetInr = budget,
      requiredSpecs = specs,
      parsedByGemini = false,
    )
  }

  fun sortAndFilterSearchResults(
    results: List<PlatformSearchResult>,
    sortOption: SearchSortOption,
    inStockOnly: Boolean,
    underBudgetOnly: Boolean,
    budget: Double?,
  ): List<PlatformSearchResult> {
    val filtered =
      results.filter { r ->
        (!inStockOnly || r.inStock) &&
          (!underBudgetOnly || budget == null || r.finalCost == null || r.finalCost <= budget)
      }
    return when (sortOption) {
      SearchSortOption.LOWEST_FINAL_PRICE ->
        filtered.sortedWith(
          compareBy<PlatformSearchResult> { it.isExternalSearchOnly }
            .thenBy { !it.isFeesComplete }
            .thenBy { it.finalCost ?: Double.MAX_VALUE }
            .thenBy { it.deliveryMinutes ?: Int.MAX_VALUE }
        )
      SearchSortOption.HIGHEST_PRICE ->
        filtered.sortedWith(
          compareBy<PlatformSearchResult> { it.isExternalSearchOnly }
            .thenByDescending { it.finalCost ?: it.productPrice ?: -1.0 }
        )
      SearchSortOption.FASTEST_DELIVERY ->
        filtered.sortedWith(
          compareBy<PlatformSearchResult> { !it.deliveryVerifiedForPin }
            .thenBy { it.deliveryMinutes ?: Int.MAX_VALUE }
            .thenBy { it.finalCost ?: Double.MAX_VALUE }
        )
      SearchSortOption.BEST_MATCH ->
        filtered.sortedWith(
          compareBy<PlatformSearchResult> { it.matchType != MatchType.EXACT }
            .thenByDescending { it.confidenceScore }
            .thenBy { it.finalCost ?: Double.MAX_VALUE }
        )
    }
  }
  private val unitFactors =
    mapOf(
      "kg" to ("kg" to 1.0),
      "kilogram" to ("kg" to 1.0),
      "kilograms" to ("kg" to 1.0),
      "g" to ("kg" to 0.001),
      "gm" to ("kg" to 0.001),
      "gms" to ("kg" to 0.001),
      "gram" to ("kg" to 0.001),
      "grams" to ("kg" to 0.001),
      "l" to ("l" to 1.0),
      "ltr" to ("l" to 1.0),
      "litre" to ("l" to 1.0),
      "litres" to ("l" to 1.0),
      "liter" to ("l" to 1.0),
      "liters" to ("l" to 1.0),
      "ml" to ("l" to 0.001),
      "millilitre" to ("l" to 0.001),
      "millilitres" to ("l" to 0.001),
      "unit" to ("unit" to 1.0),
      "units" to ("unit" to 1.0),
      "pc" to ("unit" to 1.0),
      "pcs" to ("unit" to 1.0),
      "piece" to ("unit" to 1.0),
      "pieces" to ("unit" to 1.0),
      "dozen" to ("unit" to 12.0),
      "pair" to ("pair" to 1.0),
      "pack" to ("pack" to 1.0),
      "packs" to ("pack" to 1.0),
      "packet" to ("pack" to 1.0),
      "packets" to ("pack" to 1.0),
      "pkt" to ("pack" to 1.0),
      "pkts" to ("pack" to 1.0),
    )

  private val colourAliases =
    mapOf(
      "blk" to "black",
      "midnight" to "black",
      "obsidian" to "black",
      "wht" to "white",
      "ivory" to "white",
      "slv" to "silver",
      "grey" to "gray",
      "space gray" to "gray",
      "navy blue" to "navy",
      "crimson" to "red",
    )

  private val categoryAliases =
    mapOf(
      "phone" to "smartphones",
      "mobile" to "smartphones",
      "smartphone" to "smartphones",
      "smartphones" to "smartphones",
      "shoe" to "shoes",
      "sneakers" to "shoes",
      "running shoes" to "shoes",
      "shoes" to "shoes",
      "apparel" to "clothing",
      "shirt" to "clothing",
      "t-shirt" to "clothing",
      "clothing" to "clothing",
      "earphones" to "headphones",
      "headset" to "headphones",
      "headphones" to "headphones",
      "watch" to "smartwatches",
      "smartwatch" to "smartwatches",
      "smartwatches" to "smartwatches",
      "grocery" to "groceries",
      "groceries" to "groceries",
    )

  fun cleanText(value: String?): String = value.orEmpty().trim().replace(Regex("\\s+"), " ")

  fun parsePriceValue(raw: String?): Double? {
    if (raw.isNullOrBlank()) return null
    val cleaned = raw.replace(",", "").replace(Regex("[^0-9.]"), "")
    val parsed = cleaned.toDoubleOrNull() ?: return null
    return if (parsed >= 0.0) parsed else null
  }

  data class NormalizedQty(
    val rawQty: Double,
    val rawUnit: String,
    val canonicalQty: Double,
    val canonicalUnit: String,
  )

  fun normalizeQuantityAndUnit(quantity: Double, unit: String): NormalizedQty {
    val qty = if (quantity > 0.0) quantity else 1.0
    val unitClean = cleanText(unit).lowercase(Locale.US).ifEmpty { "unit" }
    val (canonUnit, factor) = unitFactors[unitClean] ?: (unitClean to 1.0)
    return NormalizedQty(
      rawQty = round2(qty),
      rawUnit = unitClean,
      canonicalQty = ((qty * factor) * 1_000_000.0).roundToInt() / 1_000_000.0,
      canonicalUnit = canonUnit,
    )
  }

  // Module 1: Normalize product input
  fun normalizeProduct(raw: RawProductInput): NormalizedProduct {
    val nameClean = cleanText(raw.productName)
    val brandClean = cleanText(raw.brand).lowercase(Locale.US)
    val catRaw = cleanText(raw.category).lowercase(Locale.US)
    val catClean = categoryAliases[catRaw] ?: catRaw.ifEmpty { "general" }
    val modelClean = cleanText(raw.model).lowercase(Locale.US)
    val sizeClean = cleanText(raw.size).lowercase(Locale.US)
    val colRaw = cleanText(raw.colour).lowercase(Locale.US)
    val colClean = colourAliases[colRaw] ?: colRaw
    val qtyNorm = normalizeQuantityAndUnit(raw.quantity, raw.unit)
    val budget = parsePriceValue(raw.budgetRaw)

    val normTokens =
      listOf(brandClean, nameClean.lowercase(Locale.US), modelClean, sizeClean, colClean).filter {
        it.isNotEmpty()
      }
    val normName =
      normTokens.joinToString(" ").replace(Regex("[^a-z0-9\\s.-]"), "").replace(Regex("\\s+"), " ").trim()

    return NormalizedProduct(
      productName = nameClean,
      normalizedName = normName,
      brand = brandClean,
      category = catClean,
      model = modelClean,
      size = sizeClean,
      colour = colClean,
      quantity = qtyNorm.rawQty,
      unit = qtyNorm.rawUnit,
      canonicalQuantity = qtyNorm.canonicalQty,
      canonicalUnit = qtyNorm.canonicalUnit,
      budget = budget,
      dataLabel = raw.dataLabel,
    )
  }

  fun parseProductsFromJsonOrCsv(input: String): List<NormalizedProduct> {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return emptyList()
    return if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
      val arr = if (trimmed.startsWith("[")) JSONArray(trimmed) else JSONArray().put(JSONObject(trimmed))
      (0 until arr.length()).mapNotNull { idx ->
        val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
        val pname = obj.optString("product_name", obj.optString("productName", ""))
        if (pname.isBlank()) return@mapNotNull null
        normalizeProduct(
          RawProductInput(
            productName = pname,
            brand = obj.optString("brand", ""),
            category = obj.optString("category", "general"),
            model = obj.optString("model", ""),
            size = obj.optString("size", ""),
            colour = obj.optString("colour", ""),
            quantity = obj.optDouble("quantity", 1.0),
            unit = obj.optString("unit", "unit"),
            budgetRaw = obj.optString("budget", ""),
          )
        )
      }
    } else {
      // CSV format: product_name,brand,category,model,size,colour,quantity,unit,budget
      val lines = trimmed.lines().filter { it.isNotBlank() }
      if (lines.size < 2) return emptyList()
      val headers = lines.first().split(",").map { it.trim().lowercase(Locale.US) }
      lines.drop(1).mapNotNull { row ->
        val cols = row.split(",").map { it.trim() }
        val map = headers.zip(cols).toMap()
        val pname = map["product_name"] ?: return@mapNotNull null
        normalizeProduct(
          RawProductInput(
            productName = pname,
            brand = map["brand"].orEmpty(),
            category = map["category"] ?: "general",
            model = map["model"].orEmpty(),
            size = map["size"].orEmpty(),
            colour = map["colour"].orEmpty(),
            quantity = map["quantity"]?.toDoubleOrNull() ?: 1.0,
            unit = map["unit"] ?: "unit",
            budgetRaw = map["budget"].orEmpty(),
          )
        )
      }
    }
  }

  // Module 2: RapidFuzz-equivalent Token Sort + Token Set Matching
  fun tokenSortRatio(a: String, b: String): Double {
    val ta = a.lowercase(Locale.US).split(Regex("\\s+")).filter { it.isNotEmpty() }.sorted().joinToString(" ")
    val tb = b.lowercase(Locale.US).split(Regex("\\s+")).filter { it.isNotEmpty() }.sorted().joinToString(" ")
    return levenshteinSimilarity(ta, tb)
  }

  fun tokenSetRatio(a: String, b: String): Double {
    val setA = a.lowercase(Locale.US).split(Regex("\\s+")).filter { it.isNotEmpty() }.toSet()
    val setB = b.lowercase(Locale.US).split(Regex("\\s+")).filter { it.isNotEmpty() }.toSet()
    if (setA.isEmpty() || setB.isEmpty()) return 0.0
    val inter = setA.intersect(setB).sorted().joinToString(" ")
    if (inter.isEmpty()) return tokenSortRatio(a, b)
    val diffA = (setA - setB).sorted().joinToString(" ")
    val diffB = (setB - setA).sorted().joinToString(" ")
    val combA = listOf(inter, diffA).filter { it.isNotEmpty() }.joinToString(" ")
    val combB = listOf(inter, diffB).filter { it.isNotEmpty() }.joinToString(" ")
    return maxOf(
      levenshteinSimilarity(inter, combA),
      levenshteinSimilarity(inter, combB),
      levenshteinSimilarity(combA, combB),
    )
  }

  private fun levenshteinSimilarity(s1: String, s2: String): Double {
    if (s1 == s2) return 100.0
    if (s1.isEmpty() || s2.isEmpty()) return 0.0
    val dp = IntArray(s2.length + 1) { it }
    for (i in 1..s1.length) {
      var prev = dp[0]
      dp[0] = i
      for (j in 1..s2.length) {
        val temp = dp[j]
        dp[j] =
          if (s1[i - 1] == s2[j - 1]) prev
          else 1 + min(prev, min(dp[j], dp[j - 1]))
        prev = temp
      }
    }
    val maxLen = max(s1.length, s2.length)
    return ((maxLen - dp[s2.length]).toDouble() / maxLen) * 100.0
  }

  private fun attrSim(a: String, b: String): Double {
    if (a.isEmpty() && b.isEmpty()) return 100.0
    if (a.isEmpty() || b.isEmpty()) return 50.0
    if (a == b) return 100.0
    return tokenSortRatio(a, b)
  }

  fun matchProducts(
    query: NormalizedProduct,
    candidates: List<Pair<String, NormalizedProduct>>,
  ): List<MatchResult> {
    return candidates
      .map { (platform, cand) ->
        val sortScore = tokenSortRatio(query.productName, cand.productName)
        val setScore = tokenSetRatio(query.productName, cand.productName)
        val nameScore = 0.45 * sortScore + 0.55 * setScore
        val brandScore = attrSim(query.brand, cand.brand)
        val modelScore = attrSim(query.model, cand.model)
        val catScore = if (query.category == cand.category) 100.0 else attrSim(query.category, cand.category)
        val sizeScore = attrSim(query.size, cand.size)
        val colourScore = attrSim(query.colour, cand.colour)
        val qtyScore =
          if (query.canonicalUnit == cand.canonicalUnit && query.canonicalQuantity > 0.0) {
            (min(query.canonicalQuantity, cand.canonicalQuantity) /
              max(query.canonicalQuantity, cand.canonicalQuantity)) * 100.0
          } else 40.0

        val confidence =
          round2(
            0.34 * nameScore +
              0.16 * brandScore +
              0.14 * modelScore +
              0.12 * catScore +
              0.10 * sizeScore +
              0.07 * colourScore +
              0.07 * qtyScore
          )

        val modelOk =
          if (query.model.isEmpty() || cand.model.isEmpty()) true
          else query.model == cand.model
        val strictOk =
          brandScore >= 85.0 &&
            catScore >= 85.0 &&
            modelOk &&
            (sizeScore >= 90.0 || query.size.isEmpty() || cand.size.isEmpty()) &&
            qtyScore >= 90.0

        val mtype =
          when {
            confidence >= 88.0 && strictOk -> MatchType.EXACT
            confidence >= 60.0 && catScore >= 60.0 -> MatchType.SIMILAR
            else -> MatchType.NONE
          }

        MatchResult(
          queryProduct = query.productName,
          candidateProduct = cand.productName,
          platform = platform,
          confidenceScore = confidence,
          matchType = mtype,
          attributeScores =
            mapOf(
              "Name" to round2(nameScore),
              "Brand" to round2(brandScore),
              "Model" to round2(modelScore),
              "Category" to round2(catScore),
              "Size" to round2(sizeScore),
              "Colour" to round2(colourScore),
              "Qty" to round2(qtyScore),
            ),
        )
      }
      .sortedByDescending { it.confidenceScore }
  }

  // Module 3: Price Comparison
  fun evaluateOffers(offers: List<PlatformOffer>): PriceComparisonReport {
    val evaluated =
      offers
        .map { o ->
          val effectiveDiscAmt = max(0.0, o.mrp - (o.listedPrice - o.promoDiscount))
          val discPct = if (o.mrp > 0.0) round2((effectiveDiscAmt / o.mrp) * 100.0) else 0.0
          val finalPayable =
            round2(max(0.0, o.listedPrice - o.promoDiscount) + o.deliveryCharge + o.platformCharge)
          EvaluatedOffer(offer = o, discountPct = discPct, finalPayablePrice = finalPayable)
        }
        .sortedBy { it.finalPayablePrice }

    val available = evaluated.filter { it.offer.inStock }
    val prices = available.map { it.finalPayablePrice }
    return PriceComparisonReport(
      lowestPrice = prices.minOrNull(),
      highestPrice = prices.maxOrNull(),
      averagePrice = if (prices.isNotEmpty()) round2(prices.average()) else null,
      priceDifference = if (prices.size >= 2) round2(prices.max() - prices.min()) else 0.0,
      bestPlatform = available.minByOrNull { it.finalPayablePrice }?.offer?.platform,
      rankedOffers = evaluated,
    )
  }

  // Module 4: Buyhatke Observation Analysis (No invented history)
  fun classifyDeal(currentPrice: Double, histLow: Double?, histAvg: Double?, trend: String): String {
    if (histLow == null || histLow <= 0.0 || histAvg == null || histAvg <= 0.0) {
      return "Insufficient Historical Data (No Price Invented)"
    }
    val pctAboveLow = ((currentPrice - histLow) / histLow) * 100.0
    return when {
      pctAboveLow <= 3.0 -> "Strong Buy — Near Observed All-Time Low"
      currentPrice <= histAvg * 0.95 -> "Good Deal — Below Historical Average"
      currentPrice <= histAvg * 1.05 ->
        if (trend == "Falling") "Fair Price — Wait for Drop" else "Fair Price — Around Average"
      else -> "Overpriced — Wait (Above Historical Average)"
    }
  }

  fun analyzeBuyhatkeByCategory(observations: List<BuyhatkeObservation>): List<BuyhatkeCategoryAnalysis> {
    return observations
      .groupBy { it.category to it.productName }
      .mapNotNull { (key, grp) ->
        val sorted = grp.sortedBy { it.observationDate }
        val prices = sorted.map { it.currentPrice }
        if (prices.isEmpty()) return@mapNotNull null
        val latest = sorted.last()
        val current = latest.currentPrice
        val obsMin = prices.min()
        val obsMax = prices.max()
        val obsAvg = prices.average()
        val variance = prices.map { (it - obsAvg).pow(2) }.average()
        val stdDev = sqrt(variance)
        val cvPct = if (obsAvg > 0.0) round2((stdDev / obsAvg) * 100.0) else 0.0

        val refLow = latest.historicalLowestPrice ?: obsMin
        val refAvg = latest.historicalAveragePrice ?: obsAvg
        val pctAboveLow = if (refLow > 0.0) round2(((current - refLow) / refLow) * 100.0) else null

        val (slope, trend) =
          if (prices.size >= 2) {
            val n = prices.size.toDouble()
            val xs = (0 until prices.size).map { it.toDouble() }
            val xMean = xs.average()
            val num = xs.zip(prices).sumOf { (x, y) -> (x - xMean) * (y - obsAvg) }
            val den = xs.sumOf { x -> (x - xMean).pow(2) }
            val s = if (den > 0.0) num / den else 0.0
            val relSlope = if (obsAvg > 0.0) (s / obsAvg) * 100.0 else 0.0
            val t =
              when {
                relSlope < -0.8 -> "Falling"
                relSlope > 0.8 -> "Rising"
                else -> "Stable"
              }
            round2(s) to t
          } else {
            0.0 to "Insufficient Points"
          }

        BuyhatkeCategoryAnalysis(
          category = key.first,
          productName = key.second,
          platform = latest.platform,
          observations = sorted,
          currentPrice = round2(current),
          observedMin = round2(obsMin),
          observedAvg = round2(obsAvg),
          observedMax = round2(obsMax),
          pctAboveHistLow = pctAboveLow,
          volatilityStd = round2(stdDev),
          volatilityCvPct = cvPct,
          trendSlope = slope,
          priceTrend = trend,
          dealRecommendation = classifyDeal(current, refLow, refAvg, trend),
        )
      }
  }

  fun parseBuyhatkeCsv(csvText: String): List<BuyhatkeObservation> {
    val lines = csvText.trim().lines().filter { it.isNotBlank() }
    if (lines.size < 2) return emptyList()
    val headers = lines.first().split(",").map { it.trim().lowercase(Locale.US) }
    return lines.drop(1).mapNotNull { row ->
      val cols = row.split(",").map { it.trim() }
      val m = headers.zip(cols).toMap()
      val pname = m["product_name"] ?: return@mapNotNull null
      val cur = parsePriceValue(m["current_price"]) ?: return@mapNotNull null
      BuyhatkeObservation(
        category = m["category"] ?: "General",
        productName = pname,
        productUrl = m["product_url"] ?: "",
        platform = m["platform"] ?: "Manual",
        currentPrice = cur,
        historicalLowestPrice = parsePriceValue(m["historical_lowest_price"]),
        historicalAveragePrice = parsePriceValue(m["historical_average_price"]),
        historicalHighestPrice = parsePriceValue(m["historical_highest_price"]),
        observationDate = m["observation_date"] ?: "2026-11-23",
        source = m["source"] ?: "CSV Import",
      )
    }
  }

  // Modules 5 & 6: Grocery Normalization & Basket Recommendation
  fun evaluateAndRecommendBaskets(
    basket: List<GroceryBasketItem>,
    listings: List<GroceryStoreListing>,
    storeMetas: List<GroceryStoreMeta>,
  ): RecommendationResult {
    val evals =
      storeMetas.map { meta ->
        val platListings = listings.filter { it.platform == meta.platform }.associateBy { it.itemName.lowercase(Locale.US) }
        var subtotal = 0.0
        var availCount = 0
        val missing = mutableListOf<String>()
        val lines = mutableListOf<GroceryLineEvaluation>()

        for (req in basket) {
          val reqNorm = normalizeQuantityAndUnit(req.quantity, req.unit)
          val listing = platListings[req.itemName.lowercase(Locale.US)]
          if (listing == null || !listing.inStock) {
            missing.add(req.itemName)
            lines.add(
              GroceryLineEvaluation(
                itemName = req.itemName,
                requestedDisplay = "${formatQty(req.quantity)} ${req.unit}",
                normalizedDisplay = "${reqNorm.canonicalQty} ${reqNorm.canonicalUnit}",
                available = false,
                packsNeeded = null,
                lineCost = null,
              )
            )
            continue
          }
          val packNorm = normalizeQuantityAndUnit(listing.packQuantity, listing.packUnit)
          if (reqNorm.canonicalUnit != packNorm.canonicalUnit || packNorm.canonicalQty <= 0.0) {
            missing.add("${req.itemName} (unit mismatch)")
            continue
          }
          val packsExact = reqNorm.canonicalQty / packNorm.canonicalQty
          val packsNeeded = if (reqNorm.canonicalUnit == "pack") ceil(packsExact) else packsExact
          val lineCost = round2(packsNeeded * listing.packPrice)
          subtotal += lineCost
          availCount++
          lines.add(
            GroceryLineEvaluation(
              itemName = req.itemName,
              requestedDisplay = "${formatQty(req.quantity)} ${req.unit}",
              normalizedDisplay = "${reqNorm.canonicalQty} ${reqNorm.canonicalUnit}",
              available = true,
              packsNeeded = round2(packsNeeded),
              lineCost = lineCost,
            )
          )
        }

        val delivery =
          if (meta.freeDeliveryThreshold != null && subtotal >= meta.freeDeliveryThreshold) 0.0
          else meta.deliveryCharge
        val finalPayable = round2(max(0.0, subtotal - meta.basketDiscount) + delivery + meta.platformFee)
        BasketEvaluation(
          platform = meta.platform,
          isCompleteBasket = missing.isEmpty(),
          availableItems = availCount,
          totalRequestedItems = basket.size,
          missingItems = missing,
          itemSubtotal = round2(subtotal),
          deliveryCharge = delivery,
          platformFee = meta.platformFee,
          basketDiscount = meta.basketDiscount,
          finalPayableCost = finalPayable,
          deliveryTimeMins = meta.deliveryTimeMins,
          lineItems = lines,
        )
      }

    val complete =
      evals
        .filter { it.isCompleteBasket }
        .sortedWith(compareBy<BasketEvaluation> { it.finalPayableCost }.thenBy { it.deliveryTimeMins })
    val incomplete =
      evals
        .filter { !it.isCompleteBasket }
        .sortedWith(
          compareByDescending<BasketEvaluation> { it.availableItems }
            .thenBy { it.finalPayableCost }
            .thenBy { it.deliveryTimeMins }
        )

    return RecommendationResult(
      recommendedComplete = complete.firstOrNull(),
      completeRanked = complete,
      incompleteRanked = incomplete,
    )
  }

  // Module 8: Automated 8-Point Analytical Test Suite
  fun runAutomatedEngineTests(): List<EngineTestResult> {
    val results = mutableListOf<EngineTestResult>()

    // 1. Product Matching
    val q = normalizeProduct(SampleTestDatasets.sampleProducts.first())
    val cands = SampleTestDatasets.catalogCandidatesFor(q.productName).map { it.first to normalizeProduct(it.second) }
    val matched = matchProducts(q, cands)
    val exactCnt = matched.count { it.matchType == MatchType.EXACT }
    val simCnt = matched.count { it.matchType == MatchType.SIMILAR }
    results.add(
      EngineTestResult(
        "1. Product Matching",
        exactCnt >= 1 && simCnt >= 1,
        "Exact=$exactCnt, Similar=$simCnt (top=${matched.firstOrNull()?.confidenceScore}%)",
      )
    )

    // 2. Price Normalization
    val parsedPrice = parsePriceValue("₹1,24,999.50")
    val invalidPrice = parsePriceValue("N/A")
    results.add(
      EngineTestResult(
        "2. Price Normalization",
        parsedPrice != null && abs(parsedPrice - 124999.50) < 0.01 && invalidPrice == null,
        "Parsed '₹1,24,999.50' -> $parsedPrice; 'N/A' -> null",
      )
    )

    // 3. Quantity Conversion
    val gNorm = normalizeQuantityAndUnit(500.0, "grams")
    val mlNorm = normalizeQuantityAndUnit(1000.0, "ml")
    results.add(
      EngineTestResult(
        "3. Quantity Conversion",
        abs(gNorm.canonicalQty - 0.5) < 0.001 && gNorm.canonicalUnit == "kg" && abs(mlNorm.canonicalQty - 1.0) < 0.001 && mlNorm.canonicalUnit == "l",
        "500g -> ${gNorm.canonicalQty}${gNorm.canonicalUnit}, 1000ml -> ${mlNorm.canonicalQty}${mlNorm.canonicalUnit}",
      )
    )

    // 4. Discount Calculation
    val discReport =
      evaluateOffers(
        listOf(PlatformOffer("Test", "Store", mrp = 1000.0, listedPrice = 800.0, promoDiscount = 100.0, deliveryCharge = 50.0, platformCharge = 10.0))
      )
    val row = discReport.rankedOffers.first()
    results.add(
      EngineTestResult(
        "4. Discount Calculation",
        abs(row.discountPct - 30.0) < 0.01 && abs(row.finalPayablePrice - 760.0) < 0.01,
        "Discount=${row.discountPct}%, Payable=₹${row.finalPayablePrice}",
      )
    )

    // 5. Basket Total
    val rec =
      evaluateAndRecommendBaskets(
        SampleTestDatasets.testGroceryBasket,
        SampleTestDatasets.sampleGroceryListings,
        SampleTestDatasets.sampleGroceryStoreMeta,
      )
    val vg = rec.completeRanked.firstOrNull { it.platform.startsWith("ValueGrocer") }
    results.add(
      EngineTestResult(
        "5. Basket Total",
        vg != null && abs(vg.itemSubtotal - 337.0) < 0.01 && abs(vg.finalPayableCost - 327.0) < 0.01,
        "ValueGrocer subtotal=₹${vg?.itemSubtotal}, payable=₹${vg?.finalPayableCost}",
      )
    )

    // 6. Deal Classification
    val lowDeal = classifyDeal(102.0, 100.0, 120.0, "Falling")
    val highDeal = classifyDeal(140.0, 100.0, 120.0, "Rising")
    results.add(
      EngineTestResult(
        "6. Deal Classification",
        lowDeal.contains("All-Time Low") && highDeal.contains("Overpriced"),
        "Low='$lowDeal' | High='$highDeal'",
      )
    )

    // 7. Recommendation Ranking (Cost first, delivery time tie-breaker)
    val tieRec =
      evaluateAndRecommendBaskets(
        listOf(GroceryBasketItem("1", "milk", 1.0, "l")),
        listOf(
          GroceryStoreListing("IncompleteCheap", "apples", 1.0, "kg", 10.0),
          GroceryStoreListing("CompleteSlower", "milk", 1.0, "l", 60.0),
          GroceryStoreListing("CompleteFasterTie", "milk", 1.0, "l", 60.0),
        ),
        listOf(
          GroceryStoreMeta("IncompleteCheap", 0.0, null, 0.0, 0.0, 5),
          GroceryStoreMeta("CompleteSlower", 0.0, null, 0.0, 0.0, 25),
          GroceryStoreMeta("CompleteFasterTie", 0.0, null, 0.0, 0.0, 12),
        ),
      )
    results.add(
      EngineTestResult(
        "7. Recommendation Ranking",
        tieRec.recommendedComplete?.platform == "CompleteFasterTie" && tieRec.incompleteRanked.size == 1,
        "Winner=${tieRec.recommendedComplete?.platform}; Incomplete excluded=${tieRec.incompleteRanked.size}",
      )
    )

    // 8. Missing Data Handling
    val noHist = classifyDeal(500.0, null, null, "Stable")
    val speedy = rec.incompleteRanked.firstOrNull { it.platform.startsWith("SpeedyDrop") }
    results.add(
      EngineTestResult(
        "8. Missing Data Handling",
        noHist.contains("Insufficient") && speedy != null && !speedy.isCompleteBasket && speedy.missingItems.contains("apples"),
        "No-history='$noHist'; SpeedyDrop missing=${speedy?.missingItems}",
      )
    )

    return results
  }

  private fun round2(v: Double): Double = (v * 100.0).roundToInt() / 100.0

  private fun formatQty(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
}
