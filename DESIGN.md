---
name: Precision Purge
colors:
  surface: '#f7f9fc'
  surface-dim: '#d8dadd'
  surface-bright: '#f7f9fc'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f4f7'
  surface-container: '#eceef1'
  surface-container-high: '#e6e8eb'
  surface-container-highest: '#e0e3e6'
  on-surface: '#191c1e'
  on-surface-variant: '#434652'
  inverse-surface: '#2d3133'
  inverse-on-surface: '#eff1f4'
  outline: '#737783'
  outline-variant: '#c3c6d4'
  surface-tint: '#2b5bb5'
  primary: '#003178'
  on-primary: '#ffffff'
  primary-container: '#0d47a1'
  on-primary-container: '#a1bbff'
  inverse-primary: '#b0c6ff'
  secondary: '#486173'
  on-secondary: '#ffffff'
  secondary-container: '#c9e3f9'
  on-secondary-container: '#4d6678'
  tertiary: '#003b43'
  on-tertiary: '#ffffff'
  tertiary-container: '#00545e'
  on-tertiary-container: '#7bc8d5'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d9e2ff'
  primary-fixed-dim: '#b0c6ff'
  on-primary-fixed: '#001945'
  on-primary-fixed-variant: '#00429c'
  secondary-fixed: '#cbe6fb'
  secondary-fixed-dim: '#b0cadf'
  on-secondary-fixed: '#011e2e'
  on-secondary-fixed-variant: '#314a5b'
  tertiary-fixed: '#a2effd'
  tertiary-fixed-dim: '#85d2e0'
  on-tertiary-fixed: '#001f24'
  on-tertiary-fixed-variant: '#004f58'
  background: '#f7f9fc'
  on-background: '#191c1e'
  surface-variant: '#e0e3e6'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-sm:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
  label-sm:
    fontFamily: JetBrains Mono
    fontSize: 10px
    fontWeight: '500'
    lineHeight: 14px
  headline-md-mobile:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  edge-margin: 16px
  grid-gutter: 12px
---

## Brand & Style

The design system focuses on utility, efficiency, and trust. As a tool designed to manage personal data and storage, the UI must feel reliable and surgical. The brand personality is professional and unobtrusive, prioritizing content (images and videos) over decorative elements.

The visual style follows **Corporate / Modern** principles, specifically adhering to **Material Design 3 (Material You)** logic. It utilizes high-density layouts, clear functional signifiers, and a systematic approach to information architecture to ensure users feel in total control of their device hygiene.

## Colors

The palette is anchored by a deep professional blue, signaling stability and technical competence. 

- **Primary (#0D47A1):** Used for key actions, progress indicators, and active states.
- **Surface (#F5F7FA):** A cool-toned light gray background that reduces eye strain during long sorting sessions and provides better contrast for image thumbnails than pure white.
- **Functional Colors:** High-contrast neutrals are used for technical metadata (file paths, sizes, dates). Error reds are reserved strictly for destructive "Delete" actions or critical storage warnings.

## Typography

This design system utilizes **Inter** for the primary interface to ensure maximum readability across various screen densities. 

To emphasize the utility-focused nature of the app, **JetBrains Mono** is introduced for labels and technical data. This monospaced font is used for file sizes (e.g., "4.2 MB"), file paths, and timestamps, providing a distinct visual "technical" layer that separates metadata from user-facing copy.

- **Headlines:** Bold and tight for clear section anchoring.
- **Technical Labels:** Monospaced, uppercase where appropriate, to evoke a sense of precision.

## Layout & Spacing

The layout follows a **Fluid Grid** model optimized for Android handheld devices. 

- **Grid:** A standard 4-column grid for mobile, scaling to 8 columns for landscape/tablets.
- **Rhythm:** An 8px linear scale (with 4px increments for tight UI elements) governs all padding and margins.
- **Density:** High-density layouts are preferred for comparison screens. Image grids should utilize a 12px gutter to balance visibility with information density. 
- **Safe Areas:** Strict adherence to the Android status bar and navigation bar heights, ensuring content is never obscured by system overlays.

## Elevation & Depth

In line with Material Design 3, this design system uses **Tonal Layers** rather than heavy shadows to indicate depth.

- **Level 0 (Surface):** The main background (#F5F7FA).
- **Level 1 (Cards):** Slightly elevated using a pure white (#FFFFFF) background with a very subtle 1px border (#E1E4E8).
- **Level 2 (Modals/Menus):** Uses a soft ambient shadow (0px 4px 12px, 5% opacity black) to separate temporary overlays from the workspace.
- **Active States:** Subtle tonal shifts (primary color at 8% opacity) are used for pressed or selected states on list items and cards.

## Shapes

The shape language is modern and approachable but maintains a structured edge. 

- **Primary Containers:** 16px (rounded-lg) for main content cards, image thumbnails, and buttons.
- **Small Elements:** 8px (rounded-md) for chips, input fields, and small action icons.
- **Selection Indicators:** Circular (pill) shapes for checkboxes and radio indicators to provide a clear contrast against the rectangular nature of image thumbnails.

## Components

### Buttons
- **Primary:** Filled with #0D47A1, white text, 16px corner radius.
- **Secondary:** Outlined with primary color, 1px stroke.
- **Destructive:** Filled with #BA1A1A for final "Delete" confirmation.

### Cards (Comparison & Info)
- Used for image comparison groups (e.g., "Similar Photos").
- Background: #FFFFFF.
- Corner Radius: 16px.
- Padding: 12px.
- Must include a monospaced "File Size" label in the top-right or bottom-right corner.

### Selection States
- Use a bold blue checkmark icon in the top-right corner of image thumbnails.
- Selected thumbnails should receive a 3px inner border of the primary color.

### Scanning & Status
- **Progress Bars:** Thin, linear indeterminate bars for background scanning.
- **Status Indicators:** Small dot icons (Green for "Cleaned", Yellow for "Review Needed", Blue for "Scanning").

### Icons
- Use **Minimalist Outline** styles (2px stroke width). 
- Icons should be consistent in size (24x24px bounding box).
- Use distinct icons for "Duplicate," "Blurry," "Screenshot," and "Large Video" categories.

### Lists
- High-density list items for file path browsing.
- Height: 56px or 64px.
- Separators: 1px light gray (#E1E4E8) with 16px left inset.