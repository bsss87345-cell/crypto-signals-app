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
    val stopLoss: Double,
    val durationText: String,
    val candleCloseSecondsRemaining: Long,
    val isBestOpportunity: Boolean = false
) {
    // Stop loss distance in percentage: |currentPrice - stopLoss| / currentPrice
    val stopLossDistancePercent: Double
        get() {
            if (currentPrice <= 0.0) return 0.02
            val dist = kotlin.math.abs(currentPrice - stopLoss) / currentPrice
            return if (dist <= 0.0001) 0.01 else dist
        }

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
