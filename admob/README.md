# Godot 4.7.2 Android AdMob Plugin (v2)

Android-only Google AdMob plugin for **Godot 4.7.2** using the **Godot Android plugin v2** system (`GodotPlugin` + `EditorExportPlugin`) and the **Gradle custom build** export flow.

It provides a Kotlin plugin packaged as an AAR, an `EditorExportPlugin` that registers the AAR and Maven dependencies, and a GDScript autoload wrapper (`AdMob.gd`) for simple game-side usage.

## Features

- `initialize(test_device_ids, use_consent_flow)` with:
  - optional hashed test device IDs
  - Google UMP consent flow (`UserMessagingPlatform`) before initializing Mobile Ads
- Ad formats:
  - Banner (top/bottom)
  - Interstitial
  - Rewarded
- Signals (emitted to Godot safely via the plugin signal system):
  - `initialized`
  - `ad_loaded(ad_format: String)` — `"banner"`, `"interstitial"`, `"rewarded"`
  - `ad_failed_to_load(error_code: int)`
  - `ad_shown(ad_format: String)`
  - `ad_closed(ad_format: String)`
  - `rewarded_earned(reward_type: String, amount: int)`
- Google **official test ad unit IDs** by default
- Manifest includes AdMob App ID meta-data and required permissions (`INTERNET`, `ACCESS_NETWORK_STATE`)
- Ad operations are executed on the Android UI/host thread; signals are emitted back to Godot through `GodotPlugin.emitSignal`

## Repository / folder structure

```text
.
├── build.gradle.kts                     # Root Gradle build (AGP/Kotlin versions)
├── settings.gradle.kts
├── gradle.properties
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties    # Gradle distribution (8.14.3)
├── plugin/                              # Android library module (builds the AAR)
│   ├── build.gradle.kts
│   ├── export_scripts_template/         # Copied into the addon on assemble
│   │   ├── export_plugin.gd             # EditorExportPlugin (v2 packaging)
│   │   └── plugin.cfg
│   └── src/main/
│       ├── AndroidManifest.xml          # v2 plugin meta-data, permissions, AdMob App ID
│       └── java/org/godotengine/plugin/android/admob/
│           └── GodotAdMobPlugin.kt      # Kotlin GodotPlugin implementation
├── addons/
│   └── GodotAdMob/                      # Installable Godot addon (Editor + runtime)
│       ├── export_plugin.gd
│       ├── plugin.cfg
│       ├── AdMob.gd                     # Autoload wrapper for game code
│       └── bin/                         # Created by build: debug/ & release/ AARs
├── demo/
│   ├── project.godot
│   ├── demo_scene.tscn
│   └── demo.gd
└── README.md
```

> `addons/GodotAdMob/bin/{debug,release}/` is populated when you build the Android library (see below). The addon is not fully usable for export until the AARs are built.

## Requirements

- **Godot 4.7.2** (project targets 4.7; plugin compiled against `org.godotengine:godot:4.7.2.stable`)
- **Android export templates** installed for Godot 4.7.2
- **Custom Gradle build** enabled for Android export (v2 plugins require it)
- **JDK 17** (recommended for AGP 8.x)
- **Android SDK** with a platform matching `compileSdk` (36) and build tools available
- **Gradle** 8.14.3 (use your own `gradlew`/Gradle installation; wrapper properties are included but the wrapper binary/scripts are not committed here)

### Android dependency versions (declared)

- `org.godotengine:godot:4.7.2.stable` (compile-only)
- `com.google.android.gms:play-services-ads:25.5.0`
- `com.google.android.ump:user-messaging-platform:4.0.0`

These Maven dependencies are also returned by the `EditorExportPlugin` via `_get_android_dependencies(...)` so the Gradle export resolves them.

## Build the plugin (AAR)

From the repository root (with Android SDK/JDK configured, e.g. `ANDROID_HOME`/`ANDROID_SDK_ROOT` set):

```bash
# If you have a Gradle installation:
gradle :plugin:assemble

# Or if you add/generate the Gradle wrapper (gradlew) in this project:
# ./gradlew :plugin:assemble
```

This produces:
- `plugin/build/outputs/aar/GodotAdMob-debug.aar`
- `plugin/build/outputs/aar/GodotAdMob-release.aar`

and the build tasks copy them (plus export scripts) into:
- `addons/GodotAdMob/bin/debug/GodotAdMob-debug.aar`
- `addons/GodotAdMob/bin/release/GodotAdMob-release.aar`

> **Note:** This environment cannot generate a signed/complete Android toolchain for you. Build on a machine with the Android SDK and JDK installed.

## Install into your Godot project

1. **Copy the addon** folder into your game project:
   - Copy `addons/GodotAdMob/` → `<your_project>/addons/GodotAdMob/`
   - Ensure it includes the built AARs in `addons/GodotAdMob/bin/debug/` and/or `bin/release/`.

2. **Enable the plugin in the editor:**
   - `Project -> Project Settings -> Plugins` → enable **GodotAdMob**.
   - This activates the `EditorExportPlugin` that injects the AAR and Gradle dependencies during Android export.

3. **Add the GDScript wrapper as an Autoload (recommended):**
   - `Project -> Project Settings -> Globals/Autoload`
   - Path: `res://addons/GodotAdMob/AdMob.gd`
   - Name: `AdMob`
   - Game code should call `AdMob.*` and connect to `AdMob.*` signals.

4. **Install Android Build Template (Gradle):**
   - `Project -> Install Android Build Template...`

5. **Configure Android export preset:**
   - `Project -> Export...` → add/select **Android** preset
   - Enable **Gradle Build** (`Use Gradle Build = true`)
   - Ensure a valid package name, keystore (for release), and min/target SDK are set

