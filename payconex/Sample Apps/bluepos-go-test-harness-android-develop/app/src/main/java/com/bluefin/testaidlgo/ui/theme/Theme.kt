package com.bluefin.testaidlgo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

enum class ButtonGroup {
    Service,
    Payment,
    Utility,
    Transactions
}

@Immutable
data class AppButtonColors(
    val paymentContainer: Color,
    val onPaymentContainer: Color,
    val utilityContainer: Color,
    val onUtilityContainer: Color,
    val transactionsContainer: Color,
    val onTransactionsContainer: Color
)

private val LightAppButtonColors = AppButtonColors(
    paymentContainer = PaymentGreenLight,
    onPaymentContainer = OnPaymentGreenLight,
    utilityContainer = UtilityYellowLight,
    onUtilityContainer = OnUtilityYellowLight,
    transactionsContainer = TransactionBlueLight,
    onTransactionsContainer = OnTransactionBlueLight
)

private val DarkAppButtonColors = AppButtonColors(
    paymentContainer = PaymentGreenDark,
    onPaymentContainer = OnPaymentGreenDark,
    utilityContainer = UtilityYellowDark,
    onUtilityContainer = OnUtilityYellowDark,
    transactionsContainer = TransactionBlueDark,
    onTransactionsContainer = OnTransactionBlueDark
)

private val LocalAppButtonColors = staticCompositionLocalOf { LightAppButtonColors }

@Composable
fun actionButtonColors(group: ButtonGroup): ButtonColors {
    val appColors = LocalAppButtonColors.current
    val (containerColor, contentColor) = when (group) {
        ButtonGroup.Service -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        ButtonGroup.Payment -> appColors.paymentContainer to appColors.onPaymentContainer
        ButtonGroup.Utility -> appColors.utilityContainer to appColors.onUtilityContainer
        ButtonGroup.Transactions -> appColors.transactionsContainer to appColors.onTransactionsContainer
    }

    return ButtonDefaults.buttonColors(
        containerColor = containerColor,
        contentColor = contentColor
    )
}

@Composable
fun TestAIDLTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val appButtonColors = if (darkTheme) DarkAppButtonColors else LightAppButtonColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
    ) {
        CompositionLocalProvider(
            LocalAppButtonColors provides appButtonColors,
            content = content
        )
    }
}
