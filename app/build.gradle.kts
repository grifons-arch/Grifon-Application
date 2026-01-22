import com.android.build.api.variant.BuildConfigField

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val grApiBaseUrl = (project.findProperty("API_BASE_URL_GR") as String?)
    ?: "https://grifon.gr/api/"
val seApiBaseUrl = (project.findProperty("API_BASE_URL_SE") as String?)
    ?: "https://grifon.se/api/"

android {
    namespace = "com.example.grifon"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.grifon"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    flavorDimensions += "shop"
    productFlavors {
        create("gr") {
            dimension = "shop"
        }
        create("se") {
            dimension = "shop"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

androidComponents {
    onVariants { variant ->
        val (apiBaseUrl, shopId) = when (variant.flavorName) {
            "gr" -> grApiBaseUrl to "4"
            "se" -> seApiBaseUrl to "1"
            else -> grApiBaseUrl to "4"
        }
        variant.buildConfigFields?.put(
            "API_BASE_URL",
            BuildConfigField("String", "\"$apiBaseUrl\"", "Gateway base URL"),
        )
        variant.buildConfigFields?.put(
            "SHOP_ID",
            BuildConfigField("String", "\"$shopId\"", "Gateway shop identifier"),
        )
    }
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.material)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.google.play.services.auth)
}
