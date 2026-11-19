package net.momirealms.craftengine.bedrock.geyser.entity.display;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BlockStateKey;
import net.momirealms.craftengine.bedrock.geyser.locale.GeyserTranslationManager;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.api.entity.custom.CustomEntityDefinition;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.entity.BedrockEntityDefinition;
import org.geysermc.geyser.entity.EntityTypeBase;
import org.geysermc.geyser.entity.VanillaEntityType;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public final class GeyserItemDisplays {
    private final Map<Identifier, DisplayProperties> carriers = new LinkedHashMap<>();
    private final Map<String, Model> items = new LinkedHashMap<>();
    private final Map<String, Model> vanillaItems = new LinkedHashMap<>();
    private final Map<BlockStateKey, Model> blocks = new LinkedHashMap<>();
    private Model heldBlock;
    private VanillaEntityType<GeyserDisplayEntity> itemDefinition;
    private VanillaEntityType<GeyserDisplayEntity> blockDefinition;

    public void registerEntities(JsonObject plan, GeyserDefineEntitiesEvent event) {
        if (plan == null) {
            return;
        }
        try {
            this.define(plan, event);
        } catch (RuntimeException exception) {
            GeyserTranslationManager.error("display.registration_failed", exception);
        }
    }

    public void define(JsonObject plan, GeyserDefineEntitiesEvent event) {
        if (!plan.has("item_displays")) {
            return;
        }
        JsonObject displays = plan.getAsJsonObject("item_displays");
        Identifier id = Identifier.of(displays.get("identifier").getAsString());
        event.register(CustomEntityDefinition.of(id));
        DisplayProperties properties = new DisplayProperties(id);
        this.carriers.put(id, properties);
        JsonArray values = displays.getAsJsonArray("appearances");
        JsonObject colors = displays.getAsJsonObject("custom_model_data_colors");
        Appearance[] appearances = new Appearance[values.size()];
        appearances[0] = Appearance.EMPTY;
        for (int i = 1; i < appearances.length; i++) {
            JsonArray pose = values.get(i).getAsJsonArray();
            int shape = pose.get(0).getAsInt();
            JsonElement color = colors == null ? null : colors.get(Integer.toString(shape));
            appearances[i] = new Appearance(shape, vector(pose, 1, Vector3f.ZERO), vector(pose, 2, Vector3f.ZERO), vector(pose, 3, Vector3f.ONE), color == null ? -1 : color.getAsInt());
        }
        if (displays.has("vanilla_identifier")) {
            Identifier heldId = Identifier.of(displays.get("vanilla_identifier").getAsString());
            event.register(CustomEntityDefinition.of(heldId));
            DisplayProperties heldProperties = new DisplayProperties(heldId);
            this.carriers.put(heldId, heldProperties);
            if (this.heldBlock == null) {
                this.heldBlock = new Model(heldProperties, new Appearance[]{new Appearance(0,
                        Vector3f.from(8, 8, -8), Vector3f.from(0, 180, 0), Vector3f.ONE)});
            }
            for (Map.Entry<String, JsonElement> entry : displays.getAsJsonObject("vanilla").entrySet()) {
                JsonArray contexts = entry.getValue().getAsJsonArray();
                Appearance[] poses = new Appearance[contexts.size()];
                for (int i = 0; i < poses.length; i++) {
                    poses[i] = appearances[contexts.get(i).getAsInt()];
                }
                this.vanillaItems.put(entry.getKey(), new Model(poses[0].shape() == 0 ? heldProperties : properties, poses));
            }
        }
        for (Map.Entry<String, JsonElement> entry : displays.getAsJsonObject("items").entrySet()) {
            JsonArray contexts = entry.getValue().getAsJsonArray();
            Appearance[] shapes = new Appearance[contexts.size()];
            for (int i = 0; i < shapes.length; i++) {
                shapes[i] = appearances[contexts.get(i).getAsInt()];
            }
            this.items.putIfAbsent(entry.getKey(), new Model(properties, shapes));
        }
        if (displays.has("blocks")) {
            for (Map.Entry<String, JsonElement> entry : displays.getAsJsonObject("blocks").entrySet()) {
                this.blocks.putIfAbsent(BlockStateKey.parse(entry.getKey()),
                        new Model(properties, new Appearance[]{appearances[entry.getValue().getAsInt()]}));
            }
        }
    }

    private static Vector3f vector(JsonArray pose, int index, Vector3f fallback) {
        if (pose.size() <= index) {
            return fallback;
        }
        JsonArray xyz = pose.get(index).getAsJsonArray();
        return Vector3f.from(xyz.get(0).getAsFloat(), xyz.get(1).getAsFloat(), xyz.get(2).getAsFloat());
    }

    public BedrockEntityDefinition emptyCarrier() {
        return this.itemDefinition == null ? null : this.itemDefinition.defaultBedrockDefinition();
    }

    public void properties(GeyserDefineEntityPropertiesEvent event) {
        for (DisplayProperties carrier : this.carriers.values()) {
            carrier.register(event);
        }
    }

    public void initialize() {
        if (this.itemDefinition != null || this.carriers.isEmpty()) {
            return;
        }
        DisplayProperties first = this.carriers.values().iterator().next();
        BedrockEntityDefinition bedrock = Registries.BEDROCK_ENTITY_DEFINITIONS.get(first.id());
        this.itemDefinition = this.displayBase(bedrock, first, EntityType.ITEM_DISPLAY)
                .addTranslator(MetadataTypes.ITEM_STACK, (entity, meta) -> entity.item(meta.getValue()))
                .addTranslator(MetadataTypes.BYTE, (entity, meta) -> entity.context(meta.getValue()))
                .build(false);
        this.blockDefinition = this.displayBase(bedrock, first, EntityType.BLOCK_DISPLAY)
                .addTranslator(MetadataTypes.BLOCK_STATE, (entity, meta) -> entity.block((int) meta.getValue())).build(false);
    }

    @Nullable
    VanillaEntityType<GeyserDisplayEntity> itemDefinition() {
        return this.itemDefinition;
    }

    @Nullable
    VanillaEntityType<GeyserDisplayEntity> blockDefinition() {
        return this.blockDefinition;
    }

    private VanillaEntityType.Builder<GeyserDisplayEntity> displayBase(BedrockEntityDefinition bedrock, DisplayProperties first, EntityType type) {
        EntityTypeBase.Builder<GeyserDisplayEntity> base = EntityTypeBase.baseBuilder(GeyserDisplayEntity.class);
        // MCProtocolLib 的公共实体字段占 0..7。第 8 个字段起是 Display，物品栈和上下文在 23、24。
        for (int i = 0; i < 8; i++) {
            base.addTranslator(null);
        }
        return VanillaEntityType.inherited(context -> new GeyserDisplayEntity(context, this, first), base.build())
                .type(type).bedrockDefinition(bedrock).heightAndWidth(0)
                .addTranslator(MetadataTypes.INT, (entity, meta) -> entity.startInterpolation((int) meta.getValue()))
                .addTranslator(MetadataTypes.INT, (entity, meta) -> entity.duration((int) meta.getValue()))
                .addTranslator(MetadataTypes.INT, (entity, meta) -> entity.teleportDuration((int) meta.getValue()))
                .addTranslator(MetadataTypes.VECTOR3, (entity, meta) -> entity.translation(meta.getValue()))
                .addTranslator(MetadataTypes.VECTOR3, (entity, meta) -> entity.scale(meta.getValue()))
                .addTranslator(MetadataTypes.QUATERNION, (entity, meta) -> entity.left(meta.getValue()))
                .addTranslator(MetadataTypes.QUATERNION, (entity, meta) -> entity.right(meta.getValue()))
                .addTranslator(MetadataTypes.BYTE, (entity, meta) -> entity.billboard(meta.getValue()))
                .addTranslator(null).addTranslator(null).addTranslator(null).addTranslator(null)
                .addTranslator(null).addTranslator(null).addTranslator(null);
    }

    public void disable() {
        this.itemDefinition = null;
        this.blockDefinition = null;
        this.items.clear();
        this.vanillaItems.clear();
        this.blocks.clear();
        this.heldBlock = null;
        this.carriers.clear();
    }

    Model resolve(ItemData item) {
        return this.items.get(item.getDefinition().getIdentifier());
    }

    Model block(String state) {
        return this.blocks.get(BlockStateKey.parse(state));
    }

    Model heldBlock() {
        return this.heldBlock;
    }

    Model vanilla(GeyserSession session, ItemStack stack) {
        return this.vanillaItems.get(session.getItemMappings().getMapping(stack).getJavaItem().javaIdentifier().toString());
    }

    record Appearance(int shape, Vector3f translation, Vector3f rotation, Vector3f scale, int customModelDataColorIndex) {
        static final Appearance EMPTY = new Appearance(0, Vector3f.ZERO, Vector3f.ZERO, Vector3f.ONE);

        Appearance(int shape, Vector3f translation, Vector3f rotation, Vector3f scale) {
            this(shape, translation, rotation, scale, -1);
        }
    }

    record Model(DisplayProperties carrier, Appearance[] shapes) {
        Appearance appearance(int context) {
            return context >= 0 && context < this.shapes.length ? this.shapes[context] : Appearance.EMPTY;
        }
    }
}
