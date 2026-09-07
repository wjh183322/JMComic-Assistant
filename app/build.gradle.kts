plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "com.jinman.assistant"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jinman.assistant"
        minSdk = 26
        targetSdk = 35
        versionCode = 24
        versionName = "1.4.4"
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
}

// APK 文件名：jm-assistant-v版本号.apk
// 编完后复制到工程上一级的 apk 文件夹，例如 C:\Users\Jiahao\jm-assistant\apk\
android.applicationVariants.configureEach {
    val ver = versionName
    outputs.configureEach {
        (this as com.android.build.gradle.api.ApkVariantOutput).outputFileName = "jm-assistant-v$ver.apk"
    }
}

fun copyBuiltApk(fromSubdir: String) {
    val fromDir = layout.buildDirectory.dir("outputs/apk/$fromSubdir").get().asFile
    val dest = rootProject.projectDir.resolve("../apk").normalize()
    dest.mkdirs()
    fromDir.listFiles()?.filter { it.extension.equals("apk", ignoreCase = true) }?.forEach { apk ->
        apk.copyTo(dest.resolve(apk.name), overwrite = true)
    }
}

afterEvaluate {
    tasks.named("assembleDebug").configure {
        doLast { copyBuiltApk("debug") }
    }
    tasks.named("assembleRelease").configure {
        doLast { copyBuiltApk("release") }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
