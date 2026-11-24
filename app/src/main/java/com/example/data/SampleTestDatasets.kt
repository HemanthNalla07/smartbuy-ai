package com.example.data

import com.example.domain.BuyhatkeObservation
import com.example.domain.GroceryBasketItem
import com.example.domain.GroceryStoreListing
import com.example.domain.GroceryStoreMeta
import com.example.domain.PlatformOffer
import com.example.domain.RawProductInput

object SampleTestDatasets {
  val sampleProducts =
    listOf(
      RawProductInput(
        productName = "WH-1000XM5 Wireless ANC",
        brand = "Sony",
        category = "Headphones",
        model = "WH1000XM5",
        size = "Over-Ear",
        colour = "Midnight",
        quantity = 1.0,
        unit = "pcs",
        budgetRaw = "₹28,000",
      ),
      RawProductInput(
        productName = "Galaxy S24 Ultra 5G",
        brand = "Samsung",
        category = "Smartphone",
        model = "SM-S928B",
        size = "256GB",
        colour = "Titanium Black",
        quantity = 1.0,
        unit = "unit",
        budgetRaw = "₹1,25,000",
      ),
      RawProductInput(
        productName = "Pegasus 41 Running Shoes",
        brand = "Nike",
        category = "Running Shoes",
        model = "FD2722",
        size = "UK 9",
        colour = "Volt / Blk",
        quantity = 1.0,
        unit = "pair",
        budgetRaw = "11500",
      ),
      RawProductInput(
        productName = "Watch Series 10 GPS",
        brand = "Apple",
        category = "Smartwatch",
        model = "MWWE3",
        size = "46mm",
        colour = "Jet Black",
        quantity = 1.0,
        unit = "unit",
        budgetRaw = "₹47,500",
      ),
      RawProductInput(
        productName = "Essential Slim Crew Tee",
        brand = "Uniqlo",
        category = "T-Shirt",
        model = "UQ-465185",
        size = "L",
        colour = "Navy Blue",
        quantity = 2.0,
        unit = "pkts",
        budgetRaw = "2500",
      ),
    )

  fun catalogCandidatesFor(productName: String): List<Pair<String, RawProductInput>> {
    return when {
      productName.contains("WH-1000XM5", ignoreCase = true) ->
        listOf(
          "Store Alpha [SAMPLE]" to
            RawProductInput(
              productName = "Sony WH-1000XM5 Wireless Noise Cancelling Headphones",
              brand = "Sony",
              category = "Headphones",
              model = "WH1000XM5",
              size = "Over-Ear",
              colour = "Black",
              quantity = 1.0,
              unit = "unit",
            ),
          "Store Beta [SAMPLE]" to
            RawProductInput(
              productName = "WH-1000XM5 ANC Headset",
              brand = "Sony",
              category = "Headphones",
              model = "WH1000XM5",
              size = "Over-Ear",
              colour = "Silver",
              quantity = 1.0,
              unit = "pcs",
            ),
          "Store Gamma [SAMPLE]" to
            RawProductInput(
              productName = "Sony WH-1000XM4 Wireless Headphones",
              brand = "Sony",
              category = "Headphones",
              model = "WH1000XM4",
              size = "Over-Ear",
              colour = "Black",
              quantity = 1.0,
              unit = "unit",
            ),
          "Store Delta [SAMPLE]" to
            RawProductInput(
              productName = "Samsung Galaxy Buds2 Pro",
              brand = "Samsung",
              category = "Headphones",
              model = "R510",
              size = "In-Ear",
              colour = "White",
              quantity = 1.0,
              unit = "unit",
            ),
        )
      else ->
        listOf(
          "Store Alpha [SAMPLE]" to
            RawProductInput(
              productName = productName,
              brand = "",
              category = "general",
              quantity = 1.0,
              unit = "unit",
            ),
          "Store Beta [SAMPLE]" to
            RawProductInput(
              productName = "$productName Pro Edition",
              brand = "",
              category = "general",
              quantity = 1.0,
              unit = "unit",
            ),
        )
    }
  }

