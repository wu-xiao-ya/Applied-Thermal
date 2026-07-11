# Applied Thermal | 应用热力

**应用热力**是一个面向 Minecraft 1.12.2 Cleanroom/Forge 的联动模组，将 Applied Energistics 2 - Supergiant 的样板供应能力与 Thermal Expansion 机器结合。

Applied Thermal is a Minecraft 1.12.2 Cleanroom/Forge addon that integrates Applied Energistics 2 - Supergiant pattern providers with Thermal Expansion machines.

安装“样板供应升级”后，受支持的 Thermal `TileMachineBase` 机器会成为 AE2S 样板供应器宿主，可接入相邻 ME 线缆、公开处理样板、接受自动合成输入，并按设置将产物送回网络。模组的完整目标范围只包括 Thermal 机器；能源炉、装置、储物方块、能量单元和便携容器不在支持范围内。

The current AE2S branch requires AE2S `1.0.9+278077b98` or newer. This is the first build that exposes the pattern-provider toolbar initialization event used by Applied Thermal.

## 分支 / Branches

Each supported Minecraft and AE implementation is maintained on its own long-lived branch. Code from different implementations must not be placed on the same branch.

| Branch | Minecraft | AE implementation | Status |
| --- | --- | --- | --- |
| `1.12.2-ae2s` | 1.12.2 | Applied Energistics 2 - Supergiant | Active |
| `1.12.2-ae2uel` | 1.12.2 | AE2 Unofficial Extended Life | Planned |

Future branches should follow `<minecraft-version>-<ae-implementation>` and append the mod loader when one Minecraft version needs separate Forge, Fabric, or NeoForge implementations.

## 使用 / Usage

将“样板供应升级”安装到 Thermal Expansion 机器中，并在机器相邻面连接 AE2S 线缆。机器进入网络后，可通过机器 GUI 右侧的 AE 按钮打开原生样板供应器界面。

界面只接受处理样板。默认启用 9 个样板槽，每安装一张 AE2S 样板扩展卡增加 9 个可用槽，最多启用 36 个。

产物回网默认开启。样板供应器左侧工具栏底部的活塞按钮用于切换该功能：普通活塞表示开启，带红色 X 表示关闭。当网络无法完整接收产物时，产物会保留在机器输出槽中。

取出“样板供应升级”时，机器会弹出内部保存的样板、AE2S 扩展卡、待发送物品和返回缓存物品，避免附件停用后物品不可访问。

Applied-Flux 安装后，可在样板供应器的专用卡槽中使用其感应卡，为当前 Thermal 机器本体供能；该联动不会把机器变成通用 FE 中继器。

## 依赖 / Dependencies

- Minecraft 1.12.2
- Cleanroom `0.5.14+`，或 Forge `14.23.5.2859` 搭配兼容的 Mixin 环境
- Applied Energistics 2 - Supergiant `1.0.9+278077b98` 或更新版本
- CoFHCore、Thermal Foundation、Thermal Expansion
- Applied-Flux：可选，仅用于感应卡供能

## 构建 / Build

Use Java 25:

```powershell
.\gradlew.bat --no-daemon build --stacktrace
```

CI runs the same build and uploads the generated jars.
