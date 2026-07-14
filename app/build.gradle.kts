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
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 对应 Flutter --dart-define=MANAPP_BASE_URL=...
        // 构建时可通过 -PMANAPP_BASE_URL=http://xxx 覆盖
        val manappBaseUrl = project.findProperty("MANAPP_BASE_URL") as? String
            ?: "http://app-v1.manxinghai.cn/"
        buildConfigField("String", "MANAPP_BASE_URL", "\"$manappBaseUrl\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("zhuimange.jks")
            storePassword = "1qaz2wsx"
            keyAlias = "manxingkong"
            keyPassword = "1qaz2wsx"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
        create("debugMinify") {
            initWith(getByName("debug"))
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    // TODO: 添加 @Module 注解类后取消注释
    // implementation(libs.koin.annotations)
    // ksp(libs.koin.ksp.compiler)

    // ── Network ──
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // ── Image（Coil 3 + OkHttp 网络引擎） ──
    implementation(libs.coil.compose)
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.1.0")

    // ── Room (本地数据库) ──
    val roomVersion = "2.7.2"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // ── Testing ──
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
