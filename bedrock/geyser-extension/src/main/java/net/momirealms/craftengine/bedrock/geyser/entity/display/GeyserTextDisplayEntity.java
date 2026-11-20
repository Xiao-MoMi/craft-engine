package net.momirealms.craftengine.bedrock.geyser.entity.display;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bedrock.GuiTitlePart;
import net.momirealms.craftengine.bedrock.geyser.glyph.GeyserGlyphData;
import net.momirealms.craftengine.bedrock.geyser.player.GeyserPlayer;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.AdventureTranslationRendererProxy;
import net.momirealms.craftengine.bedrock.geyser.proxy.text.MessageTranslatorProxy;
import net.momirealms.craftengine.bedrock.geyser.util.GeyserTextParts;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.imaginary.Quaternionf;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.debugshape.DebugShape;
import org.cloudburstmc.protocol.bedrock.data.debugshape.DebugText;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.TextDisplayEntity;
import org.geysermc.geyser.entity.type.Tickable;
import org.geysermc.geyser.network.bedrock.GameProtocol;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

final class GeyserTextDisplayEntity extends TextDisplayEntity implements Tickable {
    private static final AtomicLong IDS = new AtomicLong(Long.MIN_VALUE);
    private static final Color TRANSPARENT = new Color(0, true);
    private static final Color INVISIBLE_TEXT = new Color(0x00FFFFFF, true);
    private static final int DEFAULT_BACKGROUND = 0x40000000;
    private static final int CENTER = 3;
    private static final int SHADOW = 1;
    private static final int SEE_THROUGH = 2;
    private static final int USE_DEFAULT_BACKGROUND = 4;
    private static final int ALIGN_LEFT = 8;
    private static final int ALIGN_RIGHT = 16;
    private static final boolean[] BEDROCK_COLOR_CODES = new boolean[128];

    static {
        String codes = "0123456789abcdefghijmnpqstu";
        for (int index = 0; index < codes.length(); index++) {
            BEDROCK_COLOR_CODES[codes.charAt(index)] = true;
        }
    }
    private final GeyserPlayer player;
    private final GeyserGlyphData glyphs;
    private final TextDisplayInterpolation transform = new TextDisplayInterpolation();
    private final List<DebugText> shapes = new ArrayList<>();
    private Object textComponent;
    private String text = "";
    private String textLocale;
    private DisplayTextLayout layout;
    private DisplayTextPlane plane;
    private String backgroundText;
    private List<String> shadowLines;
    private int lineWidth = 200;
    private Color background = new Color(DEFAULT_BACKGROUND, true);
    private Color foreground = Color.WHITE;
    private Color shadowColor = new Color(63, 63, 63);
    private int light = 255;
    private int style;
    private int billboard;
    private float viewRange = 1;
    private boolean dirty = true;
    private boolean clientFacing;
    private boolean hidden;
    private Vector3f seatOffset;
    private Vector3f vehiclePosition;
    private Vector3f passengerAnchor;
    private float vehicleYaw;
    private float seatHeight;
    private int dimension = Integer.MIN_VALUE;
    private float cameraYaw;
    private float cameraPitch;
    private int teleportDuration;
    private int moveStep;
    private Vector3f moveFrom;
    private Vector3f moveTo;
    private float yawFrom;
    private float pitchFrom;
    private float targetYaw;
    private float targetPitch;

    GeyserTextDisplayEntity(EntitySpawnContext context, GeyserPlayer player, GeyserGlyphData glyphs) {
        super(context);
        this.player = player;
        this.glyphs = glyphs;
    }

    TextDisplayInterpolation transform() {
        return this.transform;
    }

    void teleportDuration(int ticks) {
        this.teleportDuration = Math.clamp(ticks, 0, 59);
    }

    void billboard(byte value) {
        if (this.billboard != value) {
            this.billboard = value;
            this.dirty = true;
        }
    }

    void viewRange(float value) {
        float range = Math.max(0, value);
        if (this.viewRange != range) {
            this.viewRange = range;
            this.dirty = true;
        }
    }

    void lineWidth(int value) {
        int width = Math.max(1, value);
        if (this.lineWidth != width) {
            this.lineWidth = width;
            this.layout = null;
            this.dirty = true;
        }
    }

