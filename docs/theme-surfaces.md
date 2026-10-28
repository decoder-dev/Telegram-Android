# BananaGram theme surfaces

Cream and Graphite use the colors from iOS `makeBananaGramTheme` in
`TelegramPresentationData/Sources/MakePresentationTheme.swift`.
Their navigation bars use the list surface and primary text; gold is the accent,
not a navigation bar fill. Graphite uses a dark foreground on gold filled controls.
Semantic error and success colors remain separate.

`BananaThemes` applies the palette globally after stock accent transformation and
before derived table/code colors. The source accent for inherited controls is the
stock Day/Night blue, so controls outside the explicit surface overrides also tint
correctly. `BananaTheme.forExtras()` returns a lifetime-stable provider that reads
the active theme; it does not capture a separate palette for an open screen.
`BananaSettingsFragment` supplies native theme animation bindings for the header,
rows, secondary labels, section headers and switches across the Extras screens.
Row backgrounds use native cell-background bindings (rather than the fragment
background flag). A native theme-animation delegate refreshes visible and cached
text rows, preserving their semantic text/icon color tags. Closing a palette dialog
also refreshes the gesture-navigation strip for the current palette.

New installations default to Banana Cream (day) and Banana Graphite (night).
Existing explicit selections of any of the four permitted palettes are preserved.
Unsupported stored themes fall back to the branded day/night defaults. Legacy Glass
themes retain their existing migration to Classic White/Black plus the glass switch.
Glass remains an independent account option.

Regression coverage includes retained provider references across all four palettes,
header/text/control contrast, live selection through the real native palette dialog
without recreating the Extras view, selected-row label and stored ordinal, and
screenshots of the four resulting screens.
The checks inspect actual row backgrounds, text paints and navigation-strip paint
after each live switch, including rows reached by scrolling through every section.
They extend the existing
Android 13–16 acceptance suite; they do not measure physical-device FPS.
