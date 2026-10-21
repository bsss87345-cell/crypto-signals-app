package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CoinIcon(
    symbol: String,
    size: Dp = 42.dp,
    modifier: Modifier = Modifier
) {
    val (backgroundColors, textColor, displayText) = when (symbol.uppercase()) {
        "BTC" -> Triple(
            listOf(Color(0xFFF7931A), Color(0xFFFFAB40)),
            Color.White,
            "₿"
        )
        "ETH" -> Triple(
            listOf(Color(0xFF627EEA), Color(0xFF3B5998)),
            Color.White,
            "Ξ"
        )
        "SOL" -> Triple(
            listOf(Color(0xFF14F195), Color(0xFF9945FF)),
            Color.Black,
            "◎"
        )
        "AVAX" -> Triple(
            listOf(Color(0xFFE84142), Color(0xFFFF5252)),
            Color.White,
            "▲"
        )
        "LINK" -> Triple(
            listOf(Color(0xFF375BD2), Color(0xFF2A5ADA)),
            Color.White,
            "⬡"
        )
        "BNB" -> Triple(
            listOf(Color(0xFFF3BA2F), Color(0xFFFFD54F)),
            Color.Black,
            "🔶"
        )
        else -> Triple(
            listOf(Color(0xFF00E5CC), Color(0xFF00B0FF)),
            Color.Black,
            symbol.take(2)
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(backgroundColors))
            .border(1.5.dp, Color(0x33FFFFFF), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayText,
            color = textColor,
            fontSize = (size.value * 0.45).sp,
            fontWeight = FontWeight.Bold
        )
    }
}
