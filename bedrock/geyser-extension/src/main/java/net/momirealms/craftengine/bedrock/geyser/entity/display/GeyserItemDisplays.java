package net.momirealms.craftengine.bedrock.geyser.entity.display;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundAddEntityPacket;

import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public final class GeyserItemDisplays extends PacketTranslator<ClientboundAddEntityPacket> {
    private final Map<Identifier, DisplayProperties> carriers = new LinkedHashMap<>();
    private final Map<String, Model> items = new LinkedHashMap<>();
    private PacketTranslator<ClientboundAddEntityPacket> original;
    private VanillaEntityType<GeyserItemDisplayEntity> definition;

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
        Appearance[] appearances = new Appearance[values.size()];
        appearances[0] = Appearance.EMPTY;
        for (int i = 1; i < appearances.length; i++) {
            JsonArray pose = values.get(i).getAsJsonArray();
            appearances[i] = new Appearance(pose.get(0).getAsInt(), vector(pose, 1, Vector3f.ZERO), vector(pose, 2, Vector3f.ZERO), vector(pose, 3, Vector3f.ONE));
        }
        for (Map.Entry<String, JsonElement> entry : displays.getAsJsonObject("items").entrySet()) {
            JsonArray contexts = entry.getValue().getAsJsonArray();
            Appearance[] shapes = new Appearance[contexts.size()];
            for (int i = 0; i < shapes.length; i++) {
                shapes[i] = appearances[contexts.get(i).getAsInt()];
            }
            this.items.putIfAbsent(entry.getKey(), new Model(properties, shapes));
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
        return this.definition == null ? null : this.definition.defaultBedrockDefinition();
    }

    public void properties(GeyserDefineEntityPropertiesEvent event) {
        for (DisplayProperties carrier : this.carriers.values()) {
            carrier.register(event);
        }
    }

    @SuppressWarnings("unchecked")
    public void install() {
        if (this.original != null || this.carriers.isEmpty()) {
            return;
        }
        DisplayProperties first = this.carriers.values().iterator().next();
        this.definition = this.definition(Registries.BEDROCK_ENTITY_DEFINITIONS.get(first.id()), first);
        this.original = (PacketTranslator<ClientboundAddEntityPacket>) Registries.JAVA_PACKET_TRANSLATORS.get(ClientboundAddEntityPacket.class);
        Registries.JAVA_PACKET_TRANSLATORS.register(ClientboundAddEntityPacket.class, this);
    }

    VanillaEntityType<GeyserItemDisplayEntity> definition(BedrockEntityDefinition bedrock, DisplayProperties first) {
        EntityTypeBase.Builder<GeyserItemDisplayEntity> base = EntityTypeBase.baseBuilder(GeyserItemDisplayEntity.class);
        // MCProtocolLib 的公共实体字段占 0..7。第 8 个字段起是 Display，物品栈和上下文在 23、24。
        for (int i = 0; i < 8; i++) {
            base.addTranslator(null);
        }
        return VanillaEntityType.inherited(context -> new GeyserItemDisplayEntity(context, this, first), base.build())
                .type(EntityType.ITEM_DISPLAY).bedrockDefinition(bedrock).heightAndWidth(0)
                .addTranslator(MetadataTypes.INT, (entity, meta) -> entity.startInterpolation((int) meta.getValue()))
                .addTranslator(MetadataTypes.INT, (entity, meta) -> entity.duration((int) meta.getValue()))
                .addTranslator(MetadataTypes.INT, (entity, meta) -> entity.teleportDuration((int) meta.getValue()))
                .addTranslator(MetadataTypes.VECTOR3, (entity, meta) -> entity.translation(meta.getValue()))
                .addTranslator(MetadataTypes.VECTOR3, (entity, meta) -> entity.scale(meta.getValue()))
                .addTranslator(MetadataTypes.QUATERNION, (entity, meta) -> entity.left(meta.getValue()))
                .addTranslator(MetadataTypes.QUATERNION, (entity, meta) -> entity.right(meta.getValue()))
                .addTranslator(MetadataTypes.BYTE, (entity, meta) -> entity.billboard(meta.getValue()))
                .addTranslator(null).addTranslator(null).addTranslator(null).addTranslator(null)
                .addTranslator(null).addTranslator(null).addTranslator(null)
                .addTranslator(MetadataTypes.ITEM_STACK, (entity, meta) -> entity.item(meta.getValue()))
                .addTranslator(MetadataTypes.BYTE, (entity, meta) -> entity.context(meta.getValue()))
                .build(false);
    }

    public void uninstall() {
        if (this.original != null) {
            Registries.JAVA_PACKET_TRANSLATORS.get().replace(ClientboundAddEntityPacket.class, this, this.original);
            this.original = null;
        }
        this.items.clear();
        this.carriers.clear();
    }

    @Override
    public void translate(GeyserSession session, ClientboundAddEntityPacket packet) {
        if (packet.getType() != EntityType.ITEM_DISPLAY) {
            this.original.translate(session, packet);
            return;
        }
        EntitySpawnContext context = EntitySpawnContext.fromPacket(session, this.definition, packet);
        if (!context.callServerSpawnEvent()) {
            return;
        }
        GeyserItemDisplayEntity entity = this.definition.factory().create(context);
        if (context.consumers() != null) {
            context.consumers().forEach(consumer -> consumer.accept(entity));
        }
        session.getEntityCache().spawnEntity(entity);
    }

    Model resolve(GeyserSession session, ItemStack stack) {
        if (stack == null || stack.getAmount() <= 0) {
            return null;
        }
        ItemData translated = ItemTranslator.translateToBedrock(session, stack);
        return this.items.get(translated.getDefinition().getIdentifier());
    }

    record Appearance(int shape, Vector3f translation, Vector3f rotation, Vector3f scale) {
        static final Appearance EMPTY = new Appearance(0, Vector3f.ZERO, Vector3f.ZERO, Vector3f.ONE);
    }

    record Model(DisplayProperties carrier, Appearance[] shapes) {
        Appearance appearance(int context) {
            return context >= 0 && context < this.shapes.length ? this.shapes[context] : Appearance.EMPTY;
        }
    }
}
