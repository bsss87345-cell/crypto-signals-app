package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.Signal
import com.example.model.SignalDirection
import com.example.model.SignalQuality
import com.example.model.SignalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("إشارات كريبتو", appName)
  }

  @Test
  fun `verify investment calculation with 20 percent cap`() {
    val signal = Signal(
        id = "test-1",
        pair = "ETH/USDT",
        coinSymbol = "ETH",
        status = SignalStatus.LIVE,
        direction = SignalDirection.BUY,
        currentPrice = 3420.50,
        change24h = 4.82,
        sparklinePoints = listOf(3300f, 3420.5f),
        quality = SignalQuality.HIGHER,
        expectedProfitPercent = 4.66,
        targetPrice = 3580.00,
        stopLoss = 3340.00,
        durationText = "1 - 3 ساعات",
        candleCloseSecondsRemaining = 272
    )

    val capital = 10000.0
    val suggestion = signal.calculateInvestment(capital)

    // Higher quality risk = 1% = 100 USDT
    // SL distance = (3420.50 - 3340) / 3420.50 = 80.5 / 3420.50 ~ 0.023534
    // Raw amount = 100 / 0.023534 ~ 4249.19 USDT
    // 20% of 10000 = 2000 USDT -> Should be capped at 2000 USDT
    assertTrue(suggestion.isCapped)
    assertEquals(2000.0, suggestion.suggestedAmount, 0.01)
    assertEquals(20.0, suggestion.percentOfCapital, 0.01)
  }
}