    void background(int value) {
        this.transform.background(value);
    }

    void opacity(byte value) {
        this.transform.opacity(Byte.toUnsignedInt(value));
    }

    void brightness(int packed) {
        int level = Math.max((packed >>> 4) & 15, (packed >>> 20) & 15);
        float light = level / 15f;
        int tint = packed == -1 ? 255 : Math.round(255 * light / (4 - 3 * light));
        if (this.light != tint) {
            this.light = tint;
            this.dirty = true;
        }
    }

    void style(byte value) {
        if (this.style != value) {
            this.style = value;
            this.dirty = true;
        }
    }

    @Override
    public void setText(EntityMetadata<Component, ?> metadata) {
        Object component = metadata.getValue();
        if (Objects.equals(this.textComponent, component)) {
            return;
        }
        super.setText(metadata);
        this.textComponent = component;
        this.textLocale = this.session.locale();
        this.text = Objects.toString(this.metadata.get(EntityDataTypes.NAME), "");
        this.layout = null;
        this.dirty = true;
    }

    @Override
    public void setTranslation(EntityMetadata<Vector3f, ?> metadata) {
        this.transform.translation(metadata.getValue());
        super.setTranslation(metadata);
    }

    @Override
    protected void setInvisible(boolean value) {
        super.setInvisible(value);
        this.hidden = value;
        this.dirty = true;
    }

    @Override
    public void setVehicle(Entity vehicle) {
        if (this.vehicle != vehicle) {
            this.seatOffset = null;
            this.vehiclePosition = null;
            this.dirty = true;
        }
        super.setVehicle(vehicle);
    }

    @Override
    public void setRiderSeatPosition(Vector3f offset) {
        super.setRiderSeatPosition(offset);
        if (!Objects.equals(this.seatOffset, offset)) {
            this.seatOffset = offset;
            this.vehiclePosition = null;
            this.dirty = true;
        }
    }

    @Override
    public void spawnEntity() {
        this.metadata.put(EntityDataTypes.NAME, "");
        super.spawnEntity();
        this.dirty = true;
        this.updateDisplay();
    }

    @Override
    public void despawnEntity() {
        this.removeShapes(0);
        super.despawnEntity();
    }

    @Override
    public void updateBedrockMetadata() {
        this.updateDisplay();
        this.metadata.put(EntityDataTypes.NAME, "");
        super.updateBedrockMetadata();
    }

    @Override
    public void tick() {
        this.transform.tick();
        if (this.moveTo != null) {
            float fraction = Math.min(1, ++this.moveStep / (float) Math.max(1, this.teleportDuration));
            super.moveAbsoluteRaw(GenericMath.lerp(this.moveFrom, this.moveTo, fraction),
                    this.yawFrom + GenericMath.wrapAngleDeg(this.targetYaw - this.yawFrom) * fraction,
                    this.pitchFrom + GenericMath.wrapAngleDeg(this.targetPitch - this.pitchFrom) * fraction,
                    this.targetYaw, this.onGround, false);
            this.dirty = true;
            if (fraction == 1) {
                this.moveTo = null;
            }
        }
        this.updateDisplay();
    }

    @Override
    public void moveRelativeRaw(double x, double y, double z, float yaw, float pitch, float headYaw, boolean grounded) {
        Vector3f base = this.moveTo == null ? this.position : this.moveTo;
        this.moveAbsoluteRaw(base.add(x, y, z), yaw, pitch, headYaw, grounded, false);
    }

    @Override
    public void moveAbsoluteRaw(Vector3f position, float yaw, float pitch, float headYaw, boolean grounded, boolean teleported) {
        this.setOnGround(grounded);
        this.targetYaw = yaw;
        this.targetPitch = pitch;
        if (this.valid && this.teleportDuration > 0) {
            this.moveFrom = this.position;
            this.moveTo = position;
            this.yawFrom = this.yaw;
            this.pitchFrom = this.pitch;
            this.moveStep = 0;
        } else {
            this.moveTo = null;
            super.moveAbsoluteRaw(position, yaw, pitch, headYaw, grounded, teleported);
            this.dirty = true;
        }
    }