  fun sampleOffersFor(productName: String): List<PlatformOffer> {
    return when {
      productName.contains("Galaxy S24", ignoreCase = true) ->
        listOf(
          PlatformOffer(productName, "Store Alpha [SAMPLE]", 134999.0, 119999.0, 3500.0, 0.0, 99.0, true, 120),
          PlatformOffer(productName, "Store Beta [SAMPLE]", 134999.0, 118499.0, 1000.0, 199.0, 49.0, true, 90),
          PlatformOffer(productName, "Store Gamma [SAMPLE]", 134999.0, 122999.0, 5000.0, 0.0, 0.0, true, 180),
        )
      productName.contains("Pegasus", ignoreCase = true) ->
        listOf(
          PlatformOffer(productName, "Store Alpha [SAMPLE]", 11895.0, 9990.0, 500.0, 0.0, 29.0, true, 60),
          PlatformOffer(productName, "Store Beta [SAMPLE]", 11895.0, 9490.0, 0.0, 149.0, 49.0, true, 45),
          PlatformOffer(productName, "Store Gamma [SAMPLE]", 11895.0, 10490.0, 750.0, 0.0, 0.0, true, 120),
        )
      else ->
        listOf(
          PlatformOffer(productName, "Store Alpha [SAMPLE]", 34990.0, 26990.0, 1500.0, 0.0, 49.0, true, 60),
          PlatformOffer(productName, "Store Beta [SAMPLE]", 34990.0, 25990.0, 0.0, 199.0, 99.0, true, 45),
          PlatformOffer(productName, "Store Gamma [SAMPLE]", 34990.0, 27990.0, 2000.0, 0.0, 0.0, true, 90),
          PlatformOffer(productName, "Store Delta [SAMPLE]", 34990.0, 28490.0, 500.0, 120.0, 29.0, true, 30),
        )
    }
  }

  val buyhatkeCategories = listOf("Smartphones", "Shoes", "Clothing", "Headphones", "Smartwatches")

  val sampleBuyhatkeObservations =
    listOf(
      BuyhatkeObservation("Smartphones", "Samsung Galaxy S24 Ultra 256GB", "https://example.com/s24u", "Store Alpha", 129999.0, 114999.0, 122500.0, 134999.0, "2026-09-01", "Manual Sample [SAMPLE]"),
      BuyhatkeObservation("Smartphones", "Samsung Galaxy S24 Ultra 256GB", "https://example.com/s24u", "Store Alpha", 124999.0, 114999.0, 122500.0, 134999.0, "2026-10-01", "Manual Sample [SAMPLE]"),
      BuyhatkeObservation("Smartphones", "Samsung Galaxy S24 Ultra 256GB", "https://example.com/s24u", "Store Alpha", 119999.0, 114999.0, 122500.0, 134999.0, "2026-10-20", "Manual Sample [SAMPLE]"),
      BuyhatkeObservation("Smartphones", "Samsung Galaxy S24 Ultra 256GB", "https://example.com/s24u", "Store Alpha", 116499.0, 114999.0, 122500.0, 134999.0, "2026-11-15", "Manual Sample [SAMPLE]"),

      BuyhatkeObservation("Shoes", "Nike Pegasus 41 Running Shoes", "https://example.com/peg41", "Store Beta", 11895.0, 8990.0, 10450.0, 11895.0, "2026-09-01", "Manual Sample [SAMPLE]"),
      BuyhatkeObservation("Shoes", "Nike Pegasus 41 Running Shoes", "https://example.com/peg41", "Store Beta", 10795.0, 8990.0, 10450.0, 11895.0, "2026-10-01", "Manual Sample [SAMPLE]"),
      BuyhatkeObservation("Shoes", "Nike Pegasus 41 Running Shoes", "https://example.com/peg41", "Store Beta", 9890.0, 8990.0, 10450.0, 11895.0, "2026-10-25", "Manual Sample [SAMPLE]"),
      BuyhatkeObservation("Shoes", "Nike Pegasus 41 Running Shoes", "https://example.com/peg41", "Store Beta", 9490.0, 8990.0, 10450.0, 11895.0, "2026-11-15", "Manual Sample [SAMPLE]"),

      BuyhatkeObservation("Clothing", "Uniqlo Slim Crew T-Shirt", "https://example.com/uniqlo-tee", "Store Gamma", 1290.0, 890.0, 1120.0, 1490.0, "2026-09-01", "Manual Sample [SAMPLE]"),
      BuyhatkeObservation("Clothing", "Uniqlo Slim Crew T-Shirt", "https://example.com/uniqlo-tee", "Store Gamma", 1190.0, 890.0, 1120.0, 1490.0, "2026-10-01", "Manual Sample [SAMPLE]"),
      BuyhatkeObservation("Clothing", "Uniqlo Slim Crew T-Shirt", "https://example.com/uniqlo-tee", "Store Gamma", 1290.0, 890.0, 1120.0, 1490.0, "2026-10-25", "Manual Sample [SAMPLE]"),
      BuyhatkeObservation("Clothing", "Uniqlo Slim Crew T-Shirt", "https://example.com/uniqlo-tee", "Store Gamma", 1390.0, 890.0, 1120.0, 1490.0, "2026-11-15", "Manual Sample [SAMPLE]"),

      BuyhatkeObservation("Headphones", "Sony WH-1000XM5 ANC", "https://example.com/xm5", "Store Alpha", 29990.0, 23990.0, 26800.0, 34990.0, "2026-09-01", "Manual Sample [SAMPLE]"),
      BuyhatkeObservation("Headphones", "Sony WH-1000XM5 ANC", "https://example.com/xm5", "Store Alpha", 27990.0, 23990.0, 26800.0, 34990.0, "2026-10-01", "Manual Sample [SAMPLE]"),
      BuyhatkeObservation("Headphones", "Sony WH-1000XM5 ANC", "https://example.com/xm5", "Store Alpha", 25490.0, 23990.0, 26800.0, 34990.0, "2026-10-25", "Manual Sample [SAMPLE]"),
      BuyhatkeObservation("Headphones", "Sony WH-1000XM5 ANC", "https://example.com/xm5", "Store Alpha", 24490.0, 23990.0, 26800.0, 34990.0, "2026-11-15", "Manual Sample [SAMPLE]"),

      BuyhatkeObservation("Smartwatches", "Apple Watch Series 10 46mm", "https://example.com/aw10", "Store Delta", 46900.0, 41900.0, 44900.0, 49900.0, "2026-09-01", "Manual Sample [SAMPLE]"),
      BuyhatkeObservation("Smartwatches", "Apple Watch Series 10 46mm", "https://example.com/aw10", "Store Delta", 45900.0, 41900.0, 44900.0, 49900.0, "2026-10-01", "Manual Sample [SAMPLE]"),
      BuyhatkeObservation("Smartwatches", "Apple Watch Series 10 46mm", "https://example.com/aw10", "Store Delta", 44900.0, 41900.0, 44900.0, 49900.0, "2026-10-25", "Manual Sample [SAMPLE]"),
      BuyhatkeObservation("Smartwatches", "Apple Watch Series 10 46mm", "https://example.com/aw10", "Store Delta", 44900.0, 41900.0, 44900.0, 49900.0, "2026-11-15", "Manual Sample [SAMPLE]"),
    )

