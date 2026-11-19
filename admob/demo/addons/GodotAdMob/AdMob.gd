extends Node

## GDScript wrapper/autoload for the GodotAdMob Android plugin.
##
## Add this script as an Autoload (e.g. name: AdMob) in Project Settings.
## Game code should call these methods and connect to these signals instead of
## touching the Java/Kotlin singleton directly.
##
## All methods are safe to call on non-Android platforms (they become no-ops
## and emit ad_failed_to_load(-1) for load/show requests where appropriate).

# --- Signals (mirror the native plugin) ---
signal initialized
signal ad_loaded(ad_format: String)
signal ad_failed_to_load(error_code: int)
signal ad_shown(ad_format: String)
signal ad_closed(ad_format: String)
signal rewarded_earned(reward_type: String, amount: int)

# --- Google's official test ad unit IDs (defaults) ---
const TEST_BANNER_AD_UNIT_ID := "ca-app-pub-3940256099942544/6300978111"
const TEST_INTERSTITIAL_AD_UNIT_ID := "ca-app-pub-3940256099942544/1033173712"
const TEST_REWARDED_AD_UNIT_ID := "ca-app-pub-3940256099942544/5224354917"

# Banner positions
const BANNER_TOP := 0
const BANNER_BOTTOM := 1

const PLUGIN_SINGLETON_NAME := "GodotAdMob"

var _plugin: Object = null


func _ready() -> void:
	if Engine.has_singleton(PLUGIN_SINGLETON_NAME):
		_plugin = Engine.get_singleton(PLUGIN_SINGLETON_NAME)
		_connect_native_signals()
	elif OS.get_name() == "Android":
		push_error("GodotAdMob: singleton '%s' not found. Is the plugin enabled and exported with a Gradle build?" % PLUGIN_SINGLETON_NAME)
	else:
		# Not running on Android; wrapper stays inert.
		print("GodotAdMob: not on Android, plugin disabled.")


func _connect_native_signals() -> void:
	if _plugin == null:
		return
	# Connect native signals to re-emit from this wrapper.
	_plugin.connect("initialized", Callable(self, "_on_initialized"))
	_plugin.connect("ad_loaded", Callable(self, "_on_ad_loaded"))
	_plugin.connect("ad_failed_to_load", Callable(self, "_on_ad_failed_to_load"))
	_plugin.connect("ad_shown", Callable(self, "_on_ad_shown"))
	_plugin.connect("ad_closed", Callable(self, "_on_ad_closed"))
	_plugin.connect("rewarded_earned", Callable(self, "_on_rewarded_earned"))


func _on_initialized() -> void:
	initialized.emit()

func _on_ad_loaded(ad_format: String) -> void:
	ad_loaded.emit(ad_format)

func _on_ad_failed_to_load(error_code: int) -> void:
	ad_failed_to_load.emit(error_code)

func _on_ad_shown(ad_format: String) -> void:
	ad_shown.emit(ad_format)

func _on_ad_closed(ad_format: String) -> void:
	ad_closed.emit(ad_format)

func _on_rewarded_earned(reward_type: String, amount: int) -> void:
	rewarded_earned.emit(reward_type, amount)


func is_plugin_available() -> bool:
	return _plugin != null


## Initialize Mobile Ads. Optionally provide test device hashed IDs and enable/disable UMP consent flow.
## `test_device_ids` may be a PackedStringArray, Array, or comma-separated String.
func initialize(test_device_ids: Variant = PackedStringArray(), use_consent_flow: bool = true) -> void:
	if _plugin == null:
		push_warning("GodotAdMob.initialize(): plugin not available")
		return
	var csv := _to_csv_ids(test_device_ids)
	_plugin.initialize(csv, use_consent_flow)


func _to_csv_ids(ids: Variant) -> String:
	if ids is String:
		return ids
	var parts: Array[String] = []
	if ids is PackedStringArray:
		for s in ids:
			parts.append(str(s))
	elif ids is Array:
		for s in ids:
			parts.append(str(s))
	return ",".join(parts)


func is_initialized() -> bool:
	if _plugin == null:
		return false
	return bool(_plugin.isInitialized())


## --- Banner ---
func load_banner(ad_unit_id: String = TEST_BANNER_AD_UNIT_ID, position: int = BANNER_BOTTOM) -> void:
	if _plugin == null:
		ad_failed_to_load.emit(-1)
		return
	_plugin.loadBanner(ad_unit_id, position)

func show_banner() -> void:
	if _plugin:
		_plugin.showBanner()

func hide_banner() -> void:
	if _plugin:
		_plugin.hideBanner()

func destroy_banner() -> void:
	if _plugin:
		_plugin.destroyBanner()


## --- Interstitial ---
func load_interstitial(ad_unit_id: String = TEST_INTERSTITIAL_AD_UNIT_ID) -> void:
	if _plugin == null:
		ad_failed_to_load.emit(-1)
		return
	_plugin.loadInterstitial(ad_unit_id)

func show_interstitial() -> void:
	if _plugin:
		_plugin.showInterstitial()


## --- Rewarded ---
func load_rewarded(ad_unit_id: String = TEST_REWARDED_AD_UNIT_ID) -> void:
	if _plugin == null:
		ad_failed_to_load.emit(-1)
		return
	_plugin.loadRewarded(ad_unit_id)

func show_rewarded() -> void:
	if _plugin:
		_plugin.showRewarded()
