# ColorText Studio

Rich-text editor for Android — color individual characters/ranges of Unicode text (CJK, emoji,
Latin) and export the result as a shareable PNG.

## Build

1. Open the project root in Android Studio (Koala+ recommended) and let it sync, **or** build
   from the command line:
   ```bash
   ./gradlew assembleDebug
   ```
   (If `gradlew` isn't executable: `chmod +x gradlew` first, or run `gradle wrapper` once with a
   local Gradle 8.7+ install to regenerate the wrapper jar, which isn't checked into this
   deliverable.)
2. The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.
3. Install directly: `adb install app/build/outputs/apk/debug/app-debug.apk`.

## Architecture

- `data/ColorSpan.kt` — pure, Android-free span model (`SpanOps`) that keeps a non-overlapping,
  fully-covering list of colored ranges in sync with text edits (insert/delete/split/merge).
  Fully unit tested in `app/src/test/.../SpanOpsTest.kt`.
- `data/DraftRepository.kt` — DataStore-backed autosave of the current text + spans.
- `data/CreationDb.kt`, `CreationRepository.kt` — Room-backed "My Creations" history.
- `viewmodel/EditorViewModel.kt` — all text/span/undo-redo/style state; delegates every
  Context/Bitmap side effect to `util/ImageExporter.kt` so it stays unit-testable.
- `util/ImageExporter.kt` — MediaStore save (scoped storage, no legacy permission), FileProvider
  content-URI sharing, clipboard image copy, thumbnail generation.
- `ui/EditorScreen.kt` — WYSIWYG editor: a transparent `BasicTextField` overlays a
  `Text(AnnotatedString)` preview built from the span list; both are wrapped in a
  `GraphicsLayer` that's rasterized to a `Bitmap` on export via `toImageBitmap()`.
- `ui/GalleryScreen.kt` — grid of past exports (Room + Coil for thumbnail loading).

## Notes / known limitations

- The launcher icon is a placeholder vector (no external asset pipeline was available); swap
  `drawable/ic_launcher_foreground.xml` for real artwork before shipping.
- `gradle-wrapper.jar` is not included (binary asset) — regenerate with `gradle wrapper` locally,
  or open directly in Android Studio, which supplies its own wrapper distribution on sync.
- Bold/Italic styling is tracked as a separate span list (`StyleSpan`) layered on top of color
  spans in the preview; it is not (yet) diffed against text edits the way color spans are, so
  it's best applied after typing is settled for a given range.
