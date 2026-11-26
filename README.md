# CraftEngine

**English** | [简体中文](README.zh-CN.md)

CraftEngine is a Minecraft server plugin for creating custom items, blocks, furniture, and recipes. Define your content in YAML and bundle its models and textures into a resource pack for players.

[Download Community Edition](https://modrinth.com/plugin/craftengine) · [User manual](https://xiao-momi.github.io/craft-engine-wiki/) · [Ask DeepWiki](https://deepwiki.com/Xiao-MoMi/craft-engine)

## Features

- **Items and recipes** — Give items custom models and behaviors, and define recipes that use or produce them.
- **Blocks and furniture** — Configure how blocks and furniture look, where players can place them, and how players interact with them.
- **Resource packs** — Merge assets from multiple content packs into a resource pack and distribute it to players.
- **Bedrock Edition** — Use the CraftEngine Geyser extension to display custom items, blocks, furniture, and fonts for Bedrock players.

## Requirements

- Paper, Folia, or a compatible fork running a supported Minecraft version from 1.20 through 26.3.
- JDK 21 or later. Use the Java version required by your Minecraft server.

## Installation

1. Download the CraftEngine JAR for your server, place it in `plugins/`, and start the server.
2. Open `plugins/CraftEngine/resources/` to explore the bundled packs or add your own.
3. With the default workflow, run `/ce reload all` to build and distribute the resource pack. Accept the pack in-game, then run `/ce` to open the item browser.

## CraftEngine API

To use CraftEngine from a Bukkit plugin, add the repository and API dependencies to `build.gradle.kts`.

```kotlin
repositories {
    maven("https://repo.momirealms.net/releases/")
}

dependencies {
    compileOnly("net.momirealms:craft-engine-core:26.11")
    compileOnly("net.momirealms:craft-engine-bukkit:26.11")
}
```

If your plugin accesses Minecraft internals (NMS) through CraftEngine, add the optional proxy dependency. Its API can change between versions.

```kotlin
dependencies {
    compileOnly("net.momirealms:craft-engine-bukkit-proxy:26.11")
}
```

## Build

Run the Gradle wrapper from the repository root.

| Platform | Command |
| --- | --- |
| macOS or Linux | `./gradlew build` |
| Windows (PowerShell) | `.\gradlew.bat build` |

You can also import the repository as a Gradle project in your IDE and run the `build` task. The plugin JARs are written to `target/`.

## Contributing

Submit bug fixes to the `dev` branch. For new features, discuss the idea with the maintainer before opening a pull request. [Discord](https://discord.com/invite/WVKdaUPR3S)

To contribute a translation, clone the repository, add a language file following the existing format, and open a pull request against `dev`. [Translation files](common-files/src/main/resources/translations)

Contributed code is licensed under GPLv3.

## Editions

| Edition | Official support | Exclusive features | Development builds |
| --- | :---: | :---: | :---: |
| Community | No | No | No |
| Premium | Yes | Yes | Yes |

Support CraftEngine development by purchasing Premium. [VoxelShop](https://voxel.shop/product/7624/craftengine) · [BuiltByBit](https://builtbybit.com/resources/craftengine.82674/) · [NMCrate](https://nmcrate.com/products/craftengine-1) · [Afdian](https://afdian.com/@xiaomomi/)
