<h1 align="center">
  <div style="text-align:center">
    <img src="https://github.com/user-attachments/assets/4e679094-303b-481d-859d-073efc61037c" alt="logo" style="width:100px; height:auto;">
  </div>
  CraftEngine
</h1>

<p align="center">
  <a href="https://momi.gtemc.cn/craftengine" alt="GitBook">
    <img src="https://img.shields.io/badge/%E6%96%87%E6%A1%A3-%E7%94%A8%E6%88%B7%E6%89%8B%E5%86%8C-D2691E" alt="Gitbook"/>
  </a>
  <a href="https://github.com/Xiao-MoMi/craft-engine/">
    <img src="https://sloc.xyz/github/Xiao-MoMi/craft-engine/?category=codes" alt="SCC数量标识"/>
  </a>
</p>

<p align="center">
    <a target="_blank" href="/README.md">English</a> |
    <a target="_blank" href="/readme/README_zh-CN.md">简体中文</a> |
    <a target="_blank" href="/readme/README_zh-TW.md">繁體中文</a>
</p>

## 📌 关于 CraftEngine

CraftEngine 重新定义了 Minecraft 插件架构，作为下一代自定义内容实现的解决方案。通过 JVM 级别的注入，它提供了前所未有的性能、稳定性和可扩展性。该框架提供了一个代码优先的 API，用于注册原生集成的方块行为和物品交互逻辑。

## 构建

### 🐚 命令行
1. 安装 JDK 21。
2. 打开终端并切换到项目文件夹。
3. 执行 `./gradlew build`，构建产物将生成在 `/target` 文件夹中。

### 💻 IDE
1. 导入项目并执行 Gradle 构建操作。
2. 构建产物将生成在 `/target` 文件夹中。

## 安装

