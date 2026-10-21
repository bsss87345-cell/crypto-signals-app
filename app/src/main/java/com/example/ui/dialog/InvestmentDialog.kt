package com.example.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Signal
import com.example.model.SignalQuality
import com.example.ui.theme.CardBg
import com.example.ui.theme.DarkBg
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldGlow
import com.example.ui.theme.GreenAccent
import com.example.ui.theme.InputBg
import com.example.ui.theme.RedAccent
import com.example.ui.theme.TealBorder
import com.example.ui.theme.TealBorderStrong
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.DecimalFormat

@Composable
fun InvestmentDialog(
    signal: Signal,
    currentCapital: Double,
    onCapitalChanged: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var capitalInput by remember(currentCapital) {
        val defaultVal = if (currentCapital > 0) {
            if (currentCapital % 1.0 == 0.0) currentCapital.toLong().toString() else currentCapital.toString()
        } else "10000"
        mutableStateOf(defaultVal)
    }

    val parsedCapital = capitalInput.toDoubleOrNull() ?: currentCapital.coerceAtLeast(100.0)
    val suggestion = remember(parsedCapital, signal) {
        signal.calculateInvestment(parsedCapital)
    }

    val numberFormat = remember { DecimalFormat("#,##0.00") }
    val wholeFormat = remember { DecimalFormat("#,##0") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.5.dp, TealBorderStrong, RoundedCornerShape(20.dp))
                .testTag("investment_dialog"),
            color = CardBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with title and close icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(TealPrimary.copy(alpha = 0.15f))
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = "حاسبة الاستثمار",
                                tint = TealPrimary,
                                modifier = Modifier.height(20.dp).width(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "اقتراح مبلغ الاستثمار",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${signal.pair} • ${signal.quality.labelArabic}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dialog_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quality & Risk badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (signal.quality == SignalQuality.HIGHER) GoldGlow else TealBorder.copy(alpha = 0.15f))
                        .border(
                            1.dp,
                            if (signal.quality == SignalQuality.HIGHER) GoldBorder else TealBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (signal.quality == SignalQuality.HIGHER) GoldAccent else TealPrimary,
                            modifier = Modifier.height(16.dp).width(16.dp)
                        )
                        Text(
                            text = "مستوى المخاطرة المعياري:",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = if (signal.quality == SignalQuality.HIGHER) "1.0% من رأس المال" else "0.5% من رأس المال",
                        color = if (signal.quality == SignalQuality.HIGHER) GoldAccent else TealPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Capital Input Field
                Text(
                    text = "إجمالي رأس المال (USDT):",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = capitalInput,
                    onValueChange = { newValue ->
                        val filtered = newValue.filter { it.isDigit() || it == '.' }
                        capitalInput = filtered
                        filtered.toDoubleOrNull()?.let { newCap ->
                            if (newCap > 0) onCapitalChanged(newCap)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("capital_input_field"),
                    placeholder = { Text("مثال: 10000", color = TextMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = InputBg,
                        unfocusedContainerColor = InputBg,
                        focusedBorderColor = TealPrimary,
                        unfocusedBorderColor = TealBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        Text(
                            text = "USDT",
                            color = TealPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Capital Selection Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1000.0, 5000.0, 10000.0, 25000.0).forEach { amount ->
                        val isSelected = parsedCapital == amount
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) TealPrimary.copy(alpha = 0.2f) else DarkBg)
                                .border(
                                    1.dp,
                                    if (isSelected) TealPrimary else TealBorder.copy(alpha = 0.4f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    capitalInput = amount.toLong().toString()
                                    onCapitalChanged(amount)
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${amount.toLong() / 1000}k",
                                color = if (isSelected) TealPrimary else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Suggested calculation results
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkBg),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TealBorder))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Main Suggested Amount
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "المبلغ المقترح للاستثمار:",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                                if (suggestion.isCapped) {
                                    Text(
                                        text = "(تم تطبيق حد أقصى 20% من المحفظة)",
                                        color = GoldAccent,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Text(
                                text = "${wholeFormat.format(suggestion.suggestedAmount)} USDT",
                                color = TealPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CardBg))

                        // % of capital
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "النسبة من إجمالي رأس المال:",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${numberFormat.format(suggestion.percentOfCapital)}%",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Max loss
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "أقصى خسارة محتملة (وقف الخسارة):",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${numberFormat.format(suggestion.maxLossAmount)} USDT",
                                color = RedAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // SL distance
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مسافة وقف الخسارة:",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${numberFormat.format(suggestion.stopLossDistancePercent)}%",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Required disclaimer line: "تقدير لإدارة المخاطر وليس توصية مالية"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBg.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.height(14.dp).width(14.dp)
                    )
                    Text(
                        text = "تقدير لإدارة المخاطر وليس توصية مالية",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Start
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Done Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("dialog_done_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TealPrimary,
                        contentColor = DarkBg
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "تم",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
