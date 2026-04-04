# LiteCraft: A Lightweight Content Engine Blueprint

This guide details how to implement the core features of CraftEngine (Custom Blocks, Items, Armor, and Tridents) in a simplified, "Lite" plugin for Minecraft 1.21.11.

## 1. Project Dependencies (Gradle)
```kotlin
dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation("net.bytebuddy:byte-buddy:1.14.12")
}
```

## 2. NMS Block Injection (ByteBuddy)
To support custom shapes (Stairs, Slabs) without replacing vanilla blocks, you must subclass `net.minecraft.world.level.block.Block`.

```java
public class BlockInjector {
    public static Class<?> generateBlockClass(String name) {
        return new ByteBuddy()
            .subclass(net.minecraft.world.level.block.Block.class)
            .name("net.litecraft.generated." + name)
            // Intercept shape for custom collision/rendering
            .method(named("getShape"))
            .intercept(MethodDelegation.to(new Object() {
                @RuntimeType
                public Object intercept(@AllArguments Object[] args) {
                    // Return custom VoxelShape here
                    return Block.box(0, 0, 0, 16, 8, 16); // Example: Half slab
                }
            }))
            .make()
            .load(BlockInjector.class.getClassLoader())
            .getLoaded();
    }
}
```

## 3. Custom Item & Armor Implementation
Use the new Data Component API (1.20.5+).

### 3.1 Custom Armor
Instead of NBT, use the `EQUIPPABLE` component to define custom armor layers.
```java
ItemStack armor = new ItemStack(Material.LEATHER_CHESTPLATE);
ItemMeta meta = armor.getItemMeta();
meta.setCustomModelData(1001);
// In resource pack, use overrides to point CMD 1001 to your custom model
armor.setItemMeta(meta);
```

### 3.2 Custom Trident
Tridents use the `item_model` component in 1.21.4+.
```java
meta.setCustomModelData(2001);
// Resource pack model: assets/minecraft/items/trident.json
// { "model": { "type": "minecraft:select", "property": "minecraft:custom_model_data", ... } }
```

## 4. Packet Remapping (Netty Injection)
To make custom blocks visible to vanilla clients, you must remap their IDs in the Netty pipeline.

```java
public class LitePacketHandler extends ChannelDuplexHandler {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg.getClass().getSimpleName().equals("ClientboundBlockUpdatePacket")) {
            // Use reflection to get 'state' field and remap if it's our custom ID
            // Remap to Material.NOTE_BLOCK or Material.TRIPWIRE
        }
        super.write(ctx, msg, promise);
    }
}
```

## 5. Specialized Implementations

### 5.1 Custom Stairs
Stairs require handling the `StairsShape` enum and `Facing` property.
- **Injection**: Override `updateShape` in your ByteBuddy class to calculate connection logic (Inner/Outer corners) just like vanilla `StairBlock`.

### 5.2 Custom Slabs
- **Injection**: Override `getShape` to return a 0-8 or 8-16 height box depending on the `SlabType` (TOP/BOTTOM) property.

### 5.3 Custom Anvil
- **Interaction**: Listen to `InventoryClickEvent`. If the top inventory is an `AnvilInventory` and your custom item is present, cancel the vanilla result and calculate your own logic (e.g., custom repair costs or attributes).

## 6. Resource Pack Setup
- **Items**: `assets/minecraft/items/[item].json`
- **Blocks**: `assets/minecraft/models/item/[custom_id].json`
- **Textures**: `assets/minecraft/textures/item/custom/`
