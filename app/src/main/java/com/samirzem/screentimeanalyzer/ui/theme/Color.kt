package com.samirzem.screentimeanalyzer.ui.theme

import androidx.compose.ui.graphics.Color

// Brand palette - deliberately vibrant against a near-black background so the
// app reads as its own thing rather than a generic Material screen.
val Teal = Color(0xFF17E8C6)
val TealDark = Color(0xFF049C86)
val Coral = Color(0xFFFF8A65)
val CoralDark = Color(0xFFD8562F)
val Amber = Color(0xFFFFCB61)
val Periwinkle = Color(0xFF8C9EFF)
val Orchid = Color(0xFFE28BE0)

// Dark theme surfaces: a near-black base with a faint indigo tint, and a
// visibly lighter "card" tone so content reads as layered, not flat.
val BackgroundDark = Color(0xFF121018)
val SurfaceDark = Color(0xFF19161F)
val SurfaceVariantDark = Color(0xFF262231)
val OnSurfaceDark = Color(0xFFF1EEF7)
val OnSurfaceVariantDark = Color(0xFFC7C0D6)
val OutlineDark = Color(0xFF4A4459)

// Light theme surfaces, same structure.
val BackgroundLight = Color(0xFFFBFAFE)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF0EDF6)
val OnSurfaceLight = Color(0xFF1C1A22)
val OnSurfaceVariantLight = Color(0xFF52495F)
val OutlineLight = Color(0xFFD8D0E4)

/** Semantic color for "more than before" - used on trend pills, not just charts. */
val TrendUp = CoralDark
val TrendUpContainer = Color(0xFF3A241E)
val TrendUpOnContainer = Color(0xFFFFB59A)

/** Semantic color for "less than before". */
val TrendDown = TealDark
val TrendDownContainer = Color(0xFF12332D)
val TrendDownOnContainer = Color(0xFF6FF0D6)

val SeriesColors = listOf(Teal, Coral, Amber, Periwinkle, Orchid, TealDark)