6. **AdMob App ID:**
   - The plugin manifest ships with Google's **sample/test** App ID:
     - `ca-app-pub-3940256099942544~3347511713`
   - **Replace it with your real AdMob App ID before publishing.**
   - You can override it in your project's main `AndroidManifest.xml` (custom build template) with a `<meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" ...>` entry; if manifest merge conflicts occur, add `tools:replace="android:value"` on your override (and declare the `tools` namespace).

## Usage (GDScript)

```gdscript
extends Node

func _ready() -> void:
	# Connect signals from the AdMob autoload wrapper
	AdMob.initialized.connect(_on_initialized)
	AdMob.ad_loaded.connect(_on_ad_loaded)
	AdMob.ad_failed_to_load.connect(_on_ad_failed_to_load)
	AdMob.ad_shown.connect(_on_ad_shown)
	AdMob.ad_closed.connect(_on_ad_closed)
	AdMob.rewarded_earned.connect(_on_rewarded_earned)

	# Initialize with UMP consent flow. Optionally pass hashed test device IDs.
	# test_device_ids can be a PackedStringArray, Array, or comma-separated String.
	AdMob.initialize(PackedStringArray(), true)

func _on_initialized() -> void:
	# Load ads after initialization (consent permitting)
	AdMob.load_banner(AdMob.TEST_BANNER_AD_UNIT_ID, AdMob.BANNER_BOTTOM)
	AdMob.load_interstitial() # defaults to Google's test interstitial ID
	AdMob.load_rewarded()      # defaults to Google's test rewarded ID

func _on_ad_loaded(ad_format: String) -> void:
	print("Loaded: ", ad_format)
	if ad_format == "interstitial":
		# AdMob.show_interstitial()  # show when appropriate
		pass

func _on_ad_failed_to_load(error_code: int) -> void:
	print("Ad failed to load, error: ", error_code)

func _on_ad_shown(ad_format: String) -> void:
	print("Shown: ", ad_format)

func _on_ad_closed(ad_format: String) -> void:
	print("Closed: ", ad_format)

func _on_rewarded_earned(reward_type: String, amount: int) -> void:
	print("Reward: ", reward_type, " x", amount)
```

### API summary (wrapper)

- `initialize(test_device_ids = PackedStringArray(), use_consent_flow = true)`
  - `test_device_ids`: `PackedStringArray`/`Array`/comma-separated `String` of hashed test device IDs
  - `use_consent_flow`: runs UMP consent gathering before `MobileAds.initialize`
- `is_initialized() -> bool`
- Banner: `load_banner(ad_unit_id = TEST_BANNER_ID, position = BANNER_BOTTOM)`, `show_banner()`, `hide_banner()`, `destroy_banner()`
  - Positions: `AdMob.BANNER_TOP` (0), `AdMob.BANNER_BOTTOM` (1)
- Interstitial: `load_interstitial(ad_unit_id = TEST_INTERSTITIAL_ID)`, `show_interstitial()`
- Rewarded: `load_rewarded(ad_unit_id = TEST_REWARDED_ID)`, `show_rewarded()`

## Default test IDs (Google official)

| Type | ID |
| --- | --- |
| AdMob App ID (manifest sample) | `ca-app-pub-3940256099942544~3347511713` |
| Banner | `ca-app-pub-3940256099942544/6300978111` |
| Interstitial | `ca-app-pub-3940256099942544/1033173712` |
| Rewarded | `ca-app-pub-3940256099942544/5224354917` |

Replace ad unit IDs and the App ID with your own for production. Emulators are treated as test devices by Google Mobile Ads; for physical devices, register hashed test device IDs (logcat/AdMob UI) and pass them to `initialize(...)`.

## Demo

A minimal demo project is included in `demo/`:
- `demo/project.godot`
- `demo/demo_scene.tscn`
- `demo/demo.gd`

To use it, open `demo/` as a Godot project (or copy its files into a test project), ensure `addons/GodotAdMob/` with built AARs is present under the project, enable the plugin, add the autoload, install the Android build template, enable Gradle build, and deploy to an Android device/emulator. (Per environment constraints, this plugin cannot be run or visually tested here; verify on-device.)

## Notes / limitations

- **Android only.** On non-Android platforms the wrapper no-ops and reports the plugin as unavailable.
- **Consent behavior:** when `use_consent_flow` is enabled, Mobile Ads initialization proceeds only if UMP reports ads can be requested. If consent is not sufficient, `initialized` is emitted but ads may not be requestable until consent is obtained in a later session—handle this in your app logic as needed.
- **Banner size:** uses the standard `BANNER` (320x50) size placed at top/bottom of the content view. Adaptive banner sizing is not implemented in this baseline.
- **Threading:** ad operations are dispatched to the host/UI thread; `GodotPlugin.emitSignal` delivers signals back to Godot safely.
- **Godot API stability:** this targets the Godot 4.2+ v2 plugin architecture and was written against Godot 4.7.x APIs (`GodotPlugin`, `EditorExportPlugin` Android methods, `runOnHostThread`). If you target a different 4.x version, align the `org.godotengine:godot:<version>.stable` dependency and verify API compatibility.

## References

- Godot Android plugins (v2): https://docs.godotengine.org/en/stable/tutorials/platform/android/android_plugin.html
- v2 template: https://github.com/m4gr3d/Godot-Android-Plugin-Template
- Google Mobile Ads (Android): https://developers.google.com/admob/android/quick-start
- Test ads: https://developers.google.com/admob/android/test-ads
- UMP privacy/consent: https://developers.google.com/admob/android/privacy
- `EditorExportPlugin` (Android methods): https://docs.godotengine.org/en/stable/classes/class_editorexportplugin.html
