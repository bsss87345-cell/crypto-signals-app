package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Signal
import com.example.model.SignalDirection
import com.example.model.SignalQuality
import com.example.model.SignalStatus
import com.example.ui.SignalTab
import com.example.ui.components.CoinIcon
import com.example.ui.components.SparklineChart
import com.example.ui.dialog.InvestmentDialog
import com.example.ui.theme.CardBg
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBgElevated
import com.example.ui.theme.DividerColor
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldGlow
import com.example.ui.theme.GreenAccent
import com.example.ui.theme.GreenBg
import com.example.ui.theme.GreenBorder
import com.example.ui.theme.InputBg
import com.example.ui.theme.RedAccent
import com.example.ui.theme.RedBg
import com.example.ui.theme.RedBorder
import com.example.ui.theme.TealBorder
import com.example.ui.theme.TealBorderStrong
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.DecimalFormat
import java.util.Locale

@Composable
fun SignalsScreen(
    signals: List<Signal>,
    selectedTab: SignalTab,
    lastUpdateTime: String,
    isRefreshing: Boolean,
    savedCapital: Double,
    activeInvestmentSignal: Signal?,
    onTabSelected: (SignalTab) -> Unit,
    onRefresh: () -> Unit,
    onOpenInvestmentDialog: (Signal) -> Unit,
    onCloseInvestmentDialog: () -> Unit,
    onCapitalChanged: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    // Filter signals based on selected tab
    val filteredSignals = remember(signals, selectedTab) {
        when (selectedTab) {
            SignalTab.ALL -> signals
            SignalTab.LIVE -> signals.filter { it.status == SignalStatus.LIVE }
            SignalTab.PENDING -> signals.filter { it.status == SignalStatus.PENDING }
        }
    }

    val liveCount = signals.count { it.status == SignalStatus.LIVE }
    val pendingCount = signals.count { it.status == SignalStatus.PENDING }

    val rotationTransition = rememberInfiniteTransition(label = "refresh_rotation")
    val rotation by rotationTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar: App Title, Last Update Time, Refresh Button
            TopBarSection(
                lastUpdateTime = lastUpdateTime,
                isRefreshing = isRefreshing,
                refreshRotation = if (isRefreshing) rotation else 0f,
                onRefresh = onRefresh
            )

            // Fixed Risk Banner: "الإشارات أداة مساعدة وليست توصية مالية"
            FixedRiskBanner()

            // Filter Tabs: الكل / مباشر / انتظار
            TabsSection(
                selectedTab = selectedTab,
                allCount = signals.size,
                liveCount = liveCount,
                pendingCount = pendingCount,
                onTabSelected = onTabSelected
            )

            // Signals List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("signals_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredSignals, key = { it.id }) { signal ->
                    SignalCard(
                        signal = signal,
                        onInvestClick = { onOpenInvestmentDialog(signal) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Investment dialog for "مباشر" card
        if (activeInvestmentSignal != null) {
            InvestmentDialog(
                signal = activeInvestmentSignal,
                currentCapital = savedCapital,
                onCapitalChanged = onCapitalChanged,
                onDismiss = onCloseInvestmentDialog
            )
        }
    }
}

@Composable
private fun TopBarSection(
    lastUpdateTime: String,
    isRefreshing: Boolean,
    refreshRotation: Float,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "إشارات التداول",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "آخر تحديث: $lastUpdateTime",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Refresh Button
        IconButton(
            onClick = onRefresh,
            enabled = !isRefreshing,
            modifier = Modifier
                .clip(CircleShape)
                .background(CardBg)
                .border(1.dp, TealBorder, CircleShape)
                .testTag("refresh_button")
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "تحديث الإشارات",
                tint = TealPrimary,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(refreshRotation)
            )
        }
    }
}

