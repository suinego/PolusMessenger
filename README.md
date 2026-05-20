# ScanView

> Android library for **recording**, **serializing**, and **reconstructing** user interaction sessions — without screenshots or bitmaps.

ScanView intercepts touch events via `Window.Callback`, captures a structural snapshot of the touched View at the exact moment of interaction, classifies the gesture (TAP / SWIPE / LONG\_PRESS / MOVE), and saves everything to a portable JSON file that can later be replayed as a step-by-step visualization.

---

## Features

- **Zero-overhead capture** — records only when the user touches the screen, never on a timer
- **Structural, not visual** — captures `ViewNode` trees (class names, bounds, text, backgrounds), not pixel bitmaps
- **Gesture classification** — automatic TAP / SWIPE / LONG\_PRESS / MOVE detection
- **Screen-aware** — each record knows which fragment/screen was active
- **Portable JSON** — sessions can be saved, shared, and replayed on any device
- **Built-in visualization** — `ScanViewVisualizationView` renders sessions step-by-step with a seek bar
- **Diagnostics API** — FPS (FrameMetrics), widget resolution rate (Q1), timeline coverage (Q2), event overhead

---

## How It Works

```
User touches screen
        │
        ▼
Window.Callback.dispatchTouchEvent()   ← intercepts before the UI reacts
        │
        ├── ACTION_DOWN  → remember target View, start gesture buffer
        ├── ACTION_MOVE  → append to buffer (sampled, max 8 points)
        └── ACTION_UP    →
                ├── classify gesture   TAP / SWIPE / LONG_PRESS / MOVE
                ├── extract ViewNode   depth-limited tree snapshot
                └── history.add(InteractionRecord)
                              │
                              └── serialize() → JSON file
```

The key design decision: **capture on interaction, not on a clock**. This means zero CPU usage between touches and data size proportional to the number of interactions, not session duration.

---

## Quick Start

### 1. Add dependency

ScanView is distributed as a Gradle module (source). Copy the `scanview/` directory into your project, then in `settings.gradle.kts`:

```kotlin
include(":scanview")
```

In your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":scanview"))
}
```

### 2. Initialize in your Activity

```kotlin
class MainActivity : AppCompatActivity() {

