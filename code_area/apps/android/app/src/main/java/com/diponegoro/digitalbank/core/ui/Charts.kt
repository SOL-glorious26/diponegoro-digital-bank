package com.diponegoro.digitalbank.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diponegoro.digitalbank.core.util.formatRupiah
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** "Rp12,5 jt" — label sumbu ringkas. */
fun compactRupiah(value: Long): String {
    val a = abs(value)
    val sign = if (value < 0) "-" else ""
    return when {
        a >= 1_000_000_000 -> "${sign}Rp${"%.1f".format(a / 1e9).replace('.', ',')} M"
        a >= 1_000_000 -> "${sign}Rp${"%.1f".format(a / 1e6).replace('.', ',')} jt"
        a >= 1_000 -> "${sign}Rp${a / 1_000} rb"
        else -> "${sign}Rp$a"
    }
}

data class ChartPoint(val label: String, val value: Long)

private const val PLOT_LEFT_DP = 56
private const val PLOT_BOTTOM_DP = 30

/**
 * Grafik garis satu seri (saldo). Ketuk grafik untuk melihat nilai titik terdekat.
 * Satu seri → tanpa legenda, judul kartu yang menamai seri.
 */
@Composable
fun LineChart(points: List<ChartPoint>, color: Color, modifier: Modifier = Modifier) {
    if (points.isEmpty()) return
    var selected by remember(points) { mutableStateOf(points.lastIndex) }
    val measurer = rememberTextMeasurer()
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surface
    val labelStyle = TextStyle(fontSize = 11.sp, color = labelColor)

    val minV = points.minOf { it.value }.let { min(it, 0L).takeIf { v -> v < 0 } ?: (it * 0.9).toLong() }
    val maxV = max(points.maxOf { it.value }, minV + 1)

    Column(modifier) {
        SelectionReadout(points[selected].label, listOf(null to formatRupiah(points[selected].value)))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .semantics { contentDescription = "Grafik saldo. ${points.joinToString { "${it.label}: ${formatRupiah(it.value)}" }}" }
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val left = PLOT_LEFT_DP.dp.toPx()
                        val step = (size.width - left) / max(1, points.size - 1)
                        selected = ((offset.x - left) / step).let { kotlin.math.round(it).toInt() }.coerceIn(0, points.lastIndex)
                    }
                }
        ) {
            val left = PLOT_LEFT_DP.dp.toPx()
            val bottom = size.height - PLOT_BOTTOM_DP.dp.toPx()
            val top = 8.dp.toPx()
            fun y(v: Long) = bottom - (v - minV).toFloat() / (maxV - minV) * (bottom - top)
            val step = (size.width - left) / max(1, points.size - 1)
            fun x(i: Int) = left + i * step

            drawGrid(minV, maxV, left, top, bottom, axisColor, labelStyle, measurer)

            val path = Path()
            points.forEachIndexed { i, p -> if (i == 0) path.moveTo(x(i), y(p.value)) else path.lineTo(x(i), y(p.value)) }
            drawPath(path, color, style = Stroke(width = 2.dp.toPx()))

            // Crosshair + penanda titik terpilih (≥8px, cincin warna permukaan).
            val sx = x(selected)
            drawLine(axisColor, Offset(sx, top), Offset(sx, bottom), strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
            drawCircle(surface, radius = 6.dp.toPx(), center = Offset(sx, y(points[selected].value)))
            drawCircle(color, radius = 4.dp.toPx(), center = Offset(sx, y(points[selected].value)))

            drawXLabels(points.map { it.label }, { x(it) }, bottom, measurer, labelStyle)
        }
    }
}

/**
 * Grafik batang berkelompok (pemasukan vs pengeluaran per bulan). Dua seri → legenda selalu tampil.
 */
