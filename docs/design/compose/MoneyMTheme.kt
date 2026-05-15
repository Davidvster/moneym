// MoneyMTheme.kt
// Drop this into your shared module (commonMain). Wire up via:
//
//   @Composable
//   fun MoneyMApp() {
//       MoneyMTheme {
//           // your composables here — call LocalMoneyMColors.current /
//           // LocalMoneyMType.current to read tokens.
//       }
//   }
//
// This is a starter — adapt to your actual project structure / Resources setup.

package app.moneym.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Colors ─────────────────────────────────────────────────

@Immutable
data class MoneyMColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val border: Color,
    val borderStrong: Color,
    val divider: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,

    // Accent — green, sparingly used
    val accent: Color = Color(0xFF16A34A),
    val danger: Color = Color(0xFFDC2626),

    // Category palette — same in both modes
    val catHealth:        Color = Color(0xFFC2566B),
    val catEntertainment: Color = Color(0xFF8B6FB0),
    val catSalary:        Color = Color(0xFF4A8E5C),
    val catTransport:     Color = Color(0xFF4F8694),
    val catUtilities:     Color = Color(0xFFB89148),
    val catGroceries:     Color = Color(0xFF7A9572),
    val catEatingOut:     Color = Color(0xFFC97A4F),
    val catRent:          Color = Color(0xFF5A7BA8),
    val catShopping:      Color = Color(0xFFB07089),
    val catOther:         Color = Color(0xFF8A8A8A),
)

val MoneyMLight = MoneyMColors(
    bg            = Color(0xFFFFFFFF),
    surface       = Color(0xFFFAFAFA),
    surface2      = Color(0xFFF4F4F4),
    border        = Color(0xFFECECEC),
    borderStrong  = Color(0xFFD4D4D4),
    divider       = Color(0xFFF0F0F0),
    text          = Color(0xFF0A0A0A),
    text2         = Color(0xFF6B6B6B),
    text3         = Color(0xFFA3A3A3),
)

val MoneyMDark = MoneyMColors(
    bg            = Color(0xFF0A0A0A),
    surface       = Color(0xFF141414),
    surface2      = Color(0xFF1C1C1C),
    border        = Color(0xFF232323),
    borderStrong  = Color(0xFF353535),
    divider       = Color(0xFF1E1E1E),
    text          = Color(0xFFFAFAFA),
    text2         = Color(0xFFA3A3A3),
    text3         = Color(0xFF6B6B6B),
)

val LocalMoneyMColors = staticCompositionLocalOf { MoneyMLight }

// ─── Typography ─────────────────────────────────────────────
// Bundle Geist + Geist Mono .ttf via your platform's resource system
// and assemble them into FontFamily instances. Below is the structure;
// adapt to your Resources lookup.

// expect val Geist: FontFamily
// expect val GeistMono: FontFamily

@Immutable
data class MoneyMType(
    val display:   TextStyle,
    val title1:    TextStyle,
    val title2:    TextStyle,
    val title3:    TextStyle,
    val body:      TextStyle,
    val bodyMono:  TextStyle,
    val caption:   TextStyle,
    val captionMono: TextStyle,
    val micro:     TextStyle,
)

fun moneyMType(geist: FontFamily, geistMono: FontFamily) = MoneyMType(
    display = TextStyle(
        fontFamily = geistMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 56.sp,
        letterSpacing = (-1.6).sp,
        fontFeatureSettings = "tnum",
    ),
    title1 = TextStyle(
        fontFamily = geist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        letterSpacing = (-0.6).sp,
    ),
    title2 = TextStyle(
        fontFamily = geist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = (-0.4).sp,
    ),
    title3 = TextStyle(
        fontFamily = geist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        letterSpacing = (-0.2).sp,
    ),
    body = TextStyle(
        fontFamily = geist,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        letterSpacing = (-0.1).sp,
    ),
    bodyMono = TextStyle(
        fontFamily = geistMono,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        letterSpacing = (-0.2).sp,
        fontFeatureSettings = "tnum",
    ),
    caption = TextStyle(
        fontFamily = geist,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
    ),
    captionMono = TextStyle(
        fontFamily = geistMono,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        fontFeatureSettings = "tnum",
    ),
    micro = TextStyle(
        fontFamily = geist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.08.sp,
    ),
)

