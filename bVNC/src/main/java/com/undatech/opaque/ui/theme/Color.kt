package com.undatech.opaque.ui.theme

import androidx.compose.ui.graphics.Color

// Brand palette seeded from the existing app teal (#167C80).
val BrandTeal = Color(0xFF167C80)
val BrandTealDark = Color(0xFF0D5C62)
val BrandTealLight = Color(0xFF4FB3B8)

// Status accents (shared by both schemes).
val RunningGreen = Color(0xFF3DD68C)
val StoppedGrey = Color(0xFF8A929E)

// --- Dark scheme: RustDesk-style neutral slate with a teal accent. ----------------
val DarkPrimary = BrandTealLight
val DarkOnPrimary = Color(0xFF00292B)
val DarkPrimaryContainer = Color(0xFF005054)
val DarkOnPrimaryContainer = Color(0xFFB6ECEE)
val DarkSecondary = Color(0xFFB1CCCD)
val DarkOnSecondary = Color(0xFF1B3436)

val DarkBackground = Color(0xFF15171C)   // app canvas — deep slate
val DarkOnBackground = Color(0xFFE6E8EB)
val DarkSurface = Color(0xFF1B1E24)       // base surface
val DarkOnSurface = Color(0xFFE6E8EB)
val DarkSurfaceVariant = Color(0xFF2A2F38)
val DarkOnSurfaceVariant = Color(0xFF9AA2AE)
val DarkOutline = Color(0xFF3A404B)
val DarkOutlineVariant = Color(0xFF2A2F38)

// Tonal "container" surfaces for cards / sheets (RustDesk panels get progressively lighter).
val DarkSurfaceContainerLowest = Color(0xFF101216)
val DarkSurfaceContainerLow = Color(0xFF1B1E24)
val DarkSurfaceContainer = Color(0xFF21252C)
val DarkSurfaceContainerHigh = Color(0xFF282D35)
val DarkSurfaceContainerHighest = Color(0xFF323843)

// --- Light scheme: clean teal-tinted neutral. -------------------------------------
val LightPrimary = BrandTeal
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFB6ECEE)
val LightOnPrimaryContainer = Color(0xFF002021)
val LightSecondary = Color(0xFF4A6364)
val LightOnSecondary = Color(0xFFFFFFFF)

val LightBackground = Color(0xFFF4F7F8)
val LightOnBackground = Color(0xFF181C1D)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF181C1D)
val LightSurfaceVariant = Color(0xFFE3EAEB)
val LightOnSurfaceVariant = Color(0xFF566060)
val LightOutline = Color(0xFFC2CBCC)
val LightOutlineVariant = Color(0xFFDAE3E3)

val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF7FAFA)
val LightSurfaceContainer = Color(0xFFEFF3F3)
val LightSurfaceContainerHigh = Color(0xFFE8EEEE)
val LightSurfaceContainerHighest = Color(0xFFE1E8E8)
