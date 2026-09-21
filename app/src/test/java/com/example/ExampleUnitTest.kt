package com.example

import com.example.util.BatteryInfo
import com.example.util.StorageHelper
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun batteryInfo_criticalCheck() {
    val normal = BatteryInfo(level = 45, isCharging = false, isCritical = false)
    assertFalse(normal.isCritical)

    val low = BatteryInfo(level = 4, isCharging = false, isCritical = true)
    assertTrue(low.isCritical)
  }

  @Test
  fun storageHelper_thresholdAndFormatting() {
    assertEquals(500L, StorageHelper.LOW_STORAGE_THRESHOLD_MB)
    assertEquals(500L * 1024L * 1024L, StorageHelper.LOW_STORAGE_THRESHOLD_BYTES)

    val formattedMb = StorageHelper.formatBytes(250L * 1024L * 1024L)
    assertTrue(formattedMb.contains("MB") || formattedMb.contains("250"))
  }
}