    private void updateDisplay() {
        if (!this.valid) {
            return;
        }
        int dimension = this.session.getBedrockDimension().bedrockId();
        Vector3f passengerAnchor = this.passengerAnchor();
        if (dimension != this.dimension || !Objects.equals(passengerAnchor, this.passengerAnchor)) {
            this.dirty = true;
            this.dimension = dimension;
            this.passengerAnchor = passengerAnchor;
        }
        String locale = this.session.locale();
        if (this.textComponent != null && !Objects.equals(this.textLocale, locale)) {
            this.textLocale = locale;
            this.text = MessageTranslatorProxy.INSTANCE.convertMessage(this.textComponent, locale);
            this.layout = null;
            this.dirty = true;
        }
        boolean changed = this.dirty || this.transform.needsUpdate();
        if (!changed && (this.billboard == 0 || this.clientFacing)) {
            return;
        }
        this.dirty = false;
        this.transform.sample();
        if (this.hidden || this.text.isEmpty() || this.viewRange == 0) {
            this.removeShapes(0);
            return;
        }
        if (this.layout == null) {
            Object translated = AdventureTranslationRendererProxy.INSTANCE.render(MessageTranslatorProxy.INSTANCE.renderer(), this.textComponent, locale);
            List<GuiTitlePart> parts = new ArrayList<>();
            boolean useFontMetrics = GeyserTextParts.collectParts(translated, "minecraft:default", 0xFFFFFF, 0, parts, locale);
            if (useFontMetrics) {
                for (int index = 0, size = parts.size(); index < size; index++) {
                    if (parts.get(index).text().indexOf('§') >= 0) {
                        useFontMetrics = false;
                        break;
                    }
                }
            }
            this.layout = useFontMetrics
                    ? DisplayTextLayout.wrap(parts, this.glyphs.metrics(), this.lineWidth, locale)
                    : DisplayTextLayout.wrapLegacy(this.text, this.lineWidth);
            this.backgroundText = null;
            this.shadowLines = null;
        }
        DisplayPose pose = this.transform.currentPose();
        this.clientFacing = this.billboard == CENTER && this.vehicle == null && (this.style & SHADOW) == 0
                && this.layout.lines().size() == 1 && pose.translation().equals(Vector3f.ZERO)
                && pose.left().equals(Quaternionf.IDENTITY) && pose.right().equals(Quaternionf.IDENTITY)
                && pose.scale().getX() > 0 && pose.scale().getX() == pose.scale().getY();
        boolean followsYaw = !this.clientFacing && (this.billboard == 1 || this.billboard == CENTER);
        boolean followsPitch = !this.clientFacing && (this.billboard == 2 || this.billboard == CENTER);
        Vector2f camera = followsYaw || followsPitch ? this.player.displayCameraAngles() : null;
        float cameraYaw = followsYaw ? (camera == null ? this.session.getPlayerEntity().getYaw() : camera.getX()) : 0;
        float cameraPitch = followsPitch ? (camera == null ? this.session.getPlayerEntity().getPitch() : camera.getY()) : 0;
        if (!changed && cameraYaw == this.cameraYaw && cameraPitch == this.cameraPitch) {
            return;
        }
        this.cameraYaw = cameraYaw;
        this.cameraPitch = cameraPitch;
        this.render(pose, followsYaw, followsPitch);
    }

    private Vector3f passengerAnchor() {
        if (this.vehicle == null) {
            return null;
        }
        GeyserModelDisplayEntity displayVehicle = this.vehicle instanceof GeyserModelDisplayEntity display ? display : null;
        Vector3f position = displayVehicle == null ? this.vehicle.getPosition() : displayVehicle.renderPosition();
        Vector3f seat = this.seatOffset;
        float height = seat == null ? this.vehicle.getBoundingBoxHeight() : seat.getY();
        boolean offsetSideways = seat != null && (seat.getX() != 0 || seat.getZ() != 0);
        float yaw = offsetSideways ? (displayVehicle == null ? this.vehicle.getYaw() : displayVehicle.renderYaw()) : 0;
        if (position.equals(this.vehiclePosition) && yaw == this.vehicleYaw && height == this.seatHeight) {
            return this.passengerAnchor;
        }
        this.vehiclePosition = position;
        this.vehicleYaw = yaw;
        this.seatHeight = height;
        return offsetSideways ? position.add(Quaternionf.fromAngleDegAxis(-yaw, Vector3f.UNIT_Y).rotate(seat)) : position.up(height);
    }

