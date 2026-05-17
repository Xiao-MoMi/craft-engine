package net.momirealms.craftengine.core.plugin.network.protocol.recipe.legacy;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;

public interface LegacyRecipeHolder {

    void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer);

    LegacyRecipe recipe();

    static LegacyRecipeHolder read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
        if (VersionHelper.isOrAbove1_20_5) {
            return ModernRecipeHolderImpl.read(buf, reader);
        } else {
            return LegacyRecipeHolderImpl.read(buf, reader);
        }
    }

    record LegacyRecipeHolderImpl(Key id, Key type, LegacyRecipe recipe) implements LegacyRecipeHolder {

        @Override
        public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
            buf.writeKey(this.type);
            buf.writeKey(this.id);
            this.recipe.write(buf, writer);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public static LegacyRecipeHolder read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
            Key type = buf.readKey();
            Key id = buf.readKey();
            return new LegacyRecipeHolderImpl(id, type, BuiltInRegistries.LEGACY_RECIPE_TYPE.getValue(type).read(buf, (FriendlyByteBuf.Reader) reader));
        }
    }

    record ModernRecipeHolderImpl(Key id, int type, LegacyRecipe recipe) implements LegacyRecipeHolder {

        @Override
        public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
            buf.writeKey(this.id);
            buf.writeVarInt(this.type);
            this.recipe.write(buf, writer);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public static LegacyRecipeHolder read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
            Key id = buf.readKey();
            int type = buf.readVarInt();
            return new ModernRecipeHolderImpl(id, type, BuiltInRegistries.LEGACY_RECIPE_TYPE.getValue(type).read(buf, (FriendlyByteBuf.Reader) reader));
        }
    }
}
