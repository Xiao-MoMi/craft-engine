<h1 align="center">
  <div style="text-align:center">
    <img src="https://github.com/user-attachments/assets/4e679094-303b-481d-859d-073efc61037c" alt="logo" style="width:100px; height:auto;">
  </div>
  CraftEngine
</h1>

<p align="center">
  <a href="https://momi.gtemc.cn/craftengine" alt="GitBook">
    <img src="https://img.shields.io/badge/%E6%96%87%E6%AA%94-%E7%94%A8%E6%88%B6%E6%89%8B%E5%86%8A-D2691E" alt="Gitbook"/>
  </a>
  <a href="https://github.com/Xiao-MoMi/craft-engine/">
    <img src="https://sloc.xyz/github/Xiao-MoMi/craft-engine/?category=codes" alt="SCC數量標識"/>
  </a>
</p>

<p align="center">
    <a target="_blank" href="/README.md">English</a> |
    <a target="_blank" href="/readme/README_zh-CN.md">简体中文</a> |
    <a target="_blank" href="/readme/README_zh-TW.md">繁體中文</a>
</p>

## 📌 關於 CraftEngine

CraftEngine 重新定義了 Minecraft 外掛程式架構，作為下一代自定義內容實現的解決方案。通過 JVM 級別的注入，它提供了前所未有的性能、穩定性和可擴充性。該框架提供了一個代碼優先的 API，用於註冊原生集成的方塊行為和物品交互邏輯。

## 構建

### 🐚 命令行
1. 安裝 JDK 21。
2. 開啟終端並切換到項目資料夾。
3. 執行 './gradlew build'，構建產物將生成在 '/target' 資料夾中。

### 💻 IDE 開發環境
1. 匯入項目並執行 Gradle 構建作。
2. 構建產物將生成在 '/target' 資料夾中。

## 安装

