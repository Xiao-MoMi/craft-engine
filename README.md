<h1 align="center">
  <div style="text-align:center">
    <img src="https://github.com/user-attachments/assets/4e679094-303b-481d-859d-073efc61037c" alt="logo" style="width:100px; height:auto;">
  </div>
  CraftEngine
</h1>

<p align="center">
  <a href="https://github.com/Xiao-MoMi/craft-engine/">
    <img src="https://sloc.xyz/github/Xiao-MoMi/craft-engine/?category=code" alt="Scc Count Badge"/>
  </a>
  <a href="https://deepwiki.com/Xiao-MoMi/craft-engine">
    <img src="https://deepwiki.com/badge.svg" alt="Ask DeepWiki">
  </a>
  <a href="https://mo-mi.gitbook.io/xiaomomi-plugins/craftengine" alt="GitBook">
    <img src="https://img.shields.io/badge/Docs-User Manual-D2691E" alt="Gitbook"/>
  </a>
</p>

<p align="center">
    <a target="_blank" href="/README.md">English</a> |
    <a target="_blank" href="/readme/README_zh-CN.md">简体中文</a> |
    <a target="_blank" href="/readme/README_zh-TW.md">繁體中文</a>
</p>

## 📌 About CraftEngine
CraftEngine redefines plugin architecture as a next-generation solution for custom content implementation. Its JVM-level injection delivers unprecedented performance, stability, and extensibility. The framework provides a code-first API for registering natively integrated block behaviors and item interaction logic.

## Build
Getting the full jar is completely free, as long as you have a device with jdk21 installed. Follow the guide below to build it.

### 🐚 Command Line
+ Start terminal and change directory to the project folder.
+ Execute "./gradlew build" and get the artifact under /target folder.

### 💻 IDE
+ Import the project and execute gradle build action.
+ Get the artifact under /target folder.

## Installation

