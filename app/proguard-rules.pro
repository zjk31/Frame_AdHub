# ── 通用 ──
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes EnclosingMethod,InnerClasses

# ── 穿山甲/Pangle ──
-keep class com.bytedance.sdk.openadsdk.** { *; }
-keep class com.bytedance.sdk.framework.** { *; }
-keep class com.bytedance.sdk.openadsdk.mediation.** { *; }
-keep class com.bytedance.sdk.openadsdk.adapter.** { *; }
-keep class com.bytedance.sdk.component.** { *; }
-dontwarn com.bytedance.sdk.**

# ── 优量汇/GDT ──
-keep class com.qq.e.** { *; }
-keep class com.qq.e.comm.** { *; }
-keep class com.qq.e.ads.** { *; }
-keep class com.tencent.gdt.** { *; }
-dontwarn com.qq.e.**
-dontwarn com.tencent.gdt.**

# ── 百度网盟 ──
-keep class com.baidu.mobads.** { *; }
-keep class com.baidu.mobads.sdk.api.** { *; }
-keep class com.baidu.mobads.openad.** { *; }
-keep class com.baidu.android.common.** { *; }
-dontwarn com.baidu.mobads.**

# ── 友盟 ──
-keep class com.umeng.** { *; }
-keep class com.umeng.union.** { *; }
-keep class com.umeng.commonsdk.** { *; }
-dontwarn com.umeng.**

# ── Kotlin Coroutines ──
-keepclassmembernames class kotlinx.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# ── Retrofit ──
-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ── OkHttp ──
-dontwarn okhttp3.**
-dontwarn okio.**

# ── Koin ──
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# ── Gson 序列化 ──
-keep class cn.manxinghai.zhuimange.data.dto.** { *; }
-keep class cn.manxinghai.zhuimange.data.remote.** { *; }
-keep class cn.manxinghai.zhuimange.domain.model.** { *; }
-keep class cn.manxinghai.zhuimange.domain.repository.** { *; }
-keep class cn.manxinghai.zhuimange.data.repository.AdConfigRepositoryImpl { *; }

# ── 应用核心类 ──
-keep class cn.manxinghai.zhuimange.MainActivity { *; }
-keep class cn.manxinghai.zhuimange.SplashAdActivity { *; }
-keep class cn.manxinghai.zhuimange.AdHubApp { *; }
