import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

val pluginName = "GodotAdMob"
val pluginPackageName = "org.godotengine.plugin.android.admob"

android {
    namespace = pluginPackageName
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        minSdk = 24

        manifestPlaceholders["godotPluginName"] = pluginName
        manifestPlaceholders["godotPluginPackageName"] = pluginPackageName
        buildConfigField("String", "GODOT_PLUGIN_NAME", "\"${pluginName}\"")
        setProperty("archivesBaseName", pluginName)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}

dependencies {
    // Compile-only: provided by the Godot Android export/runtime.
    compileOnly("org.godotengine:godot:4.7.2.stable")

    // Google Mobile Ads SDK (AdMob). Keep in sync with export_plugin.gd dependencies.
    implementation("com.google.android.gms:play-services-ads:25.5.0")

    // User Messaging Platform (consent). Also provided transitively by ads, but declared
    // explicitly because the plugin calls UMP APIs directly.
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
}

// BUILD TASKS: copy built AARs and export scripts into the installable addon folder.
val copyDebugAARToAddons by tasks.registering(Copy::class) {
    description = "Copies the generated debug AAR binary to the plugin's addons directory"
    from("build/outputs/aar")
    include("$pluginName-debug.aar")
    into("../addons/$pluginName/bin/debug")
}

val copyReleaseAARToAddons by tasks.registering(Copy::class) {
    description = "Copies the generated release AAR binary to the plugin's addons directory"
    from("build/outputs/aar")
    include("$pluginName-release.aar")
    into("../addons/$pluginName/bin/release")
}

val cleanAddons by tasks.registering(Delete::class) {
    delete("../addons/$pluginName/bin")
}

val copyAddons by tasks.registering(Copy::class) {
    description = "Copies the export scripts templates to the plugin's addons directory"

    dependsOn(cleanAddons)
    finalizedBy(copyDebugAARToAddons)
    finalizedBy(copyReleaseAARToAddons)

    from("export_scripts_template")
    into("../addons/$pluginName")
}

tasks.named("assemble").configure {
    finalizedBy(copyAddons)
}

tasks.named<Delete>("clean").apply {
    dependsOn(cleanAddons)
}
