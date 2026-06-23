// 双影密探 ShadowSleuth 共享 Tailwind 配置
// 基于 DESIGN.md 的 48 色色板、8 级字体、间距与圆角系统

tailwind.config = {
    darkMode: "class",
    theme: {
        extend: {
            colors: {
                // Surface
                "surface": "#f7f9fc",
                "surface-dim": "#d8dadd",
                "surface-bright": "#f7f9fc",
                "surface-container-lowest": "#ffffff",
                "surface-container-low": "#f2f4f7",
                "surface-container": "#eceef1",
                "surface-container-high": "#e6e8eb",
                "surface-container-highest": "#e0e3e6",
                "on-surface": "#191c1e",
                "on-surface-variant": "#434652",
                "inverse-surface": "#2d3133",
                "inverse-on-surface": "#eff1f4",
                "outline": "#737783",
                "outline-variant": "#c3c6d4",
                "surface-tint": "#2b5bb5",
                "surface-variant": "#e0e3e6",

                // Primary
                "primary": "#003178",
                "on-primary": "#ffffff",
                "primary-container": "#0d47a1",
                "on-primary-container": "#a1bbff",
                "inverse-primary": "#b0c6ff",

                // Secondary
                "secondary": "#486173",
                "on-secondary": "#ffffff",
                "secondary-container": "#c9e3f9",
                "on-secondary-container": "#4d6678",

                // Tertiary
                "tertiary": "#003b43",
                "on-tertiary": "#ffffff",
                "tertiary-container": "#00545e",
                "on-tertiary-container": "#7bc8d5",

                // Error
                "error": "#ba1a1a",
                "on-error": "#ffffff",
                "error-container": "#ffdad6",
                "on-error-container": "#93000a",

                // Fixed colors
                "primary-fixed": "#d9e2ff",
                "primary-fixed-dim": "#b0c6ff",
                "on-primary-fixed": "#001945",
                "on-primary-fixed-variant": "#00429c",
                "secondary-fixed": "#cbe6fb",
                "secondary-fixed-dim": "#b0cadf",
                "on-secondary-fixed": "#011e2e",
                "on-secondary-fixed-variant": "#314a5b",
                "tertiary-fixed": "#a2effd",
                "tertiary-fixed-dim": "#85d2e0",
                "on-tertiary-fixed": "#001f24",
                "on-tertiary-fixed-variant": "#004f58",

                // Background
                "background": "#f7f9fc",
                "on-background": "#191c1e"
            },
            borderRadius: {
                "sm": "0.25rem",
                "DEFAULT": "0.5rem",
                "md": "0.75rem",
                "lg": "1rem",
                "xl": "1.5rem",
                "full": "9999px"
            },
            spacing: {
                "base": "4px",
                "xs": "4px",
                "sm": "8px",
                "md": "16px",
                "lg": "24px",
                "xl": "32px",
                "edge-margin": "16px",
                "grid-gutter": "12px",
                "safe": "env(safe-area-inset-bottom, 24px)",
                "safe-top": "env(safe-area-inset-top, 44px)"
            },
            fontFamily: {
                "display-lg": ["Inter", "sans-serif"],
                "headline-md": ["Inter", "sans-serif"],
                "headline-sm": ["Inter", "sans-serif"],
                "headline-md-mobile": ["Inter", "sans-serif"],
                "body-lg": ["Inter", "sans-serif"],
                "body-md": ["Inter", "sans-serif"],
                "label-md": ["JetBrains Mono", "monospace"],
                "label-sm": ["JetBrains Mono", "monospace"]
            },
            fontSize: {
                "display-lg": ["32px", { lineHeight: "40px", letterSpacing: "-0.02em", fontWeight: "700" }],
                "headline-md": ["24px", { lineHeight: "32px", fontWeight: "600" }],
                "headline-sm": ["20px", { lineHeight: "28px", fontWeight: "600" }],
                "headline-md-mobile": ["22px", { lineHeight: "28px", fontWeight: "600" }],
                "body-lg": ["16px", { lineHeight: "24px", fontWeight: "400" }],
                "body-md": ["14px", { lineHeight: "20px", fontWeight: "400" }],
                "label-md": ["12px", { lineHeight: "16px", letterSpacing: "0.5px", fontWeight: "500" }],
                "label-sm": ["10px", { lineHeight: "14px", fontWeight: "500" }]
            }
        }
    }
};
