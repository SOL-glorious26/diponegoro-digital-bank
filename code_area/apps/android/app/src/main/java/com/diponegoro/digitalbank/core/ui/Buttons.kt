package com.diponegoro.digitalbank.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.diponegoro.digitalbank.ui.theme.DdbTheme

/*
 * Tombol bersama dengan bentuk design system: sudut 10 dp (MaterialTheme.shapes.small), tinggi
 * minimum 48 dp, tanpa bayangan. Layar memakainya lewat import alias, mis.
 *   import com.diponegoro.digitalbank.core.ui.DdbButton as Button
 * sehingga pemanggilan Button(...) / OutlinedButton(...) di layar tidak perlu diubah.
 */

private val ButtonPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)

/** Tombol utama: isi warna tinta (navy di mode terang, gading di mode gelap). */
@Composable
fun DdbButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonPadding,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = null,
        border = border,
        contentPadding = contentPadding,
        content = content
    )
}

/** Tombol sekunder: tanpa isi, garis tipis, teks warna tinta. */
@Composable
fun DdbOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
    border: BorderStroke? = BorderStroke(1.dp, DdbTheme.colors.lineStrong),
    contentPadding: PaddingValues = ButtonPadding,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = shape,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        content = content
    )
}
