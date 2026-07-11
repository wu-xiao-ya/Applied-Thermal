# 应用热力 | Applied Thermal

应用热力是一个面向 Minecraft 1.20.1 Forge 的 AE2 / Thermal 联动模组，为受支持的 Thermal 机器提供样板供应升级。

Applied Thermal is a Minecraft 1.20.1 Forge addon for AE2 and Thermal that adds a pattern-provider augment for supported Thermal machines.

## 三分支矩阵

| 分支 | Minecraft | 组合 | 状态 |
| --- | --- | --- | --- |
| `1.20.1-forge-ae2` | 1.20.1 | Forge + AE2 + Thermal | 维护中 |
| `1.12.2-ae2s` | 1.12.2 | Cleanroom / Forge + AE2S | 维护中 |
| `1.12.2-ae2uel` | 1.12.2 | AE2 UEL | 维护中 |

每个分支独立维护，Java、Gradle 和资源不要跨分支混用。

## 1.20.1 特性

- `assets/appliedthermal/lang` 下的中英文本地化
- 原生 AE2 样板供应逻辑和 AE2 风格独立界面
- 36 槽样板矩阵和产物回网切换按钮
- 可选 AppFlux 感应卡自供电，只给当前 Thermal 机器供能
- `pattern_provider_augment` 合成配方和解锁进度
- 原创 GUI 背景、图标和纹理资源

## 安装

1. 安装 Forge `47.1.47` 或更新的 47.x 版本。
2. 安装 AE2 `15.4.10`、CoFH Core `11.1.0`、Thermal Core `11.1.0` 和 Thermal Expansion `11.1.0`。
3. 需要网络自供电时，额外安装 AppFlux `1.20-1.3.7-forge` 及其依赖 Glodium。
4. 合成样板供应升级并装入受支持的 Thermal 机器。

## 构建

使用 Java 17：

```powershell
.\gradlew.bat --no-daemon build --stacktrace
```

需要在开发客户端中加载 AppFlux 与 Glodium 时：

```powershell
.\gradlew.bat --no-daemon runClient -Penable_appflux_runtime=true
```

GitHub Actions 会从锁定提交构建 CoFH Core、Thermal Core 和 Thermal Expansion `11.1.0`，再构建本仓库。
