@file:OptIn(ExperimentalTextApi::class)

package com.expressit.journal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.expressit.journal.R

private fun interVariable(weight: FontWeight) = Font(
    resId = R.font.inter,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

private fun loraVariable(weight: FontWeight, style: FontStyle = FontStyle.Normal) = Font(
    resId = if (style == FontStyle.Italic) R.font.lora_italic else R.font.lora,
    weight = weight,
    style = style,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

/** Body / UI face — quiet, highly legible. */
val Inter = FontFamily(
    interVariable(FontWeight.Normal),
    interVariable(FontWeight.Medium),
    interVariable(FontWeight.SemiBold),
    interVariable(FontWeight.Bold)
)

/** Display face — a literary serif that gives the app its diary voice. */
val Lora = FontFamily(
    loraVariable(FontWeight.Normal),
    loraVariable(FontWeight.Medium),
    loraVariable(FontWeight.SemiBold),
    loraVariable(FontWeight.Bold),
    loraVariable(FontWeight.Normal, FontStyle.Italic),
    loraVariable(FontWeight.Medium, FontStyle.Italic)
)

val ExpressItTypography = Typography(
    // Month title, screen titles
    displaySmall = TextStyle(
        fontFamily = Lora,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.01).em
    ),
    headlineMedium = TextStyle(
        fontFamily = Lora,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 34.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Lora,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 30.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.01.em
    ),
    titleSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // Journal text is set in the serif for a book-like reading feel
    bodyLarge = TextStyle(
        fontFamily = Lora,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.01.em
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.02.em
    ),
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.04.em
    ),
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.06.em
    )
)