    private void render(DisplayPose pose, boolean followsYaw, boolean followsPitch) {
        this.updateColors();
        int lineCount = this.layout.lines().size();
        boolean transparent = this.background.getAlpha() == 0;
        boolean depthTest = (this.style & SEE_THROUGH) == 0;
        float renderDistance = this.viewRange * 64;
        if (this.clientFacing) {
            DisplayTextLayout.Line line = this.layout.lines().getFirst();
            if (transparent && (!line.visible() || this.foreground.getAlpha() == 0)) {
                this.removeShapes(0);
            } else {
                float scale = pose.scale().getX();
                this.writeShape(
                        0,
                        line.text(),
                        this.position.up(DisplayTextPlane.LINE_CENTER * DisplayTextPlane.PIXEL * scale),
                        Vector3f.ZERO,
                        scale,
                        this.foreground,
                        this.background,
                        0,
                        false,
                        depthTest,
                        renderDistance
                );
                this.removeShapes(1);
            }
            return;
        }
        float yaw = followsYaw ? this.cameraYaw + 180 : this.yaw;
        float pitch = followsPitch ? -this.cameraPitch : this.pitch;
        Vector3f base = this.passengerAnchor == null ? this.position : this.passengerAnchor;
        if (this.plane == null) {
            this.plane = new DisplayTextPlane(GameProtocol.is26_50orHigher(this.session.protocolVersion()));
        }
        float alignment = (this.style & ALIGN_LEFT) != 0 ? -0.5f : ((this.style & ALIGN_RIGHT) != 0 ? 0.5f : 0);
        if (!this.plane.prepare(pose, base, yaw, pitch, this.layout, alignment)) {
            this.removeShapes(0);
            return;
        }
        Vector3f angles = this.plane.rotation();
        float scale = this.plane.scale();
        boolean shadowed = (this.style & SHADOW) != 0 && this.foreground.getAlpha() != 0;
        boolean separateBackground = !transparent && lineCount > 1;
        int shapeIndex = 0;
        if (separateBackground) {
            this.writeShape(
                    shapeIndex++,
                    this.backgroundText(),
                    this.plane.background(),
                    angles,
                    scale,
                    INVISIBLE_TEXT,
                    this.background,
                    this.plane.backgroundLineGap(),
                    true,
                    depthTest,
                    renderDistance
            );
        }
        Color lineBackground = separateBackground ? TRANSPARENT : this.background;
        // 基岩多行 DebugText 的原生基线和行距都与 Java 不同，保留每行的独立位置
        for (int index = 0; index < lineCount; index++) {
            DisplayTextLayout.Line line = this.layout.lines().get(index);
            if (lineBackground.getAlpha() == 0 && (!line.visible() || this.foreground.getAlpha() == 0)) {
                continue;
            }
            if (shadowed && line.visible()) {
                this.writeShape(
                        shapeIndex++,
                        this.shadowLine(index),
                        this.plane.shadow(index),
                        angles,
                        scale,
                        this.shadowColor,
                        TRANSPARENT,
                        0,
                        true,
                        depthTest,
                        renderDistance
                );
            }
            this.writeShape(
                    shapeIndex++,
                    line.text(),
                    this.plane.line(index),
                    angles,
                    scale,
                    this.foreground,
                    lineBackground,
                    0,
                    true,
                    depthTest,
                    renderDistance
            );
        }
        this.removeShapes(shapeIndex);
    }

    private void updateColors() {
        int opacity = this.transform.currentOpacity();
        int alpha = opacity < 26 ? 0 : opacity;
        int foreground = alpha << 24 | this.light * 0x010101;
        if (this.foreground.getRGB() != foreground) {
            this.foreground = new Color(foreground, true);
            this.shadowColor = new Color(alpha << 24 | (this.light / 4) * 0x010101, true);
        }
        int background = (this.style & USE_DEFAULT_BACKGROUND) != 0 ? DEFAULT_BACKGROUND : this.transform.currentBackground();
        if (this.light != 255) {
            background = background & 0xFF000000
                    | ((background >>> 16) & 255) * this.light / 255 << 16
                    | ((background >>> 8) & 255) * this.light / 255 << 8
                    | (background & 255) * this.light / 255;
        }
        if (this.background.getRGB() != background) {
            this.background = new Color(background, true);
        }
    }

