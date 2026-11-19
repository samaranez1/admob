extends Control

## Demo for the GodotAdMob Android plugin.
## Run on an Android device/export with the plugin enabled and Gradle build turned on.

@onready var status_label: Label = %Status
@onready var log_view: RichTextLabel = %Log

func _ready() -> void:
	_log("Plugin available: %s" % str(AdMob.is_plugin_available()))

	# Connect wrapper signals.
	AdMob.initialized.connect(_on_initialized)
	AdMob.ad_loaded.connect(_on_ad_loaded)
	AdMob.ad_failed_to_load.connect(_on_ad_failed_to_load)
	AdMob.ad_shown.connect(_on_ad_shown)
	AdMob.ad_closed.connect(_on_ad_closed)
	AdMob.rewarded_earned.connect(_on_rewarded_earned)

	# Wire buttons.
	%InitButton.pressed.connect(_on_init_pressed)
	%BannerLoad.pressed.connect(_on_banner_load_pressed)
	%BannerHide.pressed.connect(_on_banner_hide_pressed)
	%BannerDestroy.pressed.connect(_on_banner_destroy_pressed)
	%InterstitialLoad.pressed.connect(_on_interstitial_load_pressed)
	%InterstitialShow.pressed.connect(_on_interstitial_show_pressed)
	%RewardedLoad.pressed.connect(_on_rewarded_load_pressed)
	%RewardedShow.pressed.connect(_on_rewarded_show_pressed)


func _on_init_pressed() -> void:
	_set_status("Initializing...")
	_log("Calling AdMob.initialize() with consent flow and no test device IDs (emulators are test devices).")
	# For physical test devices, pass your hashed test device IDs:
	# AdMob.initialize(PackedStringArray(["YOUR_HASHED_TEST_DEVICE_ID"]), true)
	AdMob.initialize(PackedStringArray(), true)


func _on_banner_load_pressed() -> void:
	_log("Loading banner (test ad unit)...")
	AdMob.load_banner(AdMob.TEST_BANNER_AD_UNIT_ID, AdMob.BANNER_BOTTOM)

func _on_banner_hide_pressed() -> void:
	_log("Hiding banner")
	AdMob.hide_banner()

func _on_banner_destroy_pressed() -> void:
	_log("Destroying banner")
	AdMob.destroy_banner()


func _on_interstitial_load_pressed() -> void:
	_log("Loading interstitial (test ad unit)...")
	AdMob.load_interstitial(AdMob.TEST_INTERSTITIAL_AD_UNIT_ID)

func _on_interstitial_show_pressed() -> void:
	_log("Showing interstitial (must be loaded first)")
	AdMob.show_interstitial()


func _on_rewarded_load_pressed() -> void:
	_log("Loading rewarded (test ad unit)...")
	AdMob.load_rewarded(AdMob.TEST_REWARDED_AD_UNIT_ID)

func _on_rewarded_show_pressed() -> void:
	_log("Showing rewarded (must be loaded first)")
	AdMob.show_rewarded()


# --- Signal handlers ---

func _on_initialized() -> void:
	_set_status("Initialized signal received. is_initialized=%s" % str(AdMob.is_initialized()))
	_log("Signal: initialized")

func _on_ad_loaded(ad_format: String) -> void:
	_log("Signal: ad_loaded (%s)" % ad_format)

func _on_ad_failed_to_load(error_code: int) -> void:
	_log("Signal: ad_failed_to_load (code=%d)" % error_code)

func _on_ad_shown(ad_format: String) -> void:
	_log("Signal: ad_shown (%s)" % ad_format)

func _on_ad_closed(ad_format: String) -> void:
	_log("Signal: ad_closed (%s)" % ad_format)

func _on_rewarded_earned(reward_type: String, amount: int) -> void:
	_log("Signal: rewarded_earned (type=%s, amount=%d)" % [reward_type, amount])


# --- UI helpers ---

func _set_status(text: String) -> void:
	status_label.text = "Status: " + text

func _log(msg: String) -> void:
	print("[AdMobDemo] ", msg)
	log_view.append_text(msg + "\n")
