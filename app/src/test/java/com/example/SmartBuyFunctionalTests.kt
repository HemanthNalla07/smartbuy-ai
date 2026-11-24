package com.example

import com.example.data.LocationHelper
import com.example.data.PlatformSearchCatalog
import com.example.domain.DeliveryAddress
import com.example.domain.MatchType
import com.example.domain.SearchSortOption
import com.example.domain.ShoppingCategory
import com.example.domain.ShoppingPlatform
import com.example.domain.SmartBuyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartBuyFunctionalTests {

  @Test
  fun test1_bananasSearchRoutesGroceryPlatformsAndCheapestVerified() {
    val parsed = SmartBuyEngine.fallbackParseSearchQuery("1 kg bananas")
    assertEquals(ShoppingCategory.GROCERIES, parsed.category)

    val results = PlatformSearchCatalog.searchAcrossPlatforms(parsed, "560103")
    val sorted = SmartBuyEngine.sortAndFilterSearchResults(results, SearchSortOption.LOWEST_FINAL_PRICE, false, false, null)

    assertTrue(sorted.any { it.platform == ShoppingPlatform.BLINKIT && it.matchType == MatchType.EXACT })
    assertTrue(sorted.none { it.platform == ShoppingPlatform.MYNTRA })
    val cheapest = sorted.firstOrNull { it.isVerifiedCheapestEligible }
    assertNotNull(cheapest)
    // Incomplete fees (JioMart has null delivery/platform fees) must NOT be verified cheapest
    assertTrue(sorted.any { it.platform == ShoppingPlatform.JIOMART && !it.isFeesComplete && !it.isVerifiedCheapestEligible })
  }

  @Test
  fun test2_steelBottleSeparatesExactFromPlasticAlternative() {
    val parsed = SmartBuyEngine.fallbackParseSearchQuery("1 litre stainless steel water bottle under ₹500")
    assertEquals("Stainless Steel", parsed.material)
    assertEquals(ShoppingCategory.HOME_KITCHEN, parsed.category)
    assertEquals(500.0, parsed.budgetInr ?: 0.0, 0.01)

    val results = PlatformSearchCatalog.searchAcrossPlatforms(parsed, "560103")
    val exact = results.filter { !it.isExternalSearchOnly && it.matchType == MatchType.EXACT }
    val similar = results.filter { !it.isExternalSearchOnly && it.matchType == MatchType.SIMILAR }

    assertTrue("Expected stainless steel exact matches", exact.isNotEmpty())
    assertTrue(exact.all { it.material.equals("Stainless Steel", ignoreCase = true) })
    assertTrue("Plastic bottle must be separated into similar alternatives", similar.any { it.material.equals("Plastic", ignoreCase = true) })
    assertFalse("Plastic bottle must NOT be in exact matches", exact.any { it.material.equals("Plastic", ignoreCase = true) })
  }

  @Test
  fun test3_iphone16RoutesElectronicsNotFashion() {
    val parsed = SmartBuyEngine.fallbackParseSearchQuery("iPhone 16")
    assertEquals(ShoppingCategory.ELECTRONICS, parsed.category)

    val results = PlatformSearchCatalog.searchAcrossPlatforms(parsed, "560103")
    assertTrue(results.any { it.platform == ShoppingPlatform.FLIPKART })
    assertTrue(results.any { it.platform == ShoppingPlatform.AMAZON })
    assertTrue(results.none { it.platform == ShoppingPlatform.MYNTRA })
    assertTrue(results.none { it.platform == ShoppingPlatform.BLINKIT })
  }

  @Test
  fun test4_nikeShoesSize9RoutesFootwearAndMatchesSize() {
    val parsed = SmartBuyEngine.fallbackParseSearchQuery("Nike shoes size 9")
    assertEquals("Nike", parsed.brand)
    assertEquals(ShoppingCategory.FOOTWEAR, parsed.category)
    assertEquals("UK 9", parsed.size)

    val results = PlatformSearchCatalog.searchAcrossPlatforms(parsed, "560103")
    assertTrue(results.any { it.platform == ShoppingPlatform.MYNTRA && it.matchType == MatchType.EXACT })
    assertTrue(results.any { it.platform == ShoppingPlatform.AJIO && it.matchType == MatchType.EXACT })
  }

  @Test
  fun test5_pinChangeUpdatesQuickCommerceServiceability() {
    val parsed = SmartBuyEngine.fallbackParseSearchQuery("1 kg bananas")
    val metroResults = PlatformSearchCatalog.searchAcrossPlatforms(parsed, "560103")
    val nonMetroResults = PlatformSearchCatalog.searchAcrossPlatforms(parsed, "800001")

    val metroBlinkit = metroResults.first { it.platform == ShoppingPlatform.BLINKIT }
    val nonMetroBlinkit = nonMetroResults.first { it.platform == ShoppingPlatform.BLINKIT }

    assertTrue(metroBlinkit.deliveryVerifiedForPin)
    assertFalse(nonMetroBlinkit.deliveryVerifiedForPin)
    assertTrue(nonMetroBlinkit.deliveryEstimate.contains("800001"))
  }

  @Test
  fun test6_indianPinCodeValidationAndAddressFormatting() {
    assertTrue(LocationHelper.isValidIndianPinCode("560103"))
    assertTrue(LocationHelper.isValidIndianPinCode("110001"))
    assertFalse(LocationHelper.isValidIndianPinCode("060103")) // starts with 0
    assertFalse(LocationHelper.isValidIndianPinCode("56010")) // 5 digits
    assertFalse(LocationHelper.isValidIndianPinCode("ABC123"))

    val addr =
      DeliveryAddress(
        id = "1",
        label = "Home",
        houseNo = "42",
        street = "27th Main",
        area = "HSR Layout",
        city = "Bengaluru",
        state = "Karnataka",
        pinCode = "560103",
        isDefault = true,
      )
    assertEquals("Bengaluru – 560103", addr.shortHeaderText)
  }

  @Test
  fun test7_incompleteFeesNeverWinCheapestVerifiedClaim() {
    val parsed = SmartBuyEngine.fallbackParseSearchQuery("1 kg bananas")
    val results = PlatformSearchCatalog.searchAcrossPlatforms(parsed, "560103")
    val cheapest = SmartBuyEngine.sortAndFilterSearchResults(results, SearchSortOption.LOWEST_FINAL_PRICE, false, false, null)
      .firstOrNull { it.isVerifiedCheapestEligible }

    assertNotNull(cheapest)
    // JioMart item price is ₹52 (lowest listed), but fees are null -> must not be chosen as verified cheapest!
    assertFalse(cheapest!!.platform == ShoppingPlatform.JIOMART)
    assertTrue(cheapest.isFeesComplete)
  }

  @Test
  fun test8_externalPlatformSearchUrlBuilderWorksForAll12Platforms() {
    ShoppingPlatform.entries.forEach { platform ->
      val url = platform.buildSearchUrl("steel water bottle")
      assertTrue(url.startsWith("https://"))
    }
  }
}
