package com.shadowsleuth.app.ui.theme

import androidx.compose.ui.graphics.Color

// ShadowSleuth — Flat Design Color System v1.2
// Philosophy: solid fills, no gradients, high contrast, intentional hierarchy.

// ── Brand colors ──────────────────────────────────────────────────────────────
// Primary: Electric Cyan — forensic, precise, trustworthy
val CyanPrimary   = Color(0xFF00BFFF)   // Deep Sky Blue — punchy flat primary
val CyanLight     = Color(0xFF67D8F7)   // Light variant for chips / containers
val CyanDark      = Color(0xFF0090C8)   // Pressed / active state
val CyanOnPrimary = Color(0xFF001E2E)   // Text / icons on cyan buttons

// Secondary: Soft Violet — analytical, sophisticated
val VioletPrimary = Color(0xFF7C5CFC)
val VioletLight   = Color(0xFFBBA8FF)
val VioletDark    = Color(0xFF5637D4)
val VioletOn      = Color(0xFF1A0A55)

// Tertiary / Warning: Amber
val AmberPrimary  = Color(0xFFFFA726)
val AmberLight    = Color(0xFFFFD180)
val AmberDark     = Color(0xFFE65100)
val AmberOn       = Color(0xFF2D1500)

// Error / Danger: Vivid Red
val ErrorPrimary  = Color(0xFFFF4646)
val ErrorLight    = Color(0xFFFFCDD2)
val ErrorDark     = Color(0xFFB71C1C)
val ErrorOn       = Color(0xFF410002)

// ── Dark Theme surfaces ───────────────────────────────────────────────────────
val DarkBg        = Color(0xFF0C1017)   // Near-black, pure flat
val DarkSurface   = Color(0xFF141A24)   // Card / sheet surface
val DarkSurface2  = Color(0xFF1C2535)   // Slightly elevated surface
val DarkSurface3  = Color(0xFF253043)   // Input / chip fill
val DarkOutline   = Color(0xFF2E3D54)   // Subtle border
val DarkDivider   = Color(0xFF1E2C3F)   // HorizontalDivider

val DarkOnBg      = Color(0xFFF0F4FA)   // Primary text on dark bg
val DarkOnMuted   = Color(0xFF8FA3BF)   // Secondary / muted text
val DarkOnSubtle  = Color(0xFF546880)   // Placeholder / disabled

// ── Light Theme surfaces ──────────────────────────────────────────────────────
val LightBg       = Color(0xFFF5F8FC)   // Page background — cool white
val LightSurface  = Color(0xFFFFFFFF)   // Card surface
val LightSurface2 = Color(0xFFEDF2F8)   // Slightly off-white container
val LightSurface3 = Color(0xFFE1EAF5)   // Chip fill / input bg
val LightOutline  = Color(0xFFCDD8E8)   // Border
val LightDivider  = Color(0xFFE5ECF6)   // HorizontalDivider

val LightOnBg     = Color(0xFF0D1B2E)   // Primary text
val LightOnMuted  = Color(0xFF4E6580)   // Secondary text
val LightOnSubtle = Color(0xFF8FA3B8)   // Placeholder / disabled

// ── M3 Light tokens ───────────────────────────────────────────────────────────
val md_theme_light_primary            = CyanDark
val md_theme_light_onPrimary          = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer   = CyanLight
val md_theme_light_onPrimaryContainer = CyanOnPrimary
val md_theme_light_secondary          = VioletPrimary
val md_theme_light_onSecondary        = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = VioletLight
val md_theme_light_onSecondaryContainer = VioletOn
val md_theme_light_tertiary           = AmberPrimary
val md_theme_light_onTertiary         = AmberOn
val md_theme_light_tertiaryContainer  = AmberLight
val md_theme_light_onTertiaryContainer = AmberOn
val md_theme_light_error              = ErrorDark
val md_theme_light_onError            = Color(0xFFFFFFFF)
val md_theme_light_errorContainer     = ErrorLight
val md_theme_light_onErrorContainer   = ErrorOn
val md_theme_light_background         = LightBg
val md_theme_light_onBackground       = LightOnBg
val md_theme_light_surface            = LightSurface
val md_theme_light_onSurface          = LightOnBg
val md_theme_light_surfaceVariant     = LightSurface2
val md_theme_light_onSurfaceVariant   = LightOnMuted
val md_theme_light_outline            = LightOutline
val md_theme_light_outlineVariant     = LightDivider

// ── M3 Dark tokens ────────────────────────────────────────────────────────────
val md_theme_dark_primary             = CyanPrimary
val md_theme_dark_onPrimary           = CyanOnPrimary
val md_theme_dark_primaryContainer    = CyanDark
val md_theme_dark_onPrimaryContainer  = CyanLight
val md_theme_dark_secondary           = VioletLight
val md_theme_dark_onSecondary         = VioletOn
val md_theme_dark_secondaryContainer  = VioletDark
val md_theme_dark_onSecondaryContainer = VioletLight
val md_theme_dark_tertiary            = AmberPrimary
val md_theme_dark_onTertiary          = AmberOn
val md_theme_dark_tertiaryContainer   = AmberDark
val md_theme_dark_onTertiaryContainer = AmberLight
val md_theme_dark_error               = ErrorPrimary
val md_theme_dark_onError             = ErrorOn
val md_theme_dark_errorContainer      = ErrorDark
val md_theme_dark_onErrorContainer    = ErrorLight
val md_theme_dark_background          = DarkBg
val md_theme_dark_onBackground        = DarkOnBg
val md_theme_dark_surface             = DarkSurface
val md_theme_dark_onSurface           = DarkOnBg
val md_theme_dark_surfaceVariant      = DarkSurface2
val md_theme_dark_onSurfaceVariant    = DarkOnMuted
val md_theme_dark_outline             = DarkOutline
val md_theme_dark_outlineVariant      = DarkDivider
