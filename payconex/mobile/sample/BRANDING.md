# Bluefin sample app branding

The Android and iOS screens use the same visual treatment inspired by the current [Bluefin website](https://www.bluefin.com/), inspected on 2 October 2026. The title remains **BluePOS Go Sample**, with **SDK developer sample** immediately below it.

## Source assets and colors

The bundled logo files are unmodified copies of public Bluefin website assets:

- [Color logo](https://www.bluefin.com/wp-content/uploads/2023/03/Bluefin-60.png)
- [White logo for dark backgrounds](https://www.bluefin.com/wp-content/uploads/2021/08/Bluefin-w-60.png)

They are bundled locally, so the samples do not need a network request to display branding. Logos remain Bluefin trademarks.

Colors are taken from the website's computed styles and [CSS variables](https://www.bluefin.com/wp-content/uploads/the7-css/css-vars.css):

| Role | Color |
| --- | --- |
| Brand navy / headings | `#071D49` |
| Action blue | `#0047BB` |
| Sky-blue accent | `#00A9E0` |
| Primary action yellow | `#FFCD00` |
| Sample pale-blue surface | `#E8F4F9` |
| Sample page background | `#F3F8FC` |

The navy, blue, sky and yellow are website values. Pale surfaces, borders and dark-mode colors are native-app adaptations, not an official brand specification. Yellow primary actions use navy text for contrast. Secondary actions use blue outlines. The header's diagonal motif echoes the website geometry without altering the logo. Native scalable sans-serif typography preserves platform accessibility rather than downloading web fonts at runtime.

## Theme entry points

- Android: `android-sample-app/app/src/main/java/com/bluefin/testaidlgo/ui/theme/Color.kt`, `Theme.kt` and `Type.kt`. `MainScreen.kt` contains the header and shared component styling. The theme intentionally does not use Android wallpaper-derived colors.
- iOS: `ios-sample-app/BluePosGoSample/BluePosGoSample/BluefinTheme.swift`, with the header and components in `CheckoutView.swift`. Logo variants live in `BrandAssets.xcassets`.

Both apps have a **Dark mode** switch in the branded header. Dark mode is the default until the user chooses otherwise; the choice is saved locally and restored on launch, independently of the device appearance. Android uses `sample_preferences/dark_mode` in SharedPreferences. iOS uses `sample.darkMode` in UserDefaults and overrides the app window appearance. The shared sections, operation mapping, input validation, SDK diagnostics and local credential configuration remain in place. Styling does not change transaction behavior.
