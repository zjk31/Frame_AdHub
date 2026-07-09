plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    // Koin KSP compiler is a dependency (ksp("io.insert-koin:koin-ksp-compiler")), not a plugin
}

android {
    namespace = "com.example.adhub"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.adhub"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // ── AAR: 广告 SDK（从 flutter_merge 项目复制） ──·
    // 友盟
    implementation(fileTree(mapOf("dir" to "libs", "include" to "umeng-*.aar")))
    // 穿山甲
    implementation(fileTree(mapOf("dir" to "libs", "include" to "open_ad_sdk-*.aar")))
    // 优量汇
    implementation(fileTree(mapOf("dir" to "libs", "include" to "GDTSDK*.aar")))
    // 百度
    implementation(fileTree(mapOf("dir" to "libs", "include" to "Baidu_MobAds_SDK-*.aar")))
    implementation(fileTree(mapOf("dir" to "libs", "include" to "Baidu_MobAds_Tools_SDK-*.aar")))
    // 其他
    implementation(fileTree(mapOf("dir" to "libs", "include" to "uyumao-*.aar")))

    // ── AndroidX ──
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)

    // Material Design (View-based, required by ad SDKs)
    implementation(libs.material)

    // ── Compose BOM ──
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.navigation)
    debugImplementation(libs.compose.ui.tooling)

    // ── Koin DI ──
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    // TODO: 添加 @Module 注解类后取消注释
    // implementation(libs.koin.annotations)
    // ksp(libs.koin.ksp.compiler)

    // ── Network ──
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // ── Image ──
    implementation(libs.coil.compose)

    // ── Testing ──
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
