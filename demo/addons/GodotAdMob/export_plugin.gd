@tool
extends EditorPlugin

# EditorExportPlugin that packages the GodotAdMob Android plugin (v2).
# It registers the AARs and required Gradle dependencies for Android exports.

const PLUGIN_NAME := "GodotAdMob"

var export_plugin: AndroidExportPlugin

func _enter_tree() -> void:
	export_plugin = AndroidExportPlugin.new()
	add_export_plugin(export_plugin)


func _exit_tree() -> void:
	if export_plugin:
		remove_export_plugin(export_plugin)
		export_plugin = null


class AndroidExportPlugin extends EditorExportPlugin:
	func _supports_platform(platform) -> bool:
		return platform is EditorExportPlatformAndroid

	func _get_android_libraries(platform, debug: bool) -> PackedStringArray:
		if debug:
			return PackedStringArray([PLUGIN_NAME + "/bin/debug/" + PLUGIN_NAME + "-debug.aar"])
		return PackedStringArray([PLUGIN_NAME + "/bin/release/" + PLUGIN_NAME + "-release.aar"])

	func _get_android_dependencies(platform, debug: bool) -> PackedStringArray:
		# Remote Maven dependencies required by this plugin.
		# Keep versions in sync with plugin/build.gradle.kts.
		return PackedStringArray([
			"com.google.android.gms:play-services-ads:25.5.0",
			"com.google.android.ump:user-messaging-platform:4.0.0"
		])

	func _get_name() -> String:
		return PLUGIN_NAME
