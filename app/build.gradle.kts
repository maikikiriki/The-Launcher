plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.aboutlibraries)
}

android {
    // Only an explicitly provisioned owner-controlled key may sign release builds.
    // PR builds receive none of these environment variables.
    val stableSigning = listOf("ACRONIX_KEYSTORE_FILE", "ACRONIX_KEYSTORE_PASSWORD",
        "ACRONIX_KEY_ALIAS", "ACRONIX_KEY_PASSWORD").map { providers.environmentVariable(it).orNull }
    require(stableSigning.all { it == null } || stableSigning.all { !it.isNullOrBlank() }) {
        "Incomplete Acronix release signing configuration"
    }
    if (stableSigning.all { !it.isNullOrBlank() }) {
        signingConfigs.create("stable") {
            storeFile = file(stableSigning[0]!!)
            storePassword = stableSigning[1]
            keyAlias = stableSigning[2]
            keyPassword = stableSigning[3]
        }
    }
    namespace = "com.galaxyrio.gracelauncher"
    compileSdk = 37
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = "com.acronix.launcher"
        minSdk = 28
        targetSdk = 37
        versionCode = 4
        versionName = "0.1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("stable")
            optimization {
                enable = true
            }
        }
        create("previewDebug") {
            initWith(getByName("debug"))
            // Safe isolated installation alongside existing Acronix installs.
            // Do not change the original applicationId or remove its data.
            applicationIdSuffix = ".preview"
            versionNameSuffix = "-preview"
            matchingFallbacks += listOf("debug")
        }
        create("fullDebug") {
            initWith(getByName("debug"))
            matchingFallbacks += listOf("debug")
            // Only this opt-in build declares the Accessibility and Notification services.
            // Android still requires the user to grant each service explicitly.
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    androidResources {
        // Map bundled fonts directly; variable weights share the same native
        // font buffer instead of inflating separate copies on the UI thread.
        noCompress += "ttf"
    }
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
    lint {
        // Community translations can be incomplete; Android falls back to English.
        // Keep reporting missing strings without blocking release builds.
        warning += "MissingTranslation"
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation("androidx.window:window:1.5.1")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.room.runtime)
    implementation(libs.material.kolor)
    implementation(libs.aboutlibraries.core)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

aboutLibraries {
    offlineMode = true
    collect {
        includeTestVariants = false
        fetchRemoteLicense = false
        fetchRemoteFunding = false
    }
    export {
        excludeFields.add("License.content")
    }
}
