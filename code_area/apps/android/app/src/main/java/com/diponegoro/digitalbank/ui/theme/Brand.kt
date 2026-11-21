package com.diponegoro.digitalbank.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.diponegoro.digitalbank.R

/**
 * Tanda logo Diponegoro (D + kepala kuda) dari logo asli, diwarnai sesuai mode:
 * navy di mode terang, emas di mode gelap. Rasio asli 630 × 720.
 */
@Composable
fun DdbMark(height: Dp, modifier: Modifier = Modifier, tint: Color = DdbTheme.colors.mark, contentDescription: String? = "Logo Diponegoro") {
    Image(
        painter = painterResource(R.drawable.ddb_mark),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(width = height * (630f / 720f), height = height)
    )
}

/** Garis emas pendek: satu-satunya aksen emas di sebagian besar layar. */
@Composable
fun GoldRule(modifier: Modifier = Modifier, width: Dp = 24.dp) {
    Box(modifier.width(width).height(1.dp).background(DdbTheme.colors.gold))
}
