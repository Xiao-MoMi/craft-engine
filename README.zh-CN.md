# CraftEngine

[English](README.md) | **简体中文**

CraftEngine 是一款 Minecraft 服务端插件。你可以用 YAML 定义物品、方块、家具和配方，再把模型与纹理打包成资源包，供玩家在游戏中使用。

[下载社区版](https://modrinth.com/plugin/craftengine) · [使用手册](https://xiao-momi.github.io/craft-engine-wiki/zh-Hans/) · [Ask DeepWiki](https://deepwiki.com/Xiao-MoMi/craft-engine)

## 运行要求

- Paper、Folia 或其兼容分支，Minecraft 版本在 1.20 至 26.3 的支持范围内。
- JDK 21 或更高版本。具体请使用对应 Minecraft 服务端要求的 Java 版本。

## 安装

1. 下载适用于服务端的 CraftEngine JAR 文件，放入 `plugins/` 目录，然后启动服务器。
2. 打开 `plugins/CraftEngine/resources/`，查看内置内容包，或添加自己的内容包。
3. 使用默认工作流时，运行 `/ce reload all` 生成并分发资源包。在游戏内接受资源包后，输入 `/ce` 打开物品浏览器。

## CraftEngine API

要在 Bukkit 插件中调用 CraftEngine，请将以下仓库和 API 依赖添加到 `build.gradle.kts`。

```kotlin
repositories {
    maven("https://repo.momirealms.net/releases/")
}

dependencies {
    compileOnly("net.momirealms:craft-engine-core:26.11")
    compileOnly("net.momirealms:craft-engine-bukkit:26.11")
}
```

如果插件需要通过 CraftEngine 访问 Minecraft 内部代码（NMS），可额外引入以下代理模块。该模块的 API 可能随版本变化。

```kotlin
dependencies {
    compileOnly("net.momirealms:craft-engine-bukkit-proxy:26.11")
}
```

## 构建

在仓库根目录运行 Gradle Wrapper。

| 平台 | 命令 |
| --- | --- |
| macOS 或 Linux | `./gradlew build` |
| Windows（PowerShell） | `.\gradlew.bat build` |

也可以在 IDE 中将仓库作为 Gradle 项目导入，再运行 `build` 任务。生成的插件 JAR 文件位于 `target/` 目录。

## 参与贡献

修复问题时，请向 `dev` 分支提交拉取请求。如果打算添加新功能，请先与维护者讨论。[Discord](https://discord.com/invite/WVKdaUPR3S)

贡献翻译时，请克隆仓库，参照已有格式添加语言文件，再向 `dev` 分支提交拉取请求。[翻译文件目录](common-files/src/main/resources/translations)

贡献的代码采用 GPLv3 许可证。

## 版本对比

| 版本 | 官方支持 | 专属功能 | 开发构建 |
| --- | :---: | :---: | :---: |
| 社区版 | 无 | 无 | 无 |
| 高级版 | 有 | 有 | 有 |

你可以通过购买高级版支持 CraftEngine 的开发。[VoxelShop](https://voxel.shop/product/7624/craftengine) · [BuiltByBit](https://builtbybit.com/resources/craftengine.82674/) · [NMCrate](https://nmcrate.com/products/craftengine-1) · [Afdian](https://afdian.com/@xiaomomi/)
