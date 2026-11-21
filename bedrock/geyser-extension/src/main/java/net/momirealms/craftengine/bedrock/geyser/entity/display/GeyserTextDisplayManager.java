package net.momirealms.craftengine.bedrock.geyser.entity.display;

import net.momirealms.craftengine.bedrock.geyser.glyph.GeyserGlyphData;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayerManager;
import org.geysermc.geyser.entity.BedrockEntityDefinitions;
import org.geysermc.geyser.entity.EntityTypeBase;
import org.geysermc.geyser.entity.VanillaEntityType;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

public final class GeyserTextDisplayManager {
    private static final int MIN_PROTOCOL = 975;
    public static volatile boolean conversionEnabled = true;

    private final GeyserPlayerManager players;
    private final GeyserGlyphData glyphs;
    private final VanillaEntityType<GeyserTextDisplayEntity> definition;

    public GeyserTextDisplayManager(GeyserPlayerManager players, GeyserGlyphData glyphs) {
        this.players = players;
        this.glyphs = glyphs;
        EntityTypeBase.Builder<GeyserTextDisplayEntity> base = EntityTypeBase.baseBuilder(GeyserTextDisplayEntity.class);
        base.addTranslator(MetadataTypes.BYTE, GeyserTextDisplayEntity::setFlags)
                .addTranslator(MetadataTypes.INT, GeyserTextDisplayEntity::setAir)
                .addTranslator(MetadataTypes.OPTIONAL_COMPONENT, GeyserTextDisplayEntity::setCustomName)
                .addTranslator(MetadataTypes.BOOLEAN, GeyserTextDisplayEntity::setCustomNameVisible)
                .addTranslator(MetadataTypes.BOOLEAN, GeyserTextDisplayEntity::setSilent)
                .addTranslator(MetadataTypes.BOOLEAN, GeyserTextDisplayEntity::setGravity)
                .addTranslator(null)
                .addTranslator(null);
        this.definition = VanillaEntityType.inherited(context -> this.create(context, this.players.get(context.session())), base.build())
                .type(EntityType.TEXT_DISPLAY).bedrockDefinition(BedrockEntityDefinitions.ARMOR_STAND)
                .addTranslator(MetadataTypes.INT, (entity, meta) -> entity.transform().startInterpolation(meta.getValue()))
                .addTranslator(MetadataTypes.INT, (entity, meta) -> entity.transform().duration(meta.getValue()))
                .addTranslator(MetadataTypes.INT, (entity, meta) -> entity.teleportDuration(meta.getValue()))
                .addTranslator(MetadataTypes.VECTOR3, GeyserTextDisplayEntity::setTranslation)
                .addTranslator(MetadataTypes.VECTOR3, (entity, meta) -> entity.transform().scale(meta.getValue()))
                .addTranslator(MetadataTypes.QUATERNION, (entity, meta) -> entity.transform().left(meta.getValue()))
                .addTranslator(MetadataTypes.QUATERNION, (entity, meta) -> entity.transform().right(meta.getValue()))
                .addTranslator(MetadataTypes.BYTE, (entity, meta) -> entity.billboard(meta.getValue()))
                .addTranslator(MetadataTypes.INT, (entity, meta) -> entity.brightness(meta.getValue()))
                .addTranslator(MetadataTypes.FLOAT, (entity, meta) -> entity.viewRange(meta.getValue()))
                .addTranslator(null)
                .addTranslator(null)
                .addTranslator(null)
                .addTranslator(null)
                .addTranslator(null)
                .addTranslator(MetadataTypes.COMPONENT, GeyserTextDisplayEntity::setText)
                .addTranslator(MetadataTypes.INT, (entity, meta) -> entity.lineWidth(meta.getValue()))
                .addTranslator(MetadataTypes.INT, (entity, meta) -> entity.background(meta.getValue()))
                .addTranslator(MetadataTypes.BYTE, (entity, meta) -> entity.opacity(meta.getValue()))
                .addTranslator(MetadataTypes.BYTE, (entity, meta) -> entity.style(meta.getValue()))
                .build(false);
    }

    public boolean supports(GeyserSession session) {
        return conversionEnabled && session.getUpstream().getProtocolVersion() >= MIN_PROTOCOL;
    }

    public VanillaEntityType<?> definition() {
        return this.definition;
    }

    public GeyserTextDisplayEntity create(EntitySpawnContext context, GeyserPlayer player) {
        return new GeyserTextDisplayEntity(context, player, this.glyphs);
    }
}
