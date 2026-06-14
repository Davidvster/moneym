# Play Store assets

Generates Google Play screenshots + hero from the app's real Compose UI via Paparazzi,
then composites each into a code-drawn Android device frame on a near-black background.

Output: 4 screens × {phone, 7" tablet, 10" tablet} = 12 framed images + 1 hero (1024×500).

## Layout

```
playstore/
  frame_store.py     # framing + hero script (Pillow)
  config.json        # canvas sizes, fonts, colors, per-screen titles/subtitles
  raw/               # raw Paparazzi screenshots (store_<device>_<screen>.png)
  framed/            # FINAL store images + hero_1024x500.png  ← upload these
  templates/         # empty device-frame wireframes (text + frame, no screenshot)
```

## Pipeline

### 1. Record the screenshots (Kotlin → Paparazzi)

The screenshots come from `StoreScreenshotTest` in each feature module, which renders a
light-theme, EUR, May-2026 mock preview (`Store*Preview()` composables) at phone / 7" / 10".

```bash
./gradlew :feature:transactions:recordPaparazziDebug \
          :feature:overview:recordPaparazziDebug \
          :feature:transactionEdit:recordPaparazziDebug \
          :feature:settings:recordPaparazziDebug \
          --no-configuration-cache --tests "*.StoreScreenshotTest"
```

Renders at full device resolution (`useDeviceResolution = true`): phone 1080×2400,
7" 1200×1920, 10" 1600×2560.

To change the mock data or copy on a screen, edit its `Store*Preview()` composable and re-record.

### 2. Collect into `raw/`

```bash
for m in transactions overview transactionEdit settings; do
  find feature/$m/src/androidUnitTest/snapshots/images -name "*StoreScreenshotTest*store_*.png"
done | while read f; do
  cp "$f" "playstore/raw/$(echo "$f" | sed -E 's/.*(store_[a-z0-9]+_[a-z]+)\.png/\1.png/')"
done
```

### 3. Frame + hero (Python)

```bash
python3 -m pip install --user Pillow      # once
python3 playstore/frame_store.py            # → framed/*.png + hero
python3 playstore/frame_store.py --templates  # also dump empty wireframes → templates/
```

## Updating screenshots later

The frame + text ("wireframe") is generated independently of screenshot content, so:

1. re-record (step 1) and re-collect (step 2) — drop new PNGs into `raw/`
2. run `python3 playstore/frame_store.py`

No script edits needed. Titles/subtitles and frame geometry live in `config.json` + the script.
Tweaking copy or colors = edit `config.json` and re-run (no re-record).

## Notes

- Frames are drawn in code (`make_device_frame`) — generic Android, no camera cutout. No assets.
- Hero pulls the app icon from `androidApp/src/main/ic_launcher-playstore.png`.
- The `Store*Preview()` composables force `MoneyMTheme(darkTheme = false)` and provide
  `LocalInspectionMode = true` so entrance animations render settled under Paparazzi.
