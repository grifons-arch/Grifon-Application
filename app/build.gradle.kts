import com.android.build.api.variant.BuildConfigField
import java.util.Properties
import java.net.NetworkInterface
import java.net.Inet4Address

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// Λειτουργία που βρίσκει την τοπική IP του υπολογιστή σου αυτόματα
fun getLocalIp(): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (iface in interfaces) {
            if (iface.isLoopback || !iface.isUp) continue
            val addresses = iface.inetAddresses
            for (addr in addresses) {
                if (addr is Inet4Address) {
                    val ip = addr.hostAddress
                    // Επιστρέφει την πρώτη IP που μοιάζει με οικιακή (192.168.x.x ή 10.x.x.x)
                    if (ip.startsWith("192.168.") || ip.startsWith("10.")) {
                        return ip
                    }
                }
            }
        }
    } catch (e: Exception) {
        // ignore
    }
    return "10.0.2.2" // Fallback για emulator αν αποτύχει η ανίχνευση
}

// Διαβάζουμε το local.properties για να πάρουμε την IP δυναμικά
val localProps = Properties()
val localPropsFile = project.rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localProps.load(localPropsFile.inputStream())
}

val apiBaseUrlFromProps = localProps.getProperty("API_BASE_URL")
val mapsApiKeyFromProps = localProps.getProperty("MAPS_API_KEY") ?: ""

// ΑΥΤΟΜΑΤΙΣΜΟΣ: Αν δεν υπάρχει IP στο local.properties, τη βρίσκουμε μόνοι μας
val computerIp = if (apiBaseUrlFromProps.isNullOrBlank()) getLocalIp() else null
val defaultGatewayUrl = apiBaseUrlFromProps ?: "http://$computerIp:3000/"

val grApiBaseUrl = (project.findProperty("API_BASE_URL_GR") as String?) ?: defaultGatewayUrl
val seApiBaseUrl = (project.findProperty("API_BASE_URL_SE") as String?) ?: defaultGatewayUrl
val debugApiBaseUrl = (project.findProperty("API_BASE_URL_DEBUG") as String?) ?: defaultGatewayUrl

android {
    namespace = "com.example.grifon"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.grifon.eshop"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.1.0"
        
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKeyFromProps
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKeyFromProps\"")
    }

    flavorDimensions += "shop"
    productFlavors {
        create("gr") { dimension = "shop" }
        create("se") { dimension = "shop" }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

androidComponents {
    onVariants { variant ->
        var (apiBaseUrl, shopId) = when (variant.flavorName) {
            "gr" -> grApiBaseUrl to "4"
            "se" -> seApiBaseUrl to "1"
            else -> grApiBaseUrl to "4"
        }
        if (variant.buildType == "debug") apiBaseUrl = debugApiBaseUrl
        variant.buildConfigFields?.put("API_BASE_URL", BuildConfigField("String", "\"$apiBaseUrl\"", "Gateway base URL"))
        variant.buildConfigFields?.put("SHOP_ID", BuildConfigField("String", "\"$shopId\"", "Gateway shop identifier"))
    }
}

dependencies {
    constraints {
        implementation(libs.javapoet)
        ksp(libs.javapoet)
    }
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.material)
    implementation(libs.androidx.navigation.compose)
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.javapoet)
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
    implementation("com.google.android.libraries.places:places:3.3.0")
    
    // Google Places API
    implementation("com.google.android.libraries.places:places:3.5.0")
    
    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
}
