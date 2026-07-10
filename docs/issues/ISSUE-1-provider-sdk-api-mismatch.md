---
title: "Provider SDK API 与 AAR 版本不完全匹配"
status: open
priority: medium
labels: [needs-info, ready-for-human]
created: 2026-07-09
---

# ISSUE-1: Provider SDK API 与 AAR 版本不完全匹配

## 现状

四个广告 Provider 的新增方法（Feed、Interstitial、Reward）代码是基于参考项目 `flutter_merge` 编写的，但 `app/libs/` 中的 AAR 文件版本不同，导致编译时接口方法不匹配。

已在 2026-07-09 修复了编译错误，但部分实现被简化为最小存根：
- **CsjAdProvider**: Feed 的 ExpressRenderListener 使用了简化逻辑（直接取 adView 而不等待渲染回调）
- **UmengAdProvider**: Banner 和 Feed 的 UMNativeLayout 用法需验证运行时行为
- **GdtAdProvider**: Feed listener 新增了参数化回调方法

## 需验证

- [ ] 各 Provider 的 Feed/Interstitial/Reward 方法在真机/模拟器上运行时是否正常
- [ ] 对比 AAR 实际 SDK 文档确认 API 调用是否正确
- [ ] 特别关注 Baidu 激励视频、CSJ 插屏/激励、GDT 插屏的回调完整性

## AAR 版本

| SDK | 文件 | 版本 |
|-----|------|------|
| 百度 | Baidu_MobAds_SDK-release_v9.450.aar | 9.450 |
| 穿山甲 | open_ad_sdk-7.5.1.0.aar | 7.5.1 |
| 优量汇 | GDTSDK.unionNormal.4.680.1550.aar | 4.680 |
| 友盟 | umeng-union-3.3.0.aar | 3.3.0 |
