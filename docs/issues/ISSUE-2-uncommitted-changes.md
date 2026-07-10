---
title: "feature/banner 分支未提交变更摘要"
status: open
priority: high
labels: [ready-for-agent]
created: 2026-07-09
---

# ISSUE-2: feature/banner 分支未提交变更

## 变更范围

17 个修改文件 + 6 组新增文件，约 +1100 行代码。

## 新增功能

- [x] **PureModeManager** — 纯净模式（2h 免广告窗口）
- [x] **HotStartInterstitialManager** — 热启动插屏自动展示
- [x] **Feed 广告 Screen + ViewModel** — 信息流
- [x] **Interstitial 广告 Screen + ViewModel** — 插屏
- [x] **Reward 广告 Screen + ViewModel** — 激励视频（支持 4 种 slotKey）
- [x] 四通道 Provider 扩展 — Feed/Interstitial/Reward 方法
- [x] Koin DI 注册新组件
- [x] Navigation 路由扩展
- [x] compileSdk 35→36 适配
- [x] lifecycle-process 依赖添加

## 建议

变更量大，建议拆分为多个 commit 提交：
1. `chore: compileSdk 36 + lifecycle-process dependency`
2. `feat(core): PureModeManager + HotStartInterstitialManager`
3. `feat(providers): 四通道 Feed/Interstitial/Reward 实现`
4. `feat(ui): Feed/Interstitial/Reward 页面`
5. `feat(nav): 路由 + DI 注册`
