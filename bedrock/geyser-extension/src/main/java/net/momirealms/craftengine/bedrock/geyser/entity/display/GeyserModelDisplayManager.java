package net.momirealms.craftengine.bedrock.geyser.entity.display;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.momirealms.craftengine.bedrock.BlockStateKey;
import net.momirealms.craftengine.bedrock.geyser.locale.GeyserTranslationManager;
import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.geyser.api.entity.custom.CustomEntityDefinition;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.entity.BedrockEntityDefinition;
import org.geysermc.geyser.entity.EntityTypeBase;
import org.geysermc.geyser.entity.VanillaEntityType;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public final class GeyserModelDisplayManager {
    static final int DEFAULT_DISPLAY_CONTEXT = 0;

    private final Map<Identifier, ModelDisplayProperties> carriers = new LinkedHashMap<>();
    private final Map<String, Model> items = new LinkedHashMap<>();
    private final Map<String, Model> vanillaItems = new LinkedHashMap<>();
    private final Map<BlockStateKey, Model> blocks = new LinkedHashMap<>();
    private Int2ObjectMap<GeyserHeldItemTypes> heldItemTypes = Int2ObjectMaps.emptyMap();
    private Model heldBlock;
    private VanillaEntityType<GeyserModelDisplayEntity> itemDefinition;
    private VanillaEntityType<GeyserModelDisplayEntity> blockDefinition;

    public void registerEntities(@Nullable JsonObject displays, GeyserDefineEntitiesEvent event) {
        if (displays == null) {
            return;
        }
        try {
            this.define(displays, event);
        } catch (RuntimeException exception) {
            GeyserTranslationManager.error("display.registration_failed", exception);
        }
    }

    public void define(JsonObject displays, GeyserDefineEntitiesEvent event) {
        ModelDisplayProperties modelCarrier = this.registerCarrier(event, displays.get("identifier").getAsString());
        Appearance[] appearances = readAppearances(displays);

        if (displays.has("vanilla_identifier")) {
            this.registerVanillaModels(displays, event, modelCarrier, appearances);
        }
        JsonObject items = displays.getAsJsonObject("items");
        if (items != null) {
            for (Map.Entry<String, JsonElement> entry : items.entrySet()) {
                Appearance[] contexts = readContextAppearances(entry.getValue().getAsJsonArray(), appearances);
                this.items.putIfAbsent(entry.getKey(), new Model(modelCarrier, contexts));
            }
        }
        if (displays.has("blocks")) {
            for (Map.Entry<String, JsonElement> entry : displays.getAsJsonObject("blocks").entrySet()) {
                BlockStateKey state = BlockStateKey.parse(entry.getKey());
                Appearance appearance = appearances[entry.getValue().getAsInt()];
                this.blocks.putIfAbsent(state, new Model(modelCarrier, new Appearance[]{appearance}));
            }
        }
    }

    private ModelDisplayProperties registerCarrier(GeyserDefineEntitiesEvent event, String identifier) {
        Identifier id = Identifier.of(identifier);
        event.register(CustomEntityDefinition.of(id));
        ModelDisplayProperties carrier = new ModelDisplayProperties(id);
        this.carriers.put(id, carrier);
        return carrier;
    }

    private void registerVanillaModels(JsonObject displays,
                                       GeyserDefineEntitiesEvent event,
                                       ModelDisplayProperties modelCarrier,
                                       Appearance[] appearances) {
        ModelDisplayProperties heldCarrier = this.registerCarrier(event, displays.get("vanilla_identifier").getAsString());
        if (this.heldBlock == null) {
            Appearance blockAppearance = new Appearance(0, Vector3f.from(8, 8, -8), Vector3f.from(0, 180, 0), Vector3f.ONE);
            this.heldBlock = new Model(heldCarrier, new Appearance[]{blockAppearance});
        }
        JsonObject vanilla = displays.getAsJsonObject("vanilla");
        if (vanilla == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : vanilla.entrySet()) {
            Appearance[] contexts = readContextAppearances(entry.getValue().getAsJsonArray(), appearances);
            ModelDisplayProperties carrier = contexts[DEFAULT_DISPLAY_CONTEXT].shape() == 0 ? heldCarrier : modelCarrier;
            this.vanillaItems.put(entry.getKey(), new Model(carrier, contexts));
        }
    }

    private static Appearance[] readAppearances(JsonObject displays) {
        JsonArray values = displays.getAsJsonArray("appearances");
        JsonObject colors = displays.getAsJsonObject("custom_model_data_colors");
        Appearance[] appearances = new Appearance[values.size()];
        appearances[0] = Appearance.EMPTY;
        for (int i = 1; i < appearances.length; i++) {
            JsonArray pose = values.get(i).getAsJsonArray();
            int shape = pose.get(0).getAsInt();
            JsonElement color = colors == null ? null : colors.get(Integer.toString(shape));
            appearances[i] = new Appearance(
                    shape,
                    readVector(pose, 1, Vector3f.ZERO),
                    readVector(pose, 2, Vector3f.ZERO),
                    readVector(pose, 3, Vector3f.ONE),
                    color == null ? -1 : color.getAsInt()
            );
        }
        return appearances;
    }

    private static Appearance[] readContextAppearances(JsonArray indices, Appearance[] appearances) {
        Appearance[] contexts = new Appearance[indices.size()];
        for (int i = 0; i < contexts.length; i++) {
            contexts[i] = appearances[indices.get(i).getAsInt()];
        }
        return contexts;
    }

    private static Vector3f readVector(JsonArray pose, int index, Vector3f fallback) {
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
        for (ModelDisplayProperties carrier : this.carriers.values()) {
            carrier.register(event);
        }
    }

    public void initialize() {
        if (this.itemDefinition != null || this.carriers.isEmpty()) {
            return;
        }
        if (this.heldBlock != null) {
            this.heldItemTypes = GeyserHeldItemTypes.load();
        }
        ModelDisplayProperties initialCarrier = this.carriers.values().iterator().next();
        BedrockEntityDefinition bedrock = Registries.BEDROCK_ENTITY_DEFINITIONS.get(initialCarrier.id());
        this.itemDefinition = this.displayEntityBuilder(bedrock, initialCarrier, EntityType.ITEM_DISPLAY)
                .addTranslator(MetadataTypes.ITEM_STACK, (entity, meta) -> entity.item(meta.getValue()))
                .addTranslator(MetadataTypes.BYTE, (entity, meta) -> entity.context(meta.getValue()))
                .build(false);
        this.blockDefinition = this.displayEntityBuilder(bedrock, initialCarrier, EntityType.BLOCK_DISPLAY)
                .addTranslator(MetadataTypes.BLOCK_STATE, (entity, meta) -> entity.block((int) meta.getValue()))
                .build(false);
    }

    @Nullable
    VanillaEntityType<GeyserModelDisplayEntity> itemDefinition() {
        return this.itemDefinition;
    }

    @Nullable
    VanillaEntityType<GeyserModelDisplayEntity> blockDefinition() {
        return this.blockDefinition;
    }

    private VanillaEntityType.Builder<GeyserModelDisplayEntity> displayEntityBuilder(BedrockEntityDefinition bedrock,
                                                                                     ModelDisplayProperties initialCarrier,
                                                                                     EntityType type) {
        EntityTypeBase.Builder<GeyserModelDisplayEntity> base = EntityTypeBase.baseBuilder(GeyserModelDisplayEntity.class);
        // MCProtocolLib 的公共实体字段占 0..7。第 8 个字段起是 Display，物品栈和上下文在 23、24。
        for (int i = 0; i < 8; i++) {
            base.addTranslator(null);
        }
        return VanillaEntityType.inherited(context -> new GeyserModelDisplayEntity(context, this, initialCarrier), base.build())
                .type(type).bedrockDefinition(bedrock).heightAndWidth(0)
                .addTranslator(MetadataTypes.INT, (entity, meta) -> entity.startInterpolation((int) meta.getValue()))
                .addTranslator(MetadataTypes.INT, (entity, meta) -> entity.duration((int) meta.getValue()))
                .addTranslator(MetadataTypes.INT, (entity, meta) -> entity.teleportDuration((int) meta.getValue()))
                .addTranslator(MetadataTypes.VECTOR3, (entity, meta) -> entity.translation(meta.getValue()))
                .addTranslator(MetadataTypes.VECTOR3, (entity, meta) -> entity.scale(meta.getValue()))
                .addTranslator(MetadataTypes.QUATERNION, (entity, meta) -> entity.left(meta.getValue()))
                .addTranslator(MetadataTypes.QUATERNION, (entity, meta) -> entity.right(meta.getValue()))
                .addTranslator(MetadataTypes.BYTE, (entity, meta) -> entity.billboard(meta.getValue()))
                .addTranslator(null)
                .addTranslator(MetadataTypes.FLOAT, (entity, meta) -> entity.viewRange(meta.getValue()))
                .addTranslator(null)
                .addTranslator(null)
                .addTranslator(null)
                .addTranslator(null)
                .addTranslator(null);
    }

    public void disable() {
        this.itemDefinition = null;
        this.blockDefinition = null;
        this.items.clear();
        this.vanillaItems.clear();
        this.blocks.clear();
        this.heldItemTypes = Int2ObjectMaps.emptyMap();
        this.heldBlock = null;
        this.carriers.clear();
    }

    Model resolve(String identifier) {
        return this.items.get(identifier);
    }

    Model block(String state) {
        return this.blocks.get(BlockStateKey.parse(state));
    }

    Model heldBlock() {
        return this.heldBlock;
    }

    GeyserHeldItemTypes heldItemTypes(int protocol) {
        return this.heldItemTypes.getOrDefault(protocol, GeyserHeldItemTypes.EMPTY);
    }

    Model vanilla(ItemMapping mapping) {
        return this.vanillaItems.get(mapping.getJavaItem().javaIdentifier().toString());
    }

    record Appearance(int shape, Vector3f translation, Vector3f rotation, Vector3f scale, int customModelDataColorIndex) {
        static final Appearance EMPTY = new Appearance(0, Vector3f.ZERO, Vector3f.ZERO, Vector3f.ONE);


        Appearance(int shape, Vector3f translation, Vector3f rotation, Vector3f scale) {
            this(shape, translation, rotation, scale, -1);
        }
    }

    record Model(ModelDisplayProperties carrier, Appearance[] appearances) {
        Appearance appearance(int context) {
            return context >= 0 && context < this.appearances.length ? this.appearances[context] : Appearance.EMPTY;
        }
    }
}
