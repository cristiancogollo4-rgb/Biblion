import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

val keyProperties = Properties()
val keyPropertiesFile = rootProject.file("key.properties")

if (keyPropertiesFile.exists()) {
    keyPropertiesFile.inputStream().use(keyProperties::load)
}

val ciKeystorePath = System.getenv("CM_KEYSTORE_PATH")
val ciKeystorePassword = System.getenv("CM_KEYSTORE_PASSWORD")
val ciKeyAlias = System.getenv("CM_KEY_ALIAS")
val ciKeyPassword = System.getenv("CM_KEY_PASSWORD")
val hasCiSigning = listOf(
    ciKeystorePath,
    ciKeystorePassword,
    ciKeyAlias,
    ciKeyPassword
).all { !it.isNullOrBlank() }

val localKeystorePath = keyProperties.getProperty("storeFile")
val localKeystorePassword = keyProperties.getProperty("storePassword")
val localKeyAlias = keyProperties.getProperty("keyAlias")
val localKeyPassword = keyProperties.getProperty("keyPassword")
val hasLocalSigning = listOf(
    localKeystorePath,
    localKeystorePassword,
    localKeyAlias,
    localKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.cristiancogollo.biblion"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cristiancogollo.biblion"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            when {
                hasCiSigning -> {
                    storeFile = file(ciKeystorePath!!)
                    storePassword = ciKeystorePassword
                    keyAlias = ciKeyAlias
                    keyPassword = ciKeyPassword
                }
                hasLocalSigning -> {
                    storeFile = file(localKeystorePath!!)
                    storePassword = localKeystorePassword
                    keyAlias = localKeyAlias
                    keyPassword = localKeyPassword
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasCiSigning || hasLocalSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.ui)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.compose.ui.ui)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.richeditor.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.material.icons.extended)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
}