### 💻 環境要求
1. 確保您正在運行 [Paper](https://papermc.io/)（或其分支）1.20.1+ 伺服器。CraftEngine 不支援 Spigot，且未來也不太可能支援。
2. 使用 JDK 21 來運行伺服器。

### 🔍 安裝方式
CraftEngine 提供了兩種安裝模式：標準安裝和 Mod 模式。標準安裝與傳統外掛程式安裝方式相同，即將外掛程式放入外掛程式資料夾中。下面我們將詳細介紹 Mod 模式的安裝步驟。

### 🔧 安裝伺服器 Mod
1. 下載最新的 [ignite.jar](https://github.com/vectrix-space/ignite/releases)到您的伺服器根目錄。
2. 选择以下任一操作：
    - 將您的伺服器 JAR 檔案重新命名為 `paper.jar`
    - 添加啟動參數：`-Dignite.locator=paper -Dignite.paper.jar=./paper-xxx.jar`
    - 示例：`java -Dignite.locator=paper -Dignite.paper.jar=./paper-1.21.4-164.jar -jar ignite.jar`
3. 啟動伺服器以生成 '/mods' 目錄。
4. 將最新的 [mod.jar](https://github.com/Xiao-MoMi/craft-engine/releases) 放入 '/mods' 資料夾。
5. 將外掛程式的 JAR 檔案放入 '/plugins' 資料夾安裝。
6. 執行兩次重啟：
    1. 第一次重啟用於檔案初始化。
    2. 第二次重啟以啟動所有元件。

## 技術概述

### ⚙️ 方塊
CraftEngine 使用運行時位元組碼生成技術，在伺服器原生級別註冊自定義方塊，並結合客戶端數據包修改以實現視覺同步。此架構提供了以下功能：

🧱 自訂原生方塊
- 動態註冊方塊，完全可控。
- 物理屬性：硬度、引燃幾率、亮度等所有標準屬性。
- 自定義行為：通過 API 實現樹苗、作物、下落的方塊等。
- 原生相容性：完全保留原生方塊機制（例如音符盒、絆線）。

📦 數據包集成
- 定義自定義礦脈。
- 產生自定義樹木。
- 配置自定義地形生成。

⚡ 性能優勢
- 比傳統的 Bukkit 事件監聽器更快、更穩定。
- 策略性代碼注入以最小化開銷。

### 🥘 配方
CraftEngine 通過底層注入實現完全可定製的合成系統。與傳統外掛程式不同，它在處理 NBT 修改時不會失效，確保配方結果僅與唯一的物品標識符綁定。

### 🪑 傢俱
該外掛程式使用核心實體來儲存傢俱元數據，同時將碰撞實體和模組元件作為用戶端數據包傳輸。此架構實現了顯著的伺服器端性能優化，同時支持通過多部分物品集成實現複合傢俱組裝。

### 📝 範本
鑒於外掛程式配置的廣泛性和複雜性，CraftEngine 實現了模組化範本系統以分隔關鍵設置。這使得使用者可以自定義配置格式，同時顯著減少冗餘的 YAML 定義。

### 🛠️ 模型
該外掛程式通過配置實現模型繼承和紋理覆蓋，同時支援從 1.21.4 版本開始的[所有物品模型](https://misode.github.io/assets/item/)。它包含一個版本遷移系統，可以自動將 1.21.4+ 的物品模型降級為舊格式，以實現最大向後相容性。

### ❗️ 您必須了解和部分插件的不相容性
- CraftEngine 會注入到 PalettedContainer 中，以確保插件的區塊資料會被高效的儲存與同步。這有可能會導致與一些修改palette的其他插件發生衝突。當您使用 Spark 來分析伺服器效能時，palette的操作所導致的效能開銷，將會在Spark的分析結果中歸因於 CraftEngine 外掛程式。
- CraftEngine 會注入到 FurnaceBlockEntity 中，來修改它的配方獲取邏輯.
- CraftEngine 會使用真實的server-side blocks, 任何依賴Bukkit的Material類別的插件，都會無法正確識別自訂方塊類型。 正確的方法是使用像 BlockState#getBlock(mojmap) 這樣的替代方式，而不是使用 Material 類別。
- CraftEngine 透過擴展某些 Minecraft 實體來實現 0-tick 碰撞實體, 確保碰撞在伺服器端可以正常工作（例如，讓豬站在椅子上）. 然而，一些反作弊插件在檢測玩家移動時沒有正確檢查實體 AABB（Axis-Aligned Bounding Box），這可能會導致誤報。
- CraftEngine 的自訂配方處理可能會與其他配方插件 不完全相容。

## 靈感來源
CraftEngine 從以下開源專案中汲取了靈感：
- [Paper](https://github.com/PaperMC/Paper)
- [LuckPerms](https://github.com/LuckPerms/LuckPerms)
- [Fabric](https://github.com/FabricMC/fabric)
- [packetevents](https://github.com/retrooper/packetevents)
- [NBT](https://github.com/Querz/NBT)
- [DataFixerUpper](https://github.com/Mojang/DataFixerUpper)
- [ViaVersion](https://github.com/ViaVersion/ViaVersion)

### 核心依賴
CraftEngine 的實現依賴於以下基礎庫：
- [ignite](https://github.com/vectrix-space/ignite)
- [cloud-minecraft](https://github.com/Incendo/cloud-minecraft)
- [rtag](https://github.com/saicone/rtag)
- [adventure](https://github.com/KyoriPowered/adventure)
- [byte-buddy](https://github.com/raphw/byte-buddy)

## 如何貢獻

### 🔌 新功能與 Bug 修復
如果您提交的 PR 是關於 Bug 修復的，它很可能會被合併。如果您想提交新功能，請提前在 [Discord](https://discord.com/invite/WVKdaUPR3S) 上聯繫我。

### 🌍 翻譯
1. 克隆此倉庫。
2. 在 '/bukkit/loader/src/main/resources/translations' 中創建一個新的語言檔。
3. 完成後，提交 **pull request** 以供審核。我們感謝您的貢獻！

## 版本之間的差異
| 版本 | 官方支援 | 最大玩家數 | 線上模式需求 | 商用 |
|-------------------|--------------------|-------------|----------------------|----------------|
| Community Edition(社群版) | ❌ 不支援 | 20 | ✔️ 需要 | ✔️ 允許 |
| GitHub Edition(GitHub版) | ❌ 不支援 | 無限 | ❌ 無需 | ✔️ 允許 |
| Premium Edition(進階版) | ✔️ 支援 | 無限 | ❌ 無需 | ✔️ 允許 |

### 💖 支持開發者
如果您喜欢使用 CraftEngine，请考虑支持开发者！

- **Polymart**: [无]
- **BuiltByBit**: [无]
- **愛發電**: [通過愛發電支援](https://afdian.com/@xiaomomi/)

## CraftEngine API

```kotlin
repositories {
    maven("https://repo.momirealms.net/releases/")
    // 如果你的網路環境受限可以嘗試下面的存儲庫位址
    // maven("https://repo-momi.gtemc.cn/releases/")
}
```
```kotlin
dependencies {
    compileOnly("net.momirealms:craft-engine-core:0.0.54")
    compileOnly("net.momirealms:craft-engine-bukkit:0.0.54")
}
```
