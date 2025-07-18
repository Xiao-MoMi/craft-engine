package net.momirealms.craftengine.core.item.recipe.network.legacy;

import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;

public interface LegacyRecipeHolder {

    void write(FriendlyByteBuf buf);

    LegacyRecipe recipe();

    static LegacyRecipeHolder read(FriendlyByteBuf buf) {
        if (VersionHelper.isOrAbove1_20_5()) {
            return ModernRecipeHolderImpl.read(buf);
        } else {
            return LegacyRecipeHolderImpl.read(buf);
        }
    }

    record LegacyRecipeHolderImpl(Key id, Key type, LegacyRecipe recipe) implements LegacyRecipeHolder {

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeKey(this.type);
            buf.writeKey(this.id);
            this.recipe.write(buf);
        }

        public static LegacyRecipeHolder read(FriendlyByteBuf buf) {
            Key type = buf.readKey();
            Key id = buf.readKey();
            return new LegacyRecipeHolderImpl(id, type, BuiltInRegistries.LEGACY_RECIPE_TYPE.getValue(type).read(buf));
        }
    }

    record ModernRecipeHolderImpl(Key id, int type, LegacyRecipe recipe) implements LegacyRecipeHolder {

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeKey(this.id);
            buf.writeVarInt(this.type);
            this.recipe.write(buf);
        }

        public static LegacyRecipeHolder read(FriendlyByteBuf buf) {
            Key id = buf.readKey();
            int type = buf.readVarInt();
            return new ModernRecipeHolderImpl(id, type, BuiltInRegistries.LEGACY_RECIPE_TYPE.getValue(type).read(buf));
        }
    }
}