val LocalMoneyMType = staticCompositionLocalOf<MoneyMType> {
    error("MoneyMType not provided. Wrap your tree in MoneyMTheme { }.")
}

// ─── Spacing ────────────────────────────────────────────────

@Immutable
data class MoneyMSpace(
    val s1: Dp = 4.dp,
    val s2: Dp = 8.dp,
    val s3: Dp = 12.dp,
    val s4: Dp = 16.dp,
    val s5: Dp = 20.dp,
    val s6: Dp = 24.dp,
    val s8: Dp = 32.dp,
    val s10: Dp = 40.dp,
    val s12: Dp = 48.dp,
)

val LocalMoneyMSpace = staticCompositionLocalOf { MoneyMSpace() }

// ─── Shape / Radius ─────────────────────────────────────────

@Immutable
data class MoneyMRadius(
    val xs:   RoundedCornerShape = RoundedCornerShape(6.dp),
    val sm:   RoundedCornerShape = RoundedCornerShape(8.dp),
    val md:   RoundedCornerShape = RoundedCornerShape(12.dp),
    val lg:   RoundedCornerShape = RoundedCornerShape(16.dp),
    val xl:   RoundedCornerShape = RoundedCornerShape(20.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(50),
)

val LocalMoneyMRadius = staticCompositionLocalOf { MoneyMRadius() }

// ─── Theme entry point ─────────────────────────────────────

@Composable
fun MoneyMTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    type: MoneyMType,    // pass moneyMType(YourGeistFamily, YourGeistMonoFamily)
    content: @Composable () -> Unit,
) {
    val colors = if (isDark) MoneyMDark else MoneyMLight
    CompositionLocalProvider(
        LocalMoneyMColors provides colors,
        LocalMoneyMType   provides type,
        LocalMoneyMSpace  provides MoneyMSpace(),
        LocalMoneyMRadius provides MoneyMRadius(),
    ) {
        content()
    }
}

// ─── Convenience accessors so you can write MM.colors.text etc. ─

object MM {
    val colors: MoneyMColors @Composable get() = LocalMoneyMColors.current
    val type:   MoneyMType   @Composable get() = LocalMoneyMType.current
    val space:  MoneyMSpace  @Composable get() = LocalMoneyMSpace.current
    val radius: MoneyMRadius @Composable get() = LocalMoneyMRadius.current
}

// ─── Category mapping ──────────────────────────────────────

enum class CatIcon { Heart, Film, Banknote, Car, Bolt, Basket, Utensils, Home, Bag, Tag, Gift }

@Immutable
data class CategoryStyle(val color: Color, val icon: CatIcon)

@Composable
fun rememberCategoryStyle(name: String): CategoryStyle {
    val c = MM.colors
    return when (name) {
        "Health"        -> CategoryStyle(c.catHealth,        CatIcon.Heart)
        "Entertainment" -> CategoryStyle(c.catEntertainment, CatIcon.Film)
        "Salary"        -> CategoryStyle(c.catSalary,        CatIcon.Banknote)
        "Transport"     -> CategoryStyle(c.catTransport,     CatIcon.Car)
        "Utilities"     -> CategoryStyle(c.catUtilities,     CatIcon.Bolt)
        "Groceries"     -> CategoryStyle(c.catGroceries,     CatIcon.Basket)
        "Eating out"    -> CategoryStyle(c.catEatingOut,     CatIcon.Utensils)
        "Rent"          -> CategoryStyle(c.catRent,          CatIcon.Home)
        "Shopping"      -> CategoryStyle(c.catShopping,      CatIcon.Bag)
        else            -> CategoryStyle(c.catOther,         CatIcon.Tag)
    }
}
