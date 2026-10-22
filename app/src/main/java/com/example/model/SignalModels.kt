package com.example.model

enum class SignalStatus(val labelArabic: String) {
    LIVE("مباشر"),
    PENDING("انتظار")
}

enum class SignalDirection(val labelArabic: String) {
    BUY("شراء"),
    SELL("بيع")
}

enum class SignalQuality(val labelArabic: String, val riskPercent: Double) {
    HIGHER("جودة أعلى", 0.01),      // 1% of capital
    STANDARD("جودة قياسية", 0.005)  // 0.5% of capital
}

data class Signal(
    val id: String,
    val pair: String,
    val coinSymbol: String,
    val status: SignalStatus,
    val direction: SignalDirection,
    val currentPrice: Double,
    val change24h: Double,
    val sparklinePoints: List<Float>,
    val quality: SignalQuality,
    val expectedProfitPercent: Double,
    val targetPrice: Double,
    val target1: Double = targetPrice,
    val target2: Double = targetPrice,
    val target3: Double = targetPrice,
    val stopLoss: Double,
    val durationText: String,
    val candleCloseSecondsRemaining: Long,
    val isBestOpportunity: Boolean = false,
    val score: Int = 0
) {
    // Stop loss distance in percentage: |currentPrice - stopLoss| / currentPrice
    val stopLossDistancePercent: Double
        get() {
            if (currentPrice <= 0.0) return 0.02
            val dist = kotlin.math.abs(currentPrice - stopLoss) / currentPrice
            return if (dist <= 0.0001) 0.01 else dist
        }

    // Profit % of each target relative to current price (works for buy and sell)
    private fun profitPercentTo(target: Double): Double {
        if (currentPrice <= 0.0) return 0.0
        return kotlin.math.abs(target - currentPrice) / currentPrice * 100.0
    }
    val target1ProfitPercent: Double get() = profitPercentTo(target1)
    val target2ProfitPercent: Double get() = profitPercentTo(target2)
    val target3ProfitPercent: Double get() = profitPercentTo(target3)

    // Weighted expected profit: 40% at target 1, 30% at target 2, 30% at target 3
    val weightedExpectedProfitPercent: Double
        get() = 0.4 * target1ProfitPercent +
            0.3 * target2ProfitPercent +
            0.3 * target3ProfitPercent

    // Calculates suggested investment details based on capital
    fun calculateInvestment(capital: Double): InvestmentSuggestion {
        val safeCapital = if (capital <= 0.0) 1000.0 else capital
        val riskPercent = quality.riskPercent
        val maxRiskAmount = safeCapital * riskPercent
        val slDist = stopLossDistancePercent

        // Amount = (capital x risk%) / stop-loss distance %, capped at 20% of capital
        val uncappedAmount = maxRiskAmount / slDist
        val maxCapAmount = safeCapital * 0.20 // 20% cap
        val suggestedAmount = kotlin.math.min(uncappedAmount, maxCapAmount)
        val percentOfCapital = (suggestedAmount / safeCapital) * 100.0
        val actualMaxLoss = suggestedAmount * slDist

        return InvestmentSuggestion(
            capital = safeCapital,
            suggestedAmount = suggestedAmount,
            percentOfCapital = percentOfCapital,
            maxLossAmount = actualMaxLoss,
            riskPercent = riskPercent * 100.0,
            stopLossDistancePercent = slDist * 100.0,
            isCapped = uncappedAmount > maxCapAmount
        )
    }
}

data class InvestmentSuggestion(
    val capital: Double,
    val suggestedAmount: Double,
    val percentOfCapital: Double,
    val maxLossAmount: Double,
    val riskPercent: Double,
    val stopLossDistancePercent: Double,
    val isCapped: Boolean
)
