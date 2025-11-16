# StudyPal Styling Reference

## Color Palette
- **Material Theme Sources:** Base palette defined in `app/src/main/java/com/fouwaz/studypal/ui/theme/Color.kt:7-29` and wired into light/dark schemes in `app/src/main/java/com/fouwaz/studypal/ui/theme/Theme.kt:12-68`.
  - `BrandBlue` `#2196F3` - primary accent for buttons and highlights.
  - `BrandBlueDark` `#1976D2` - primary container/on-secondary contrast.
  - `BrandBlueLight` `#64B5F6` - tertiary accent and tonal variations.
  - Light surfaces: `SurfaceLight` `#FAFAFA`, `SurfaceVariantLight` `#F5F5F5`, `CardSurfaceLight` `#FFFFFF`.
  - Dark surfaces: `SurfaceDark` `#121212`, `SurfaceVariantDark` `#1E1E1E`, `CardSurfaceDark` `#2C2C2C`.
  - Text hierarchy: `TextPrimary` `#212121`, `TextSecondary` `#757575`, `TextTertiary` `#9E9E9E`.
  - Feedback colors: `SuccessGreen` `#4CAF50`, `ErrorRed` `#F44336`, `WarningOrange` `#FF9800`.
  - Additional scheme values (for example `primaryContainer`, `errorContainer`) located in `Theme.kt:39-65`.

- **Global Background Tint:** `#FFFFFCF9` applied to welcome, voice session, and draft screens for a warm neutral canvas (`WelcomeScreen.kt:49`, `VoiceSessionScreen.kt:119`, `DraftViewScreen.kt:78`).
- **Primary Text Override:** `#000000` used extensively for on-surface/on-primary content such as titles and icons (`WelcomeScreen.kt:70`, `DraftViewScreen.kt:90`, `VoiceSessionScreen.kt:132`).
- **Secondary Text Accent:** `#2A2A37` provides muted headings (`WelcomeScreen.kt:81`, `DraftViewScreen.kt:176`, `VoiceSessionScreen.kt:262`).
- **Neutral Pebble/Panel Base:** `#E9DED9` highlights premium surfaces like the FAB, cards, and buttons (`WelcomeScreen.kt:101`, `VoiceSessionScreen.kt:374`, `DraftViewScreen.kt:265`).
- **Soft Panel Background:** `#F8F8F8` backs secondary cards and status areas (`VoiceSessionScreen.kt:255`, `DraftViewScreen.kt:186`).
- **Transparent Overlays:** Semi-transparent black `#000000` with alpha about 0.7 for modal dimming (`MilestoneCelebrationDialog.kt:53`).
- **Pebble Identity Colors:** Custom swatches per achievement type in `app/src/main/java/com/fouwaz/studypal/domain/model/PebbleType.kt:28-82` (for example `#9E9E9E` default, `#00A86B` growth, `#0B1215` mastery).

## Typography
- **Font Family:** All styles use `FontFamily.Default` (no custom fonts) as defined in `app/src/main/java/com/fouwaz/studypal/ui/theme/Type.kt:12-121`.
- **Material Typography Scale:** Key sizes and weights:
  - Display Large `57sp / 64sp` line height, bold (`Type.kt:14-20`).
  - Display Medium `45sp / 52sp`, bold (`Type.kt:21-27`).
  - Display Small `36sp / 44sp`, semi-bold (`Type.kt:28-34`).
  - Headline Large `32sp / 40sp`, semi-bold (`Type.kt:37-43`).
  - Headline Medium `28sp / 36sp`, semi-bold (`Type.kt:44-50`).
  - Headline Small `24sp / 32sp`, semi-bold (`Type.kt:51-57`).
  - Title Large `22sp / 28sp`, medium (`Type.kt:60-66`).
  - Title Medium `16sp / 24sp`, medium (`Type.kt:67-73`).
  - Title Small `14sp / 20sp`, medium (`Type.kt:74-80`).
  - Body Large `16sp / 24sp`, normal (`Type.kt:83-89`).
  - Body Medium `14sp / 20sp`, normal (`Type.kt:90-96`).
  - Body Small `12sp / 16sp`, normal (`Type.kt:97-103`).
  - Label Large `14sp / 20sp`, medium (`Type.kt:106-112`).
  - Label Medium `12sp / 16sp`, medium (`Type.kt:113-119`).
  - Label Small `11sp / 16sp`, medium (`Type.kt:120-126`).

- **Common Overrides:**
  - Welcome title uses `displaySmall` with bold weight and black tint (`WelcomeScreen.kt:67-74`).
  - Draft screen headers draw from `titleMedium` and `labelLarge` with explicit weights (`DraftViewScreen.kt:166-278`).
  - Project list cards frequently apply `FontWeight.SemiBold` on `titleMedium` for emphasis (`ProjectListScreen.kt:283`, `ProjectListScreen.kt:307`, `ProjectListScreen.kt:497`).
  - Buttons leverage `labelLarge` with uppercase styling and increased letter spacing (`DraftViewScreen.kt:268-278`, `VoiceSessionScreen.kt:505-516`).

## Iconography and Shapes
- Icons default to `Icons.Default` with tint matching either `#000000` or `BrandBlue` depending on context (see `WelcomeScreen.kt:105-115`, `VoiceSessionScreen.kt:515`).
- Buttons and FABs use rounded shapes: `CircleShape` for the primary FAB (`WelcomeScreen.kt:101-118`) and `RoundedCornerShape(28.dp)` for the main call-to-action buttons (`DraftViewScreen.kt:259-273`).
