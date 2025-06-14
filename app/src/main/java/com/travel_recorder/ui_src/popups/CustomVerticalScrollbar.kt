package com.travel_recorder.ui_src.popups

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.travel_recorder.R

@Composable
fun Modifier.verticalColumnScrollbar(
    scrollState: ScrollState,
    width: Dp = 4.dp,
    scrollBarCornerRadius: Float = 4f,
    rightPadding: Float = 12f
): Modifier {
    val primeColor = colorResource(id = R.color.brown)
    val backColor = colorResource(id = R.color.light_brown)
    return drawWithContent {
        drawContent()
        val totalContentHeight = scrollState.maxValue.toFloat() + this.size.height
        val scrollBarStartOffset = scrollState.value.toFloat() * this.size.height / totalContentHeight
        drawRoundRect(
            cornerRadius = CornerRadius(scrollBarCornerRadius),
            color = backColor,
            topLeft = Offset(this.size.width - rightPadding, 0f),
            size = Size(width.toPx(), this.size.height),
        )
        drawRoundRect(
            cornerRadius = CornerRadius(scrollBarCornerRadius),
            color = primeColor,
            topLeft = Offset(this.size.width - rightPadding, scrollBarStartOffset),
            size = Size(width.toPx(), this.size.height * this.size.height / totalContentHeight)
        )
    }
}