    private val scanViewManager: ScanViewManager by lazy {
        ScanViewManagerFactory.create(object : ScanViewManagerDeps {
            override val context = this@MainActivity
            override val rootViewProvider: () -> View? = { window.decorView.rootView }
            override val activityProvider: (() -> Activity) = { this@MainActivity }
            override val logger: ((String, String) -> Unit) = { tag, msg -> Log.d(tag, msg) }
            //Not necessary but you can also
            // Provide the current screen name for richer session data
            override val screenNameProvider: (() -> String) = {
                when (supportFragmentManager.findFragmentById(R.id.container)) {
                    is HomeFragment    -> "Home"
                    is ProfileFragment -> "Profile"
                    is ChatFragment    -> "Chat"
                    else               -> "Unknown"
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        scanViewManager.startRecording()
    }

    override fun onPause() {
        super.onPause()
        scanViewManager.stopRecording()
    }
}
```

### 3. Save a session

```kotlin
private fun saveSession() {
    val history = scanViewManager.getHistory()
    if (history.isEmpty()) return
    val json = scanViewManager.serialize()
    val file = File(filesDir, "session_${System.currentTimeMillis()}.json")
    file.writeText(json)
}
```

### 4. Load and visualize

```xml
<!-- layout.xml -->
<com.example.scanview.visualization.ScanViewVisualizationView
    android:id="@+id/visualizationView"
    android:layout_width="match_parent"
    android:layout_height="0dp"
    android:layout_weight="1" />

<SeekBar
    android:id="@+id/seekBar"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

```kotlin
val json = File(filesDir, "session.json").readText()
val history = scanViewManager.deserialize(json)

val m = resources.displayMetrics
visualizationView.setInteractions(history, m.widthPixels, m.heightPixels)

seekBar.max = (history.size - 1).coerceAtLeast(1)
seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
    override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
        if (fromUser) visualizationView.setProgress(value.toFloat() / (history.size - 1))
    }
    override fun onStartTrackingTouch(sb: SeekBar?) {}
    override fun onStopTrackingTouch(sb: SeekBar?) {}
})
```

### 5. Interaction timeline list (optional)

```kotlin
val adapter = InteractionAdapter(history) { record ->
    val idx = history.indexOf(record)
    visualizationView.goToStep(idx, history.size)
    seekBar.progress = idx
}
recyclerView.layoutManager = LinearLayoutManager(
    context, LinearLayoutManager.HORIZONTAL, false
)
recyclerView.adapter = adapter
```

---


## Data Model

### `InteractionRecord`

```
InteractionRecord
├── screenName: String          "Chat", "Home" …
├── timestamp: Long             System.currentTimeMillis()
├── viewInfo: ViewInfo
│   ├── className: String       "AppCompatButton"
│   ├── idName: String?         "btnSend"  (null if view has no id)
│   ├── bounds: Rect?           screen-absolute bounding box
│   ├── text: String?           label text for TextView / Button
│   └── viewNode: ViewNode?     structural snapshot (see below)
└── gesture: Gesture
    ├── type: GestureType       TAP | SWIPE | LONG_PRESS | MOVE
    ├── startEvent: TouchEvent  x, y, timestamp at ACTION_DOWN
    ├── endEvent: TouchEvent?   x, y, timestamp at ACTION_UP
    └── moveEvents: List<TouchEvent>   sampled intermediate points
```

### Gesture classification rules

| Gesture | Condition |
|---|---|
| `TAP` | Duration ≤ 300 ms **and** distance < 20 dp |
| `SWIPE` | Distance > 100 dp |
| `LONG_PRESS` | Duration > 500 ms **and** distance < 20 dp |
| `MOVE` | Anything else |

### `ViewNode` — structural snapshot

```
ViewNode
├── className: String            "ConstraintLayout", "TextView" …
├── bounds: Rect                 screen-absolute pixels
├── background: BackgroundState? Color(argb) | Unknown(drawableClass)
├── alpha: Float                 0.0 – 1.0
├── visibility: Int              View.VISIBLE = 0
├── content: ViewContent?        Text(text, color, size, bold)
│                                ImagePlaceholder(tint)
└── children: List<ViewNode>     up to maxViewNodeDepth levels
```

> **Why not bitmaps?** `Drawable` objects are not serializable. ScanView extracts only primitive values (color ints, strings, floats) from the View hierarchy — nothing that holds references to `Canvas`, `Paint`, or `Bitmap`.

---

## Visualization

### `ScanViewVisualizationView`

Renders one interaction at a time. No overlapping — the seek bar moves through steps discretely.

```kotlin
// Load data
visualizationView.setInteractions(history, screenWidthPx, screenHeightPx)
visualizationView.setInteractions(history, screenW, screenH, VisualizationConfig(tapPointRadius = 30f))

// Seek
visualizationView.setProgress(0f)          // first step
visualizationView.setProgress(1f)          // last step
visualizationView.goToStep(3, history.size) // jump to step index 3
```

**Visual elements:**

| Element | Description |
|---|---|
| Rounded rectangles | ViewNode tree of the touched view |
| Ripple touch point | Concentric rings + colored center |
| SWIPE trail | Growing gradient dots → arrowhead |
| LONG\_PRESS rings | Pulsating concentric circles |
| Top indicator | Story dots (≤18 steps) or progress bar |
| Bottom panel | Screen name · step N/total · gesture type · target · elapsed time |

**Color coding:**

| Gesture | Color |
|---|---|
| TAP | `#00E676` green |
| SWIPE | `#FF6D00` orange |
| LONG\_PRESS | `#D500F5` purple |
| MOVE | `#00B0FF` blue |

### `VisualizationConfig`

```kotlin
data class VisualizationConfig(
    val tapPointRadius: Float = 22f,
    val swipeLineWidth: Float = 5f,
    val arrowLength:    Float = 24f
)
```

---

## JSON Format

```json
{
  "statistics": {
    "totalInteractions": 5,
    "taps": 3,
    "swipes": 1,
    "moves": 0,
    "longPresses": 1,
    "uniqueViews": 4
  },
  "interactions": [
    {
      "screenName": "Chat",
      "viewInfo": {
        "className": "AppCompatButton",
        "id": 2131231234,
        "idName": "btnSend",
        "bounds": { "left": 820, "top": 1730, "right": 1000, "bottom": 1810 },
        "text": "Send",
        "viewNode": {
          "className": "AppCompatButton",
          "bounds": { "left": 820, "top": 1730, "right": 1000, "bottom": 1810 },
          "background": { "type": "unknown", "drawableClass": "RippleDrawable" },
          "alpha": 1.0,
          "visibility": 0,
          "content": { "type": "text", "text": "Send", "textColor": -1, "textSizePx": 48.0, "isBold": false },
          "children": []
        }
      },
      "gesture": {
        "type": "TAP",
        "startEvent": { "action": "ACTION_DOWN", "x": 910.0, "y": 1770.0, "localX": 90.0, "localY": 40.0, "timestamp": 1715000000000 },
        "endEvent":   { "action": "ACTION_UP",   "x": 910.0, "y": 1770.0, "localX": 90.0, "localY": 40.0, "timestamp": 1715000000120 },
        "posledovatelnostMoveEvent": []
      },
      "timestamp": 1715000000000
    }
  ],
  "timestamp": 1715000005000
}
```

---

## Diagnostics API

For research and performance evaluation (thesis metrics M3, Q1, Q2):

```kotlin
// 1. Collect FrameMetrics during recording
private val frameDurations = mutableListOf<Double>()
private val frameListener = Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
    if (scanViewManager.isRecording())
        frameDurations.add(metrics.getMetric(FrameMetrics.TOTAL_DURATION) / 1_000_000.0)
}

override fun onResume() {
    super.onResume()
    frameDurations.clear()
    window.addOnFrameMetricsAvailableListener(frameListener, Handler(mainLooper))
    scanViewManager.startRecording()
}

override fun onPause() {
    super.onPause()
    scanViewManager.stopRecording()
    window.removeOnFrameMetricsAvailableListener(frameListener)
    scanViewManager.setFrameMetricsData(frameDurations.toList())
    
    // 2. Compute and log all metrics
    val m = scanViewManager.computeDiagnostics()
    Log.d("M3", "avg %.1f ms  p95 %.1f ms  jank %d/%d frames"
        .format(m.avgFrameMs, m.p95FrameMs, m.jankFrameCount, m.totalFrameCount))
    Log.d("Q1", "idName resolved %d/%d (%.0f%%)"
        .format(m.resolvedIdCount, m.totalInteractions, m.idNameResolutionRate * 100))
    Log.d("Q2", "max gap %d ms  avg gap %.0f ms"
        .format(m.maxTimestampGapMs, m.avgTimestampGapMs))
    Log.d("OVH", "event handling avg %.1f µs  max %d µs"
        .format(m.avgEventHandlingUs, m.maxEventHandlingUs))
}
```

### `DiagnosticMetrics`

| Field | Metric | Description |
|---|---|---|
| `avgFrameMs` / `p95FrameMs` | M3 | Average and 95th-percentile frame duration (ms) |
| `jankFrameCount` / `totalFrameCount` | M3 | Frames exceeding 16.6 ms budget |
| `idNameResolutionRate` | Q1 | Fraction of interactions with a resolved `idName` (0.0–1.0) |
| `resolvedIdCount` / `totalInteractions` | Q1 | Absolute counts |
| `maxTimestampGapMs` / `avgTimestampGapMs` | Q2 | Max and average gap between interaction timestamps |
| `avgEventHandlingUs` / `maxEventHandlingUs` | Overhead | Time spent in `dispatchTouchEvent` callback (microseconds) |

---

## API Reference

### `ScanViewManager`

| Method | Returns | Description |
|---|---|---|
| `startRecording()` | `Unit` | Install `Window.Callback` interceptor, clear history, begin capture |
| `stopRecording()` | `Unit` | Restore original callback, stop capture |
| `isRecording()` | `Boolean` | Whether capture is active |
| `getHistory()` | `List<InteractionRecord>` | All captured interactions |
| `getStatistics()` | `InteractionStatistics` | Aggregate counts by gesture type |
| `serialize()` | `String` | Export to pretty-printed JSON |
| `deserialize(json)` | `List<InteractionRecord>` | Import a saved session |
| `clearHistory()` | `Unit` | Discard all captured interactions |
| `findViewAt(view, x, y)` | `View?` | Deepest visible View at screen coordinates |
| `captureViewNode(view, depth)` | `ViewNode?` | Extract ViewNode tree from any View |
| `computeDiagnostics()` | `DiagnosticMetrics` | Compute M3/Q1/Q2 and event overhead |
| `setFrameMetricsData(durations)` | `Unit` | Feed frame durations for M3 computation |

---

## Requirements

| | Value |
|---|---|
| Min SDK | 24 (Android 7.0) |
| Compile / Target SDK | 36 |
| Kotlin | 1.9+ |
| Java | 21 |

**Bundled dependencies** (no manual additions needed):
- `com.google.code.gson:gson:2.10.1`
- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3`
- `androidx.recyclerview:recyclerview:1.3.2`
- `androidx.cardview:cardview:1.0.0`
- `androidx.appcompat:appcompat:1.6.1`

---

## Known Limitations

| Limitation | Notes |
|---|---|
| Single-Activity | `Window.Callback` is per-Activity. Multi-Activity apps need one instance per Activity. |
| Custom Views | Views that render only in `onDraw()` with no children/background produce a minimal `ViewNode` (className + bounds only). The touch event is still captured correctly. |
| EditText privacy | Text content is captured. Consider masking sensitive fields for production use. |
| Visibility filter | Only `View.VISIBLE` children are included in the `ViewNode` tree. |
| Main thread | ViewNode extraction runs synchronously on the UI thread during touch handling. Measured overhead: < 2 ms per interaction on modern devices. |

---

## Performance Benchmarks

Measured on **Pixel 7** (Android 17, Google Tensor G2, 8 GB RAM, 2400×1080).  
Scenario: list scrolling + screen navigation, ~2 minutes.

### Strategy comparison

| Strategy | Description |
|---|---|
| **S1 Bitmap** | Periodic `View.drawToBitmap()` at 2 fps |
| **S2 ViewNode** | Periodic full ViewNode tree traversal at 2 fps |
| **S3 ScanView** | This library — capture only on user interaction |

### Results

| Metric | Baseline | S1 Bitmap | S2 ViewNode | **S3 ScanView** |
|---|---|---|---|---|
| **M1 CPU max, %** | 7.3 | 28.4 | 8.9 | **10.5** |
| **M1 CPU delta, pp** | — | +21.1 | +1.6 | **+3.2** |
| **M4 Data, KB/min** | — | 14 262 | 122.6 | **55.6** |
| **M4 vs S3** | — | 256× | 2.2× | **1×** |
| **M3 Avg frame, ms** | ~5.5 | ~11 ¹ | ~7 ¹ | **6.1** |
| **M3 p95 frame, ms** | ~9 | ~52 ¹ | ~21 ¹ | **16.4** |
| **M3 Jank frames** | ~1–2% | ~22% ¹ | ~9% ¹ | **4.9% (23/473)** |
| **Q1 id resolution** | n/a | n/a | n/a | **83% (15/18)** |
| **Event overhead avg** | n/a | n/a | n/a | **74.1 µs** |
| **Event overhead max** | n/a | n/a | n/a | **1 243 µs** |

> ¹ Estimated from CPU observations; not measured directly with FrameMetrics.

### Key findings

**Data size (M4):** S3 generates **256× less data than S1** and **2.2× less than S2**.  
S1 stores raw ARGB_8888 bitmaps (≈10 MB/frame uncompressed, ≈135 KB/frame as PNG); S3 stores only a structural snapshot at the moment of touch.

**CPU overhead (M1):** S3 adds **+3.2 pp** over no-recording baseline.  
S1 adds +21.1 pp — nearly 7× more than S3.

**Frame times (M3):** S3 average frame is **6.1 ms**, well within the 16.6 ms budget.  
p95 = 16.4 ms — 95% of frames are jank-free throughout the session.

**Widget identification (Q1):** **83%** of interactions resolve to a named resource ID (`idName`).  
The 17% remainder are views without an explicit Android ID (programmatic or third-party).

**Touch overhead:** Average **74 µs per MotionEvent** — imperceptible to users (perception threshold ≈ 10 ms).
