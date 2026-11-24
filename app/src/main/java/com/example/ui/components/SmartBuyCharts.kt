package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.domain.BasketEvaluation
import com.example.domain.BuyhatkeObservation
import com.example.ui.theme.AlertRose
import com.example.ui.theme.AmberTertiary
import com.example.ui.theme.DealGreen

@Composable
fun PriceHistoryCanvasChart(
  observations: List<BuyhatkeObservation>,
  modifier: Modifier = Modifier,
) {
  val primaryColor = MaterialTheme.colorScheme.primary
  val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
  val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

  if (observations.isEmpty()) {
    Text(
      text = "No observations available for this category.",
      style = MaterialTheme.typography.bodyMedium,
      color = labelColor,
      modifier = modifier.padding(16.dp),
    )
    return
  }

  val prices = observations.map { it.currentPrice }
  val minP = (prices.minOrNull() ?: 0.0) * 0.96
  val maxP = (prices.maxOrNull() ?: 1.0) * 1.04
  val range = (maxP - minP).coerceAtLeast(1.0)

  Column(modifier = modifier.fillMaxWidth()) {
    Canvas(
      modifier =
        Modifier.fillMaxWidth()
          .height(170.dp)
          .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
      val w = size.width
      val h = size.height

      // Horizontal reference grid lines
      for (i in 0..3) {
        val y = h * (i / 3f)
        drawLine(color = gridColor, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1.dp.toPx())
      }

      val points =
        observations.mapIndexed { idx, obs ->
          val x =
            if (observations.size == 1) w / 2f
            else (idx.toFloat() / (observations.size - 1)) * w
          val normY = ((obs.currentPrice - minP) / range).toFloat()
          val y = h - (normY * h)
          Offset(x, y)
        }

      if (points.size >= 2) {
        val fillPath =
          Path().apply {
            moveTo(points.first().x, h)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, h)
            close()
          }
        drawPath(
          path = fillPath,
          brush =
            Brush.verticalGradient(
              colors = listOf(primaryColor.copy(alpha = 0.28f), Color.Transparent)
            ),
        )

        val linePath =
          Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
          }
        drawPath(
          path = linePath,
          color = primaryColor,
          style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
      }

      points.forEachIndexed { idx, pt ->
        drawCircle(color = primaryColor, radius = 5.dp.toPx(), center = pt)
        drawCircle(
          color = if (idx == points.lastIndex) AmberTertiary else Color.White,
          radius = 2.5.dp.toPx(),
          center = pt,
        )
      }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      observations.forEach { obs ->
        Text(
          text = obs.observationDate.takeLast(5),
          style = MaterialTheme.typography.labelSmall,
          color = labelColor,
        )
      }
    }
  }
}

@Composable
fun BasketComparisonCanvasChart(
  baskets: List<BasketEvaluation>,
  modifier: Modifier = Modifier,
) {
  if (baskets.isEmpty()) return
  val maxCost = (baskets.maxOfOrNull { it.finalPayableCost } ?: 1.0).coerceAtLeast(1.0)
  val primaryColor = MaterialTheme.colorScheme.primary
  val trackColor = MaterialTheme.colorScheme.surfaceVariant

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    baskets.forEach { basket ->
      val barColor = if (basket.isCompleteBasket) DealGreen else AlertRose
      val fraction = (basket.finalPayableCost / maxCost).toFloat().coerceIn(0.08f, 1f)
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(
            text = basket.platform.replace(" [SAMPLE]", ""),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "₹${basket.finalPayableCost.toInt()} · ${basket.deliveryTimeMins}m",
            style = MaterialTheme.typography.labelMedium,
            color = if (basket.isCompleteBasket) primaryColor else AlertRose,
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(12.dp)) {
          drawRoundRect(
            color = trackColor,
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
          )
          drawRoundRect(
            color = barColor,
            size = Size(size.width * fraction, size.height),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
          )
        }
      }
    }
  }
}