@Composable
fun GroupedBarChart(
    labels: List<String>,
    series: List<Pair<String, List<Long>>>,
    colors: List<Color>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (labels.isEmpty()) return
    val measurer = rememberTextMeasurer()
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val highlight = MaterialTheme.colorScheme.surfaceVariant
    val labelStyle = TextStyle(fontSize = 11.sp, color = labelColor)
    val maxV = max(1L, series.maxOf { s -> s.second.maxOrNull() ?: 0L })

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Legend(series.map { it.first }.zip(colors))
        SelectionReadout(
            labels[selectedIndex],
            series.mapIndexed { i, s -> colors[i] to "${s.first}: ${formatRupiah(s.second[selectedIndex])}" }
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .semantics {
                    contentDescription = "Grafik pemasukan dan pengeluaran. " + labels.indices.joinToString { i ->
                        "${labels[i]}: " + series.joinToString { "${it.first} ${formatRupiah(it.second[i])}" }
                    }
                }
                .pointerInput(labels) {
                    detectTapGestures { offset ->
                        val left = PLOT_LEFT_DP.dp.toPx()
                        val group = (size.width - left) / labels.size
                        onSelect(((offset.x - left) / group).toInt().coerceIn(0, labels.lastIndex))
                    }
                }
        ) {
            val left = PLOT_LEFT_DP.dp.toPx()
            val bottom = size.height - PLOT_BOTTOM_DP.dp.toPx()
            val top = 8.dp.toPx()
            val group = (size.width - left) / labels.size
            val gap = 2.dp.toPx()
            val barWidth = min(28.dp.toPx(), (group * 0.7f - gap) / series.size)

            // Sorotan kelompok terpilih (hit target = seluruh kolom).
            drawRect(highlight, Offset(left + selectedIndex * group, top), Size(group, bottom - top))
            drawGrid(0, maxV, left, top, bottom, axisColor, labelStyle, measurer)

            labels.indices.forEach { i ->
                val groupWidth = barWidth * series.size + gap * (series.size - 1)
                var x = left + i * group + (group - groupWidth) / 2
                series.forEachIndexed { s, (_, values) ->
                    val h = (values[i].toFloat() / maxV) * (bottom - top)
                    if (h > 0f) drawTopRoundedBar(colors[s], x, bottom - h, barWidth, h, 4.dp.toPx())
                    x += barWidth + gap
                }
            }
            drawXLabels(labels, { left + it * group + group / 2 }, bottom, measurer, labelStyle)
        }
    }
}

@Composable
fun Legend(items: List<Pair<String, Color>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        items.forEach { (name, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
                Spacer(Modifier.width(6.dp))
                Text(name, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SelectionReadout(title: String, values: List<Pair<Color?, String>>) {
    Column {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        values.forEach { (color, text) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (color != null) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
                    Spacer(Modifier.width(6.dp))
                }
                Text(text, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

private fun DrawScope.drawTopRoundedBar(color: Color, x: Float, y: Float, w: Float, h: Float, r: Float) {
    val radius = min(r, min(w / 2, h))
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = x, top = y, right = x + w, bottom = y + h,
                topLeftCornerRadius = CornerRadius(radius), topRightCornerRadius = CornerRadius(radius),
                bottomLeftCornerRadius = CornerRadius.Zero, bottomRightCornerRadius = CornerRadius.Zero
            )
        )
    }
    drawPath(path, color)
}

private fun DrawScope.drawGrid(
    minV: Long, maxV: Long, left: Float, top: Float, bottom: Float, color: Color,
    style: TextStyle, measurer: androidx.compose.ui.text.TextMeasurer
) {
    val ticks = 3
    for (t in 0..ticks) {
        val v = minV + (maxV - minV) * t / ticks
        val y = bottom - (bottom - top) * t / ticks
        drawLine(color, Offset(left, y), Offset(size.width, y), strokeWidth = 1f)
        val layout = measurer.measure(compactRupiah(v), style)
        drawText(layout, topLeft = Offset(left - layout.size.width - 6f, y - layout.size.height / 2))
    }
}

private fun DrawScope.drawXLabels(
    labels: List<String>, xOf: (Int) -> Float, bottom: Float,
    measurer: androidx.compose.ui.text.TextMeasurer, style: TextStyle
) {
    // Tampilkan label secukupnya agar tidak bertumpuk: jarak antar-label dihitung dari lebar
    // label terlebar, dan dihitung mundur dari label terakhir (titik terbaru selalu tampil).
    val layouts = labels.map { measurer.measure(it, style) }
    val gap = 8.dp.toPx()
    val widest = layouts.maxOf { it.size.width } + gap
    val slot = if (labels.size > 1) kotlin.math.abs(xOf(1) - xOf(0)) else Float.MAX_VALUE
    val every = max(1, kotlin.math.ceil(widest / slot).toInt())
    // Di bawah label sumbu-Y terbawah (berpusat di garis dasar) supaya tidak bertabrakan.
    val labelTop = bottom + layouts.first().size.height / 2f + 2.dp.toPx()
    // Digambar dari kanan; label yang digeser agar tidak keluar tepi bisa menabrak tetangganya,
    // jadi label yang tumpang-tindih dengan label yang sudah digambar dilewati.
    var nextLeft = Float.MAX_VALUE
    for (i in labels.lastIndex downTo 0) {
        if ((labels.lastIndex - i) % every != 0) continue
        val layout = layouts[i]
        val x = (xOf(i) - layout.size.width / 2).coerceIn(0f, size.width - layout.size.width)
        if (x + layout.size.width + gap > nextLeft) continue
        drawText(layout, topLeft = Offset(x, labelTop))
        nextLeft = x
    }
}
