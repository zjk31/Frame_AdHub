plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    // Koin KSP compiler is a dependency (ksp("io.insert-koin:koin-ksp-compiler")), not a plugin
}

android {
    namespace = "cn.manxinghai.zhuimange"
    compileSdk = 36

    defaultConfig {
        applicationId = "cn.manxinghai.zhuimange"
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
        buildConfig = true
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
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)

    // Material Design (View-based, required by ad SDKs)
    implementation(libs.material)
    // 穿山甲 SDK 需要旧 support 库（对齐 flutter_merge）—— Jetifier 仅转换字节码，SDK 内部可能直接依赖
    implementation("com.android.support:recyclerview-v7:28.0.0")
    implementation("com.android.support:support-v4:28.0.0")

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
    // Koin Annotations 不在本骨架使用——统一用 DSL

    // ── Network ──
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // ── Image（Coil 3 + OkHttp 网络引擎） ──
    implementation(libs.coil.compose)
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.1.0")

    // OAID 设备标识
    implementation("com.github.gzu-liyujiang:Android_CN_OAID:4.2.11")

    // ── Testing ──
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
