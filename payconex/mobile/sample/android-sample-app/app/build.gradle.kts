import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Sample-only build-time configuration. payment.properties is ignored by Git, but its values
// become BuildConfig constants inside the APK; ignoring source files does not protect a shipped
// binary. Choose credential provisioning appropriate to your app before distributing it.
val paymentProperties = Properties().apply {
    val paymentPropertiesFile = rootProject.file("payment.properties")
    if (paymentPropertiesFile.isFile) {
        paymentPropertiesFile.inputStream().use(::load)
    }
}

// Precedence: Gradle property > environment variable > local file > empty. This also lets CI
// supply values without committing a file. Keep payment.example.properties blank; rebuild after
// changes and never print these inputs or generated BuildConfig contents in diagnostics.
fun paymentConfigValue(name: String): String =
    providers.gradleProperty(name).orNull
        ?: providers.environmentVariable(name).orNull
        ?: paymentProperties.getProperty(name)
        ?: ""

// Quote/escape for a generated Java string literal; this is syntax escaping, not encryption.
fun String.asBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.bluefin.testaidlgo"
    // Match the existing target SDK; this changes compile-time APIs, not the minimum device API.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.bluefin.testaidlgo"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BLUEPOS_ACCOUNT_ID", paymentConfigValue("BLUEPOS_ACCOUNT_ID").asBuildConfigString())
        buildConfigField("String", "BLUEPOS_API_KEY", paymentConfigValue("BLUEPOS_API_KEY").asBuildConfigString())
        buildConfigField("String", "BLUEPOS_API_SECRET", paymentConfigValue("BLUEPOS_API_SECRET").asBuildConfigString())
        buildConfigField(
            "String",
            "BLUEPOS_ENVIRONMENT",
            paymentConfigValue("BLUEPOS_ENVIRONMENT").ifBlank { "STAGING" }.asBuildConfigString()
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // The screen uses Compose Material 3; legacy AppCompat/ConstraintLayout/View Material
    // dependencies are unnecessary. Keep the vendor AAR separate from the UI toolkit.
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    // Vendor binary pinned to the API used by PaymentHelpers/MainActivity. Re-check request,
    // callback and service/activity contracts against the supplied SDK when changing versions.
    implementation(files("libs/blueposgo-sdk-1.1.0.aar"))
}
