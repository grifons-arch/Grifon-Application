import com.android.build.api.variant.BuildConfigField

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val defaultGatewayUrl = (project.findProperty("API_BASE_URL") as String?)
    ?: "http://10.0.2.2:3000/"
val grApiBaseUrl = (project.findProperty("API_BASE_URL_GR") as String?)
    ?: defaultGatewayUrl
val seApiBaseUrl = (project.findProperty("API_BASE_URL_SE") as String?)
    ?: defaultGatewayUrl
val debugApiBaseUrl = (project.findProperty("API_BASE_URL_DEBUG") as String?)
    ?: defaultGatewayUrl

android {
    namespace = "com.example.grifon"
    compileSdk = 34



    defaultConfig {
        applicationId = "com.grifon.eshop"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.1.0"
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
        var (apiBaseUrl, shopId) = when (variant.flavorName) {
            "gr" -> grApiBaseUrl to "4"
            "se" -> seApiBaseUrl to "1"
            else -> grApiBaseUrl to "4"
        }
        if (variant.buildType == "debug") {
            apiBaseUrl = debugApiBaseUrl
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

val resourceNameRegex = Regex("^[a-z0-9_]+$")

fun suggestedResourceName(file: File): String {
    val fileName = file.name
    val baseName = fileName.substringBeforeLast('.')
    val extension = fileName.substringAfterLast('.', "")
    val isNinePatch = baseName.endsWith(".9")
    val rawBase = if (isNinePatch) baseName.removeSuffix(".9") else baseName
    val normalizedBase = rawBase
        .lowercase()
        .replace(Regex("[^a-z0-9_]"), "_")
        .replace(Regex("_+"), "_")
        .trim('_')
        .ifBlank { "resource" }
    val normalizedWithNinePatch = if (isNinePatch) "${normalizedBase}.9" else normalizedBase
    return if (extension.isNotEmpty()) {
        "$normalizedWithNinePatch.$extension"
    } else {
        normalizedWithNinePatch
    }
}

tasks.register("normalizeResourceNames") {
    group = "verification"
    description = "Renames invalid Android resource files to lowercase underscore naming."
    doLast {
        val invalidResources = fileTree("src/main/res") {
            include("**/*.*")
        }.files.filter { file ->
            val fileName = file.name
            val baseName = fileName.substringBeforeLast('.')
            val sanitizedBaseName = if (baseName.endsWith(".9")) {
                baseName.removeSuffix(".9")
            } else {
                baseName
            }
            !resourceNameRegex.matches(sanitizedBaseName)
        }

        invalidResources.sortedBy { it.path }.forEach { file ->
            val targetName = suggestedResourceName(file)
            val targetFile = File(file.parentFile, targetName)
            if (targetFile.exists()) {
                throw GradleException("Cannot rename ${file.path} to ${targetFile.path}: target already exists.")
            }
            if (!file.renameTo(targetFile)) {
                throw GradleException("Failed to rename ${file.path} to ${targetFile.path}.")
            }
        }
    }
}

tasks.register("validateResourceNames") {
    group = "verification"
    description = "Ensures Android resource file names only contain lowercase letters, digits, or underscores."
    doLast {

        val invalidResources = fileTree("src/main/res") {
            include("**/*.*")
        }.files.filter { file ->
            val fileName = file.name
            val baseName = fileName.substringBeforeLast('.')
            val sanitizedBaseName = if (baseName.endsWith(".9")) {
                baseName.removeSuffix(".9")
            } else {
                baseName
            }
            !resourceNameRegex.matches(sanitizedBaseName)
        }
        if (invalidResources.isNotEmpty()) {
            val names = invalidResources.sortedBy { it.path }.joinToString(separator = "\n") { it.path }
            throw GradleException(
                buildString {
                    append("Invalid Android resource file names detected:\n")
                    append(names)
                    append("\nResource file names must contain only lowercase a-z, 0-9, or underscore.\n")
                    append("Run ./gradlew :app:normalizeResourceNames to auto-rename them.\n")
                    append("Suggested renames:\n")
                    invalidResources.sortedBy { it.path }.forEach { file ->
                        append("${file.path} -> ${suggestedResourceName(file)}\n")
                    }
                }
            )
        }
    }
}
tasks.named("preBuild") {
    dependsOn("validateResourceNames")
}

dependencies {
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
