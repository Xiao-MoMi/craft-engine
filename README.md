<h1 align="center">
  <div style="text-align:center">
    <img src="https://github.com/user-attachments/assets/4e679094-303b-481d-859d-073efc61037c" alt="logo" style="width:100px; height:auto;">
  </div>
  CraftEngine
</h1>

<p align="center">
  <a href="https://deepwiki.com/Xiao-MoMi/craft-engine">
    <img src="https://deepwiki.com/badge.svg" alt="Ask DeepWiki">
  </a>
  <a href="https://mo-mi.gitbook.io/xiaomomi-plugins/craftengine" alt="GitBook">
    <img src="https://img.shields.io/badge/📙-User Manual-D2691E" alt="Gitbook"/>
  </a>
</p>

## 📌 About CraftEngine
CraftEngine works as a next-generation solution for custom content implementation.

## Build

### 🐚 Command Line
+ Start terminal and change directory to the project folder.
+ Execute "./gradlew build" and get the artifact under /target folder.

### 💻 IDE
+ Import the project and execute gradle build action.
+ Get the artifact under /target folder.

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
+ [cloud-minecraft](https://github.com/Incendo/cloud-minecraft)
+ [adventure](https://github.com/KyoriPowered/adventure)
+ [byte-buddy](https://github.com/raphw/byte-buddy)

## How to Contribute

### 🔌 New Features & Bug Fixes 
If your PR is about a bug fix, it will most likely get merged. If you want to submit a new feature, please make sure to contact me in advance on [Discord](https://discord.com/invite/WVKdaUPR3S).
The code you contribute will be open-sourced under the GPLv3 license. If you prefer a more permissive license(MIT), you can specifically indicate it at the top of the file.

### 🌍 Translations
1. Clone this repository.
2. Create a new language file in: `/common-files/src/main/resources/translations`
3. Once done, submit a **pull request** to **dev** branch for review. We appreciate your contributions!

## Differences Between Versions
| Version           | Official Support | Max Players | Dev Builds |
|-------------------|------------------|-------------|------------|
| Community Edition | ❌ No             | 20          | ❌ No       |
| Premium Edition   | ✔️ Yes           | Unlimited   | ✔️ Yes     |

### 💖 Support the Developer
Help sustain CraftEngine's development by going Premium!

- **Polymart**: [Support via Polymart](https://polymart.org/product/7624/craftengine)
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
    compileOnly("net.momirealms:craft-engine-core:0.0.59")
    compileOnly("net.momirealms:craft-engine-bukkit:0.0.59")
}
```