### 💻 环境要求
1. 确保您正在运行 [Paper](https://papermc.io/)（或其分支）1.20.1+ 服务器。CraftEngine 不支持 Spigot，且未来也不太可能支持。
2. 使用 JDK 21 来运行服务器。

### 🔍 安装方式
CraftEngine 提供了两种安装模式：标准安装和 Mod 模式。标准安装与传统插件安装方式相同，即将插件放入插件文件夹中。下面我们将详细介绍 Mod 模式的安装步骤。

### 🔧 安装服务器 Mod
1. 下载最新的 [ignite.jar](https://github.com/vectrix-space/ignite/releases) 到您的服务器根目录。
2. 选择以下任一操作：
    - 将您的服务器 JAR 文件重命名为 `paper.jar`
    - 添加启动参数：`-Dignite.locator=paper -Dignite.paper.jar=./paper-xxx.jar`
    - 示例：`java -Dignite.locator=paper -Dignite.paper.jar=./paper-1.21.4-164.jar -jar ignite.jar`
3. 启动服务器以生成 `/mods` 目录。
4. 将最新的 [mod.jar](https://github.com/Xiao-MoMi/craft-engine/releases) 放入 `/mods` 文件夹。
5. 将插件的 JAR 文件放入 `/plugins` 文件夹进行安装。
6. 执行两次重启：
    1. 第一次重启用于文件初始化。
    2. 第二次重启以激活所有组件。

## 技术概述

### ⚙️ 方块
CraftEngine 使用运行时字节码生成技术，在服务器原生级别注册自定义方块，并结合客户端数据包修改以实现视觉同步。此架构提供了以下功能：

🧱 自定义原生方块
- 动态注册方块，完全可控。
- 物理属性：硬度、引燃几率、亮度等所有标准属性。
- 自定义行为：通过 API 实现树苗、作物、下落的方块等。
- 原版兼容性：完全保留原版方块机制（例如音符盒、绊线）。

📦 数据包集成
- 定义自定义矿脉。
- 生成自定义树木。
- 配置自定义地形生成。

⚡ 性能优势
- 比传统的 Bukkit 事件监听器更快、更稳定。
- 策略性代码注入以最小化开销。

### 🥘 配方
CraftEngine 通过底层注入实现完全可定制的合成系统。与传统插件不同，它在处理 NBT 修改时不会失效，确保配方结果仅与唯一的物品标识符绑定。

### 🪑 家具
该插件使用核心实体来存储家具元数据，同时将碰撞实体和模块组件作为客户端数据包传输。此架构实现了显著的服务器端性能优化，同时支持通过多部分物品集成实现复合家具组装。

### 📝 模板
鉴于插件配置的广泛性和复杂性，CraftEngine 实现了模块化模板系统以分隔关键设置。这使得用户可以自定义配置格式，同时显著减少冗余的 YAML 定义。

### 🛠️ 模型
该插件通过配置实现模型继承和纹理覆盖，同时支持从 1.21.4 版本开始的[所有物品模型](https://misode.github.io/assets/item/)。它包含一个版本迁移系统，可以自动将 1.21.4+ 的物品模型降级为旧格式，以实现最大向后兼容性。

### ❗️ 您必须了解和部分插件的不兼容性
- CraftEngine 会注入到 PalettedContainer 中，以确保插件的块数据会被高效的存储与同步。这有可能会导致与一些修改palette的其他插件发生冲突。当您使用 Spark 来分析服务器性能时，palette的操作导致的性能开销，将会在Spark的分析结果中归因于 CraftEngine 插件。
- CraftEngine 会注入到 FurnaceBlockEntity 中，来修改它的配方获取逻辑.
- CraftEngine 会使用真实的server-side blocks, 任何依赖于Bukkit的Material类的插件，都会无法正确识别自定义方块类型。 正确的方法是使用像 BlockState#getBlock(mojmap) 这样的替代方式，而不是使用 Material 类。
- CraftEngine 通过扩展某些 Minecraft 实体来实现 0-tick 碰撞实体, 确保碰撞在服务器端可以正常工作（例如，让猪站在椅子上）. 然而，一些反作弊插件在检测玩家移动时没有正确检查实体 AABB（Axis-Aligned Bounding Box），这可能会导致误报。
- CraftEngine 的自定义配方处理可能会与其他配方插件 不完全兼容。
## 灵感来源
CraftEngine 从以下开源项目中汲取了灵感：
- [Paper](https://github.com/PaperMC/Paper)
- [LuckPerms](https://github.com/LuckPerms/LuckPerms)
- [Fabric](https://github.com/FabricMC/fabric)
- [packetevents](https://github.com/retrooper/packetevents)
- [DataFixerUpper](https://github.com/Mojang/DataFixerUpper)
- [ViaVersion](https://github.com/ViaVersion/ViaVersion)

### 核心依赖
CraftEngine 的实现依赖于以下基础库：
- [ignite](https://github.com/vectrix-space/ignite)
- [cloud-minecraft](https://github.com/Incendo/cloud-minecraft)
- [rtag](https://github.com/saicone/rtag)
- [adventure](https://github.com/KyoriPowered/adventure)
- [byte-buddy](https://github.com/raphw/byte-buddy)

## 如何贡献

### 🔌 新功能与 Bug 修复
如果您提交的 PR 是关于 Bug 修复的，它很可能会被合并。如果您想提交新功能，请提前在 [Discord](https://discord.com/invite/WVKdaUPR3S) 上联系我。

### 🌍 翻译
1. 克隆此仓库。
2. 在 `/bukkit/loader/src/main/resources/translations` 中创建一个新的语言文件。
3. 完成后，提交 **pull request** 以供审核。我们感谢您的贡献！

## 版本之间的差异
| 版本           | 官方支持 | 最大玩家数 | 在线模式需求 | 商用 |
|-------------------|------------------|-------------|----------------------|----------------|
| Community Edition | ❌ 不支持             | 20          | ✔️ 需要               | ✔️ Allowed(社区版)     |
| GitHub Edition    | ❌ 不支持             | 无限   | ❌ 无需                 | ✔️ Allowed(GitHub版)     |
| Premium Edition   | ✔️ 支持           | 无限   | ❌ 无需                 | ✔️ Allowed(高级版)     |

### 💖 支持开发者
如果您喜欢使用 CraftEngine，请考虑支持开发者！

- **Polymart**: [无]
- **BuiltByBit**: [无]
- **爱发电**: [通过爱发电支持](https://afdian.com/@xiaomomi/)

## CraftEngine API

```kotlin
repositories {
    maven("https://repo.momirealms.net/releases/")
    // 如果你的网络环境受限可以尝试下面的存储库地址
    // maven("https://repo-momi.gtemc.cn/releases/")
}
```
```kotlin
dependencies {
    compileOnly("net.momirealms:craft-engine-core:0.0.54")
    compileOnly("net.momirealms:craft-engine-bukkit:0.0.54")
}
```
