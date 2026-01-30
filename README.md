# In-App Keyboard (Android)

This repo contains an **in-app keyboard** component (not a system IME) with multiple layouts (EN / ZH Pinyin / FR / AR) and a simple **candidate bar** for Pinyin.

- **App module (`:app`)**: demo host app (only for testing).
- **Library module (`:keyboard`)**: the reusable keyboard component.

> Note: This project is an *in-app* keyboard view meant to be embedded inside your app UI. It does **not** register as an Android system input method.

---

## Library: `:keyboard`

### 1) Add the module dependency

If you’re consuming the library as a module in the same Gradle project:

**settings.gradle**
```gradle
include ':app', ':keyboard'
```

**app/build.gradle**
```gradle
dependencies {
    implementation project(":keyboard")
}
```

### 2) Important: keep `dict_pinyin.dat` uncompressed

The Pinyin decoder reads the dictionary from `res/raw/dict_pinyin.dat` via file descriptor offsets.
If AAPT compresses it, the native decoder may fail or crash.

Add this to the **host app**:

```gradle
android {
    aaptOptions {
        noCompress 'dat'
    }
}
```

### 3) Add CandidateBarView in your layout (optional, for Pinyin)

```xml
<com.carbit.inappkeyboard.keyboard.CandidateBarView
    android:id="@+id/candidate_bar_view"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

### 4) Create and show `InAppKeyboardView`

Minimal integration pattern:

```kotlin
import com.carbit.inappkeyboard.keyboard.InAppKeyboardView

val editText = findViewById<EditText>(R.id.et_input)
val container = findViewById<FrameLayout>(R.id.keyboard_container)

// Prevent system IME.
editText.showSoftInputOnFocus = false

val keyboard = InAppKeyboardView(this).apply {
    attachTarget(editText.text) // commit into this Editable
}
container.addView(keyboard)

editText.setOnFocusChangeListener { _, hasFocus ->
    keyboard.visibility = if (hasFocus) View.VISIBLE else View.GONE
}
editText.setOnClickListener {
    keyboard.visibility = View.VISIBLE
}
```

### 5) Enable Pinyin composing + candidate commit (optional)

The library provides:
- `PinyinDecoder`: JNI-backed decoder
- `PinyinImeSession`: composing state machine (buffer + candidate list)
- `CandidateBarView`: candidate UI

A typical wiring (same as the demo app) looks like:

```kotlin
import com.carbit.inappkeyboard.keyboard.*

val decoder = PinyinDecoder(this)
val session = PinyinImeSession(decoder)
val candidateBar = findViewById<CandidateBarView>(R.id.candidate_bar_view)

val commitTarget = object : ITextCommitTarget {
    override fun insert(text: String) {
        editText.text.insert(editText.selectionStart.coerceAtLeast(editText.text.length), text)
    }
}

fun refreshCandidates() {
    if (!session.hasComposing()) {
        candidateBar.clear()
        return
    }
    session.bindCandidateClicks(commitTarget, candidateBar)
}

keyboard.onCommitText = { layout, text ->
    if (layout == InAppKeyboardView.Layout.ZH_PINYIN) {
        session.onCommitChar(text, candidateBar)
        refreshCandidates()
        true
    } else false
}

keyboard.onBackspace = { layout ->
    if (layout == InAppKeyboardView.Layout.ZH_PINYIN) {
        val consumed = session.onBackspace(candidateBar)
        if (consumed) refreshCandidates()
        consumed
    } else false
}

keyboard.onSpace = { layout ->
    if (layout == InAppKeyboardView.Layout.ZH_PINYIN) {
        val consumed = session.onSpaceCommitBest(commitTarget, candidateBar)
        if (consumed) refreshCandidates()
        consumed
    } else false
}
```

Don’t forget to release resources:
```kotlin
override fun onDestroy() {
    decoder.close()
    super.onDestroy()
}
```

---

## Testing

Run unit tests for the library:

```bash
./gradlew :keyboard:testDebugUnitTest
```

Build the demo app:

```bash
./gradlew :app:assembleDebug
```