    private String backgroundText() {
        if (this.backgroundText == null) {
            this.prepareShadowLines();
            this.backgroundText = String.join("\n", this.shadowLines);
        }
        return this.backgroundText;
    }

    private String shadowLine(int index) {
        this.prepareShadowLines();
        return this.shadowLines.get(index);
    }

    private void prepareShadowLines() {
        if (this.shadowLines == null) {
            this.shadowLines = new ArrayList<>(this.layout.lines().size());
            for (DisplayTextLayout.Line line : this.layout.lines()) {
                this.shadowLines.add(withoutTextColors(line.text()));
            }
        }
    }

    private static String withoutTextColors(String text) {
        int first = text.indexOf('§');
        if (first < 0) {
            return text;
        }
        int length = text.length();
        while (first >= 0 && first + 1 < length && !isBedrockColorCode(text.charAt(first + 1))) {
            first = text.indexOf('§', first + 1);
        }
        if (first < 0 || first + 1 == length) {
            return text;
        }
        StringBuilder masked = new StringBuilder(length - 2).append(text, 0, first);
        for (int index = first + 2; index < length; index++) {
            char character = text.charAt(index);
            if (character == '§' && index + 1 < length && isBedrockColorCode(text.charAt(index + 1))) {
                index++;
            } else {
                masked.append(character);
            }
        }
        return masked.toString();
    }

    private static boolean isBedrockColorCode(char code) {
        return code < BEDROCK_COLOR_CODES.length && BEDROCK_COLOR_CODES[code];
    }

    private void writeShape(int index,
                            String text,
                            Vector3f position,
                            Vector3f rotation,
                            float scale,
                            Color color,
                            Color background,
                            float lineGap,
                            boolean useRotation,
                            boolean depthTest,
                            float renderDistance) {
        DebugText previous = index < this.shapes.size() ? this.shapes.get(index) : null;
        boolean sameDimension = previous != null && previous.getDimension() == this.dimension;
        Float previousScale = previous == null ? null : previous.getScale();
        Float previousDistance = previous == null ? null : previous.getMaximumRenderDistance();
        boolean sameScale = previousScale != null && previousScale == scale;
        boolean sameDistance = previousDistance != null && previousDistance == renderDistance;
        if (sameDimension
                && Objects.equals(previous.getPosition(), position)
                && Objects.equals(previous.getRotation(), rotation)
                && sameScale && previous.getText().equals(text)
                && Objects.equals(previous.getColor(), color)
                && Objects.equals(previous.getBackgroundColor(), background)
                && previous.getLineGapHeight() == lineGap
                && previous.isUseRotation() == useRotation
                && previous.isDepthTest() == depthTest
                && sameDistance) {
            return;
        }
        if (previous != null && !sameDimension) {
            this.player.queueShape(new DebugShape(previous.getId(), previous.getDimension()));
        }
        DebugText shape = new DebugText();
        shape.setId(sameDimension ? previous.getId() : IDS.getAndIncrement());
        shape.setDimension(this.dimension);
        shape.setPosition(position);
        shape.setRotation(rotation);
        shape.setScale(sameScale ? previousScale : Float.valueOf(scale));
        shape.setUseRotation(useRotation);
        shape.setColor(color);
        shape.setBackgroundColor(background);
        shape.setLineGapHeight(lineGap);
        shape.setText(text);
        shape.setDepthTest(depthTest);
        shape.setMaximumRenderDistance(sameDistance ? previousDistance : Float.valueOf(renderDistance));
        if (previous == null) {
            this.shapes.add(shape);
        } else {
            this.shapes.set(index, shape);
        }
        this.player.queueShape(shape);
    }

    private void removeShapes(int remaining) {
        for (int index = this.shapes.size() - 1; index >= remaining; index--) {
            DebugText shape = this.shapes.remove(index);
            this.player.queueShape(new DebugShape(shape.getId(), shape.getDimension()));
        }
    }
}
