package com.example

import com.example.domain.SmartBuyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartBuyEngineUnitTest {
  @Test
  fun allEightEngineChecksPass() {
    val results = SmartBuyEngine.runAutomatedEngineTests()
    assertEquals(8, results.size)
    results.forEach { r ->
      assertTrue("${r.testName} failed: ${r.details}", r.passed)
    }
  }
}
