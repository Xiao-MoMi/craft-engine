package net.momirealms.craftengine.core.util;

import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.TagStringIO;
import net.kyori.adventure.text.Component;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;
import net.momirealms.sparrow.nbt.NBT;
import net.momirealms.sparrow.nbt.StringTag;
import net.momirealms.sparrow.nbt.Tag;
import net.momirealms.sparrow.nbt.adventure.NBTComponentSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * An NBT component serializer that preserves custom click-event payloads while Adventure processes components.
 *
 * <p>Adventure stores custom payloads as SNBT strings, while the network component format accepts arbitrary
 * NBT tags. This decorator converts between those representations without changing the wrapped serializer's
 * other behavior.</p>
 */
final class CustomClickEventNBTSerializer implements NBTComponentSerializer {
    private static final TagStringIO TAG_STRING_IO = TagStringIO.builder()
            .acceptLegacy(true)
            .acceptHeterogeneousLists(true)
            .build();

    private final NBTComponentSerializer delegate;

    /**
     * Creates a serializer that decorates the specified NBT component serializer.
     *
     * @param delegate the serializer responsible for the underlying component conversion
     */
    CustomClickEventNBTSerializer(NBTComponentSerializer delegate) {
        this.delegate = delegate;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Custom payload tags are encoded as SNBT before being passed to Adventure.</p>
     */
    @Override
    public Component deserialize(Tag input) {
        if (!containsCustomPayload(input)) {
            return this.delegate.deserialize(input);
        }
        Tag copied = input.deepClone();
        transformCustomPayloads(copied, true);
        return this.delegate.deserialize(copied);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Custom payload SNBT is restored to its original NBT tag type before serialization completes.</p>
     */
    @Override
    public Tag serialize(Component component) {
        Tag output = this.delegate.serialize(component);
        transformCustomPayloads(output, false);
        return output;
    }

    /**
     * Checks whether a tag tree contains a custom click event with a payload.
     *
     * @param tag the root tag to inspect
     * @return {@code true} if a custom click-event payload is present
     */
    private static boolean containsCustomPayload(Tag tag) {
        if (tag instanceof CompoundTag compoundTag) {
            Tag clickEventTag = compoundTag.get("click_event");
            if (clickEventTag instanceof CompoundTag clickEvent
                    && "custom".equals(clickEvent.getString("action"))
                    && clickEvent.get("payload") != null) {
                return true;
            }
            for (Tag child : compoundTag.values()) {
                if (containsCustomPayload(child)) {
                    return true;
                }
            }
        } else if (tag instanceof ListTag listTag) {
            for (Tag child : listTag) {
                if (containsCustomPayload(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Converts custom click-event payloads recursively between native NBT tags and SNBT strings.
     *
     * @param tag the root tag to transform
     * @param encodeAsSnbt {@code true} to encode native tags as SNBT; {@code false} to restore native tags
     */
    private static void transformCustomPayloads(Tag tag, boolean encodeAsSnbt) {
        if (tag instanceof CompoundTag compoundTag) {
            Tag clickEventTag = compoundTag.get("click_event");
            if (clickEventTag instanceof CompoundTag clickEvent
                    && "custom".equals(clickEvent.getString("action"))) {
                Tag payload = clickEvent.get("payload");
                if (payload != null) {
                    if (encodeAsSnbt) {
                        clickEvent.put("payload", new StringTag(payload.toString()));
                    } else if (payload instanceof StringTag stringPayload) {
                        Tag restored = parseSnbt(stringPayload.value());
                        if (restored != null) {
                            clickEvent.put("payload", restored);
                        }
                    }
                }
            }
            for (Tag child : compoundTag.values()) {
                transformCustomPayloads(child, encodeAsSnbt);
            }
        } else if (tag instanceof ListTag listTag) {
            for (Tag child : listTag) {
                transformCustomPayloads(child, encodeAsSnbt);
            }
        }
    }

    /**
     * Parses an arbitrary SNBT value into a Sparrow NBT tag.
     *
     * @param snbt the SNBT value to parse
     * @return the parsed tag, or {@code null} if the value is not valid SNBT
     */
    private static Tag parseSnbt(String snbt) {
        try {
            CompoundBinaryTag parsed = TAG_STRING_IO.asCompound("{value:" + snbt + "}");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            BinaryTagIO.writer().writeNameless(parsed, (DataOutput) new DataOutputStream(bytes));
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                return NBT.readCompound((DataInput) input, false).get("value");
            }
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    @Override
    public boolean modernEvent() {
        return this.delegate.modernEvent();
    }

    @Override
    public boolean dataComponentRelease() {
        return this.delegate.dataComponentRelease();
    }

    @Override
    public boolean compactTextComponent() {
        return this.delegate.compactTextComponent();
    }

    @Override
    public boolean serializeComponentType() {
        return this.delegate.serializeComponentType();
    }

    @Override
    public boolean intArrayUUID() {
        return this.delegate.intArrayUUID();
    }
}
