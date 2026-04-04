# CraftEngine Deep Analysis Report

## 1. Architecture Overview
CraftEngine is a high-performance content engine for Minecraft servers (Bukkit/Paper). It uses a modular Gradle structure:
- `core`: API and platform-agnostic logic.
- `bukkit`: Implementation for Spigot/Paper, handling NMS and packet interception.
- `common-files`: Resources and configurations.

## 2. Core Mechanics
### 2.1 Registry System (`BuiltInRegistries`)
The engine uses a `Holder` pattern for late-binding of custom blocks and items. This allows the server to register placeholders during startup and bind the actual logic/data once the engine is fully initialized or reloaded.

### 2.2 NMS Injection (ByteBuddy)
This is the most critical part. CraftEngine uses **ByteBuddy** to dynamically subclass Minecraft's internal `Block` and `BlockState` classes.
- **`BlockGenerator`**: Creates a `CraftEngineBlock` class that extends `net.minecraft.world.level.block.Block`. It intercepts methods like `getShape`, `getCollisionShape`, and `tick`.
- **`BlockStateGenerator`**: Creates a corresponding `BlockState` subclass.
This approach allows custom blocks to have genuine collision and ticking behavior without replacing vanilla blocks.

### 2.3 Networking and Remapping
`BukkitNetworkManager` injects custom Netty handlers:
- **`PluginChannelDecoder` / `PluginChannelEncoder`**: These handle raw `ByteBuf` manipulation.
- **ID Remapping**: The engine maintains a mapping of custom block state IDs to vanilla IDs. When a packet (like `LevelChunkWithLight` or `BlockUpdate`) is sent to a client, the IDs are swapped on-the-fly so the vanilla client sees a "real" block (like a Note Block or Tripwire) that corresponds to the custom model.

### 2.4 Resource Pack Generation
The `PackManager` automatically generates a resource pack by:
- Merging models and textures.
- Assigning `CustomModelData` to items.
- Handling file conflicts via custom resolution strategies.
- [Premium] Applying obfuscation and optimization (Zopfli).

## 3. Specialized Content Implementation
- **Stairs/Slabs**: Supported by overriding the shape and collision logic in the injected Block classes.
- **Tridents/Armor**: Uses `ItemProcessor` to modify NBT and components (1.20.5+), allowing for custom rendering via resource packs.
- **Anvils/Interactables**: Handled through `BlockBehavior` and `ItemBehavior` listeners that intercept interactions and cancel vanilla behavior if necessary.

## 4. Conclusion
CraftEngine is a sophisticated framework that pushes the limits of Bukkit/Paper by combining bytecode manipulation with low-level packet handling. Its complexity stems from supporting a wide range of Minecraft versions and providing features like automated resource pack hosting and emoji support.