@Composable
private fun FixedRiskBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("fixed_risk_banner"),
        color = CardBg,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(GoldGlow),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = "تنبيه المخاطر",
                    tint = GoldAccent,
                    modifier = Modifier.size(15.dp)
                )
            }

            Text(
                text = "الإشارات أداة مساعدة وليست توصية مالية",
                color = GoldAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TabsSection(
    selectedTab: SignalTab,
    allCount: Int,
    liveCount: Int,
    pendingCount: Int,
    onTabSelected: (SignalTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SignalTab.values().forEach { tab ->
            val isSelected = selectedTab == tab
            val count = when (tab) {
                SignalTab.ALL -> allCount
                SignalTab.LIVE -> liveCount
                SignalTab.PENDING -> pendingCount
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) TealPrimary else CardBg)
                    .border(
                        1.dp,
                        if (isSelected) TealPrimary else TealBorder.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 10.dp)
                    .testTag("tab_${tab.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (tab == SignalTab.LIVE) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) DarkBg else RedAccent)
                        )
                    }
                    Text(
                        text = "${tab.labelArabic} ($count)",
                        color = if (isSelected) DarkBg else TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SignalCard(
    signal: Signal,
    onInvestClick: () -> Unit
) {
    val numberFormat = remember { DecimalFormat("#,##0.00") }
    val isBuy = signal.direction == SignalDirection.BUY
    val directionColor = if (isBuy) GreenAccent else RedAccent
    val isLive = signal.status == SignalStatus.LIVE

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (signal.isBestOpportunity) 1.5.dp else 1.dp,
                color = if (signal.isBestOpportunity) GoldAccent else TealBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("signal_card_${signal.id}"),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            // Top Row: Pair Name + Coin Logo, Status (مباشر with red dot or انتظار gray), Direction badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pair & Coin Logo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CoinIcon(symbol = signal.coinSymbol, size = 40.dp)

                    Column {
                        Text(
                            text = signal.pair,
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        // Quality label (جودة أعلى / جودة قياسية)
                        Text(
                            text = signal.quality.labelArabic,
                            color = if (signal.quality == SignalQuality.HIGHER) GoldAccent else TealPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Badges: Status + Direction
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status Badge ("مباشر" with red dot or "انتظار" gray)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isLive) RedBg else DarkBgElevated)
                            .border(1.dp, if (isLive) RedBorder else DividerColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isLive) RedAccent else TextMuted)
                        )
                        Text(
                            text = signal.status.labelArabic,
                            color = if (isLive) RedAccent else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Direction Badge (شراء green / بيع red)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isBuy) GreenBg else RedBg)
                            .border(1.dp, if (isBuy) GreenBorder else RedBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = if (isBuy) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = directionColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = signal.direction.labelArabic,
                            color = directionColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Price & Sparkline Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(InputBg)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$${numberFormat.format(signal.currentPrice)}",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${if (signal.change24h >= 0) "+" else ""}${signal.change24h}% (24h)",
                        color = if (signal.change24h >= 0) GreenAccent else RedAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Small area sparkline chart
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(38.dp)
                ) {
                    SparklineChart(
                        points = signal.sparklinePoints,
                        lineColor = directionColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metrics Grid: Expected Profit %, Stop Loss, Target Price, Expected Duration, Countdown
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkBgElevated)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Row 1: Target Price & Stop Loss
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                 Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "وقف الخسارة (SL):",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "$${numberFormat.format(signal.stopLoss)}",
                            color = RedAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                        text = "الربح حتى الهدف 3:",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        Text(
                        text = "+${numberFormat.format(signal.target3ProfitPercent)}%",
                            color = GreenAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerColor))

                // Targets row: هدف 1 / هدف 2 / هدف 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "هدف 1:", color = TextMuted, fontSize = 11.sp)
                        Text(
                            text = "$${numberFormat.format(signal.target1)}",
                            color = GreenAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "+${numberFormat.format(signal.target1ProfitPercent)}%",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "هدف 2:", color = TextMuted, fontSize = 11.sp)
                        Text(
                            text = "$${numberFormat.format(signal.target2)}",
                            color = GreenAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "+${numberFormat.format(signal.target2ProfitPercent)}%",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "هدف 3:", color = TextMuted, fontSize = 11.sp)
                        Text(
                            text = "$${numberFormat.format(signal.target3)}",
                            color = GreenAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "+${numberFormat.format(signal.target3ProfitPercent)}%",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerColor))

                // Row 2: Duration & Countdown to Candle Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "المدة: ${signal.durationText}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    // "متبقي للدخول" countdown to candle close
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassBottom,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "متبقي للدخول: ${formatCountdown(signal.candleCloseSecondsRemaining)}",
                            color = TealPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Only "مباشر" cards show a button "اقتراح مبلغ الاستثمار"
            if (isLive) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onInvestClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("invest_suggestion_button_${signal.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TealPrimary.copy(alpha = 0.15f),
                        contentColor = TealPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TealBorderStrong),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "اقتراح مبلغ الاستثمار",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun formatCountdown(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.US, "%02d:%02d", m, s)
}