  val testGroceryBasket =
    listOf(
      GroceryBasketItem("1", "bananas", 4.0, "pieces"),
      GroceryBasketItem("2", "milk", 1.0, "litre"),
      GroceryBasketItem("3", "tomatoes", 500.0, "grams"),
      GroceryBasketItem("4", "apples", 1.0, "kg"),
      GroceryBasketItem("5", "bread", 2.0, "packets"),
    )

  val sampleGroceryStoreMeta =
    listOf(
      GroceryStoreMeta("QuickMart [SAMPLE]", 25.0, 350.0, 5.0, 15.0, 14),
      GroceryStoreMeta("FreshBasket [SAMPLE]", 15.0, 500.0, 0.0, 10.0, 22),
      GroceryStoreMeta("ValueGrocer [SAMPLE]", 0.0, null, 10.0, 20.0, 35),
      GroceryStoreMeta("SpeedyDrop [SAMPLE]", 20.0, null, 5.0, 25.0, 10),
    )

  val sampleGroceryListings =
    listOf(
      GroceryStoreListing("QuickMart [SAMPLE]", "bananas", 4.0, "pcs", 36.0),
      GroceryStoreListing("QuickMart [SAMPLE]", "milk", 500.0, "ml", 33.0),
      GroceryStoreListing("QuickMart [SAMPLE]", "tomatoes", 500.0, "g", 28.0),
      GroceryStoreListing("QuickMart [SAMPLE]", "apples", 1.0, "kg", 160.0),
      GroceryStoreListing("QuickMart [SAMPLE]", "bread", 1.0, "packet", 45.0),

      GroceryStoreListing("FreshBasket [SAMPLE]", "bananas", 1.0, "unit", 8.5),
      GroceryStoreListing("FreshBasket [SAMPLE]", "milk", 1.0, "l", 62.0),
      GroceryStoreListing("FreshBasket [SAMPLE]", "tomatoes", 250.0, "g", 13.0),
      GroceryStoreListing("FreshBasket [SAMPLE]", "apples", 500.0, "g", 75.0),
      GroceryStoreListing("FreshBasket [SAMPLE]", "bread", 1.0, "pkt", 42.0),

      GroceryStoreListing("ValueGrocer [SAMPLE]", "bananas", 4.0, "pieces", 30.0),
      GroceryStoreListing("ValueGrocer [SAMPLE]", "milk", 1000.0, "ml", 60.0),
      GroceryStoreListing("ValueGrocer [SAMPLE]", "tomatoes", 1.0, "kg", 48.0),
      GroceryStoreListing("ValueGrocer [SAMPLE]", "apples", 1.0, "kg", 145.0),
      GroceryStoreListing("ValueGrocer [SAMPLE]", "bread", 2.0, "packets", 78.0),

      GroceryStoreListing("SpeedyDrop [SAMPLE]", "bananas", 4.0, "pieces", 32.0),
      GroceryStoreListing("SpeedyDrop [SAMPLE]", "milk", 1.0, "litre", 64.0),
      GroceryStoreListing("SpeedyDrop [SAMPLE]", "tomatoes", 500.0, "grams", 24.0),
      GroceryStoreListing("SpeedyDrop [SAMPLE]", "apples", 1.0, "kg", 140.0, inStock = false),
      GroceryStoreListing("SpeedyDrop [SAMPLE]", "bread", 1.0, "packet", 40.0),
    )
}
