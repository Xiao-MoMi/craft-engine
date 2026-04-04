# CraftEngine Independent Implementation Wiki

This wiki explains the high-level engineering required to build a custom content engine for Minecraft from scratch, based on the techniques used by CraftEngine.

## 1. Runtime Bytecode Injection (The Block System)
CraftEngine's secret sauce is using **ByteBuddy** to modify Minecraft's code without using a Mod Loader (Fabric/Forge).

### How it works:
1. **Subclassing NMS**: You don't replace the `Block` class; you create a new class at runtime that extends `net.minecraft.world.level.block.Block`.
2. **Method Interception**: You override `getShape`, `getCollisionShape`, and `tick`.
3. **The Registry Hack**: Minecraft's block registry (`BuiltInRegistries.BLOCK`) is usually frozen. You must use reflection to unfreeze it or use a "Mirror Registry" that intercepts calls to the internal `IdMapper`.
4. **Implementation**:
   - Use `ByteBuddy` to define a class.
   - Use `@RuntimeType` and `@SuperCall` to delegate logic.

## 2. Netty Pipeline Manipulation (The Packet Remapper)
To the client, your custom blocks don't exist. You must "lie" to the client.

### How it works:
1. **Pipeline Injection**: When a player joins, intercept their Netty `Channel`. Add a `ChannelDuplexHandler` before the standard Minecraft decoder/encoder.
2. **Packet Interception**:
   - **`ClientboundBlockUpdatePacket`**: Catch this packet, check the block ID. If it's a custom ID, swap it for a `NOTE_BLOCK` or `TRIPWIRE` ID before it leaves the server.
   - **`ClientboundLevelChunkWithLightPacket`**: This is the hardest. You must iterate through the chunk's `PalettedContainer` and remap IDs within the raw ByteBuf.
3. **Remapping Table**: Maintain a `BiMap<Integer, Integer>` (Custom ID <-> Vanilla ID).

## 3. Dynamic Registry & Holder Pattern
You need a way to refer to custom content before the engine is fully loaded.

### How it works:
1. **ResourceKey**: Use a unique identifier system (e.g., `namespace:value`).
2. **Holders**: A `Holder` is a wrapper. You give the plugin a `Holder<CustomBlock>`. Initially, it's empty (unbound). Later, when the ByteBuddy class is generated, you "bind" the instance to the holder. This allows for "Hot Reloading" without breaking references.

## 4. Item Components & CustomModelData (1.20.5+)
Modern Minecraft uses components instead of NBT.

### How it works:
1. **CustomModelData**: Used for simple 3D model swaps.
2. **ItemModel Component (1.21.4+)**: Allows defining complex model logic in the resource pack.
3. **Processors**: Create a chain of `ItemProcessor` classes. Each processor modifies a specific component (e.g., `LoreProcessor`, `AttributeProcessor`, `EquippableProcessor`).

## 5. Specialized Content Logic
- **Stairs/Slabs**: These aren't just shapes. They have `BlockState` properties. You must implement the logic that checks neighbors and updates the `Facing` or `Shape` (Inner/Outer) property dynamically.
- **Tridents**: To make a custom Trident, you override the `thrown_item` component or intercept the `EntitySpawn` packet to swap the vanilla Trident entity with your custom one.
- **Armor**: Use the `EQUIPPABLE` component and `EQUIPMENT_ASSET` to point to a custom texture layer defined in your resource pack.