### 💻 Enviroment Requirements
+ First, ensure that you are running a [Paper](https://papermc.io/)/[Folia](https://github.com/PaperMC/Folia) (or its fork) 1.20.1+ server. The plugin does not support Spigot and is unlikely to do so in the future.
+ Secondly, please use JDK 21 to run the server. I believe this is quite straightforward for you.

### 🔍 How to Install
CraftEngine offers two installation modes: Standard Installation and Mod Mode. As the name suggests, Standard Installation involves placing the plugin into your plugins folder just like any conventional plugin. Below, we will provide a detailed explanation on how to install using mod mode.

### 🔧 Install Server Mod
- Download the latest [ignite.jar](https://github.com/vectrix-space/ignite/releases) into your server's root directory
- Either:
    - Rename your server JAR to `paper.jar` and modify startup command to: `-jar ignite.jar`
- Or:
    - Use advanced launch arguments 
       - For paper/folia: `-Dignite.locator=paper -Dignite.paper.jar=./server-xxx.jar -jar ignite.jar` 
       - For some special paper forks `-Dignite.locator=paper -Dignite.paper.target=cn.dreeam.leaper.QuantumLeaper -Dignite.paper.jar=./leaf-xxx.jar -jar ignite.jar`
- Start the server to generate the `/mods` directory
- Place the latest [mod.jar](https://github.com/Xiao-MoMi/craft-engine/releases) in `/mods`
- Install the plugin by placing its JAR in `/plugins`
- Perform two restarts:
    1. Initial restart for file initialization
    2. Final restart to activate all components

## Technical Overview

### ⚙️ Blocks
The plugin employs runtime bytecode generation to register custom blocks at the server-native level, combined with client-side data packet modification for visual synchronization. This architecture provides:

🧱 Native Block Customization
+ Dynamically register blocks with complete control over
+ Physics: hardness, flammability, light emission, etc.
+ Custom Behaviors: Tree saplings, crops, falling blocks, and more via API
+ Vanilla Compatibility: Full preservation of vanilla block mechanism (e.g., note blocks, tripwires)

📦 Data Pack Integration
+ Define custom ore veins
+ Generate custom trees
+ Configure custom terrain generation

⚡ Performance Advantages
+ Significantly faster and stabler than traditional Bukkit event listeners
+ Strategic code injection for minimal overhead

### 🥘 Recipes
CraftEngine implements a fully customizable crafting system via low-level injection. Unlike conventional plugins that fail with NBT modifications, it ensures resilient handling of item metadata - recipe outcomes remain exclusively tied to unique item identifiers.

### 🪑 Furniture
The plugin utilizes a core entity to store furniture metadata, while transmitting collision entities and modular components as client-bound packets. This architecture achieves significant server-side performance optimization, while supporting composite furniture assembly through multi-part item integration.

### 📝 Templates
Given the extensive and intricate nature of plugin configurations, a modular template system is implemented to compartmentalize critical settings. This enables user-customizable configuration formats while significantly minimizing redundant YAML definitions.

### 🛠️ Models
The plugin enables model inheritance and texture overrides through configuration, while supporting [all item models](https://misode.github.io/assets/item/) from version 1.21.4 onward. It incorporates a version migration system that automatically downgrades 1.21.4+ item models to legacy formats with maximum backward compatibility.

### Breaking Changes You Have to Know & Possible Incompatibility with Other Plugins
- CraftEngine injects into PalettedContainer to ensure efficient storage and synchronization of plugin block data. This may cause conflicts with some other plugins that modify the palette. When analyzing server performance using Spark, palette operation overhead will be attributed to the CraftEngine plugin in the profiling results.
- CraftEngine injects into FurnaceBlockEntity to modify its recipe fetching logic.
- CraftEngine uses real server-side blocks, any plugin relying on Bukkit's Material class will fail to correctly identify custom block types. The proper approach is to use alternatives like BlockState#getBlock (mojmap) instead of the Material class.
- CraftEngine implements 0-tick collision entities by extending certain Minecraft entities, ensuring hard collision works correctly on the server side (e.g., making a pig stand on a chair). However, some anti-cheat plugins do not check entity AABB (Axis-Aligned Bounding Box) properly when detecting player movement, which may lead to false flags.
- CraftEngine's custom recipe handling may not be fully compatible with other recipe-managing plugins.

## Inspired Projects
This project draws inspiration and refers to some implementations from the following open-source works:
+ [Paper](https://github.com/PaperMC/Paper)
+ [LuckPerms](https://github.com/LuckPerms/LuckPerms)
+ [Fabric](https://github.com/FabricMC/fabric)
+ [packetevents](https://github.com/retrooper/packetevents)
+ [DataFixerUpper](https://github.com/Mojang/DataFixerUpper)
+ [ViaVersion](https://github.com/ViaVersion/ViaVersion)

### Core Dependencies
The implementation relies on these fundamental libraries:
+ [ignite](https://github.com/vectrix-space/ignite)
+ [cloud-minecraft](https://github.com/Incendo/cloud-minecraft)
+ [rtag](https://github.com/saicone/rtag)
+ [adventure](https://github.com/KyoriPowered/adventure)
+ [byte-buddy](https://github.com/raphw/byte-buddy)

## How to Contribute

### 🔌 New Features & Bug Fixes 
If your PR is about a bug fix, it will most likely get merged. If you want to submit a new feature, please make sure to contact me in advance on [Discord](https://discord.com/invite/WVKdaUPR3S).
The code you contribute will be open-sourced under the GPLv3 license. If you prefer a more permissive license(MIT), you can specifically indicate it at the top of the file.

### 🌍 Translations
1. Clone this repository.
2. Create a new language file in: `/bukkit/loader/src/main/resources/translations`
3. Once done, submit a **pull request** for review. We appreciate your contributions!

## Differences Between Versions
| Version           | Official Support | Max Players | Online Mode Required | Commercial Use |
|-------------------|------------------|-------------|----------------------|----------------|
| Community Edition | ❌ No             | 20          | ✔️ Yes               | ✔️ Allowed     |
| GitHub Edition    | ❌ No             | Unlimited   | ❌ No                 | ✔️ Allowed     |
| Premium Edition   | ✔️ Yes           | Unlimited   | ❌ No                 | ✔️ Allowed     |

### 💖 Support the Developer
Help sustain CraftEngine's development by going Premium!

- **Polymart**: [None]
- **BuiltByBit**: [None]
- **Afdian**: [Support via Afdian](https://afdian.com/@xiaomomi/)

## CraftEngine API

```kotlin
repositories {
    maven("https://repo.momirealms.net/releases/")
}
```
```kotlin
dependencies {
    compileOnly("net.momirealms:craft-engine-core:0.0.54")
    compileOnly("net.momirealms:craft-engine-bukkit:0.0.54")
}
```