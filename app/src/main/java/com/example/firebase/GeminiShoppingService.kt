package com.example.firebase

import android.util.Log
import com.example.domain.ParsedSearchQuery
import com.example.domain.ProductUsageInfo
import com.example.domain.ShoppingCategory
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiShoppingService"

object GeminiShoppingService {
  private val searchSchema =
    Schema.obj(
      properties =
        mapOf(
          "productName" to Schema.string("Core consumer product name without budget or filler words"),
          "brand" to Schema.string("Brand name if specified, otherwise empty string"),
          "category" to
            Schema.enumeration(
              values = ShoppingCategory.entries.map { it.label },
              description = "Best matching shopping category",
            ),
          "material" to Schema.string("Material like stainless steel, cotton, plastic, glass, leather"),
          "size" to Schema.string("Size like UK 9, L, 46mm, 55 inch"),
          "capacity" to Schema.string("Volume/capacity like 1 litre, 500 ml, 256GB"),
          "weight" to Schema.string("Weight like 1 kg, 500 g"),
          "colour" to Schema.string("Colour if mentioned"),
          "model" to Schema.string("Model identifier like iPhone 16, WH-1000XM5, Pegasus 41"),
          "quantity" to Schema.double("Numeric quantity requested, default 1.0"),
          "unit" to Schema.string("Unit like kg, litre, packet, unit, pair"),
          "budgetInr" to Schema.double("Max budget in INR if specified, or 0 if not specified"),
          "requiredSpecs" to
            Schema.array(
              items = Schema.string(),
              description = "Key constraints or specs requested by the user",
            ),
        ),
      optionalProperties = listOf("brand", "material", "size", "capacity", "weight", "colour", "model", "budgetInr"),
    )

  private val usageSchema =
    Schema.obj(
      properties =
        mapOf(
          "title" to Schema.string("Question title like 'What are bananas useful for?'"),
          "summary" to Schema.string("Clear 2-sentence explanation of what the product is"),
          "commonUses" to Schema.array(items = Schema.string(), description = "4-6 practical uses"),
          "whoShouldBuy" to Schema.string("Who finds this product most useful"),
          "keyFeatures" to
            Schema.array(
              items = Schema.string(),
              description = "Key features or nutritional highlights (no unsupported medical claims)",
            ),
          "advantages" to Schema.array(items = Schema.string(), description = "3-4 advantages"),
          "limitations" to Schema.array(items = Schema.string(), description = "2-3 limitations"),
          "buyingConsiderations" to
            Schema.array(
              items = Schema.string(),
              description = "3-5 practical tips on how to choose a good product",
            ),
        )
    )

  private val searchModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      generationConfig =
        generationConfig {
          responseMimeType = "application/json"
          responseSchema = searchSchema
        },
    )
  }

  private val usageModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      generationConfig =
        generationConfig {
          responseMimeType = "application/json"
          responseSchema = usageSchema
        },
    )
  }

  suspend fun parseSearchQueryWithGemini(rawQuery: String): Result<ParsedSearchQuery> {
    return try {
      val prompt =
        """
        Parse this Indian e-commerce & grocery shopping search query into structured product attributes.
        Query: "$rawQuery"
        Extract material, capacity, weight, size, brand, model, quantity, unit, budgetInr (0 if omitted), and category accurately.
        """.trimIndent()
      val response = withTimeout(45_000L) { searchModel.generateContent(prompt) }
      val jsonText = response.text ?: throw IllegalStateException("Gemini returned an empty response.")
      val json = JSONObject(jsonText)
      val catLabel = json.optString("category", ShoppingCategory.OTHER.label)
      val category = ShoppingCategory.fromLabel(catLabel)
      val specsArr = json.optJSONArray("requiredSpecs")
      val specs =
        if (specsArr != null) (0 until specsArr.length()).mapNotNull { specsArr.optString(it).takeIf { s -> s.isNotBlank() } }
        else emptyList()
      val budgetRaw = json.optDouble("budgetInr", 0.0)
      Result.success(
        ParsedSearchQuery(
          rawQuery = rawQuery,
          productName = json.optString("productName", rawQuery).ifBlank { rawQuery },
          brand = json.optString("brand", ""),
          category = category,
          material = json.optString("material", ""),
          size = json.optString("size", ""),
          capacity = json.optString("capacity", ""),
          weight = json.optString("weight", ""),
          colour = json.optString("colour", ""),
          model = json.optString("model", ""),
          quantity = json.optDouble("quantity", 1.0).let { if (it > 0) it else 1.0 },
          unit = json.optString("unit", "unit").ifBlank { "unit" },
          budgetInr = if (budgetRaw > 0.0) budgetRaw else null,
          requiredSpecs = specs,
          parsedByGemini = true,
        )
      )
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Gemini search parsing timed out", e)
      Result.failure(e)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Gemini search parsing failed", e)
      Result.failure(e)
    }
  }

  suspend fun explainProductUsage(
    productName: String,
    brand: String,
    category: String,
    specs: String,
  ): Result<ProductUsageInfo> {
    return try {
      val prompt =
        """
        Provide an informative, objective consumer guide answering "What is this product useful for?" for:
        Product: $productName
        Brand: ${brand.ifBlank { "Any" }}
        Category: $category
        Specifications: ${specs.ifBlank { "Standard" }}

        Rules:
        - Tailor every bullet specifically to "$productName".
        - For food/groceries, mention balanced nutritional highlights (e.g., carbs, potassium, fibre) without making unsupported medical claims.
        - For durables (bottles, electronics, footwear, appliances), focus on materials, specs, durability, cleaning/care, and buying considerations.
        """.trimIndent()
      val response = withTimeout(60_000L) { usageModel.generateContent(prompt) }
      val jsonText = response.text ?: throw IllegalStateException("Gemini returned an empty explanation.")
      val json = JSONObject(jsonText)

      fun readArray(key: String): List<String> {
        val arr = json.optJSONArray(key) ?: return emptyList()
        return (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
      }

      Result.success(
        ProductUsageInfo(
          title = json.optString("title", "What is $productName useful for?"),
          summary = json.optString("summary", ""),
          commonUses = readArray("commonUses"),
          whoShouldBuy = json.optString("whoShouldBuy", ""),
          keyFeatures = readArray("keyFeatures"),
          advantages = readArray("advantages"),
          limitations = readArray("limitations"),
          buyingConsiderations = readArray("buyingConsiderations"),
          generatedByGemini = true,
        )
      )
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Gemini usage explanation timed out", e)
      Result.failure(Exception("Gemini request timed out after 60 seconds. Please try again."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Gemini usage explanation failed", e)
      Result.failure(e)
    }
  }
}
