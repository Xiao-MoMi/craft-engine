package net.momirealms.craftengine.core.pack.bedrock.block;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.VectorUtils;

import java.io.IOException;
import java.nio.file.Path;

public final class BedrockBlockDefinitions {
    private final JsonObject definitions = new JsonObject();

    public void register(String name, String sound) {
        JsonObject block = new JsonObject();
        block.addProperty("sound", sound);
        this.definitions.add("geyser_custom:" + name, block);
    }

    public void write(Path root) throws IOException {
        if (!this.definitions.isEmpty()) {
            this.definitions.add(BedrockPackLayout.FORMAT_VERSION, VectorUtils.toJson(1, 1, 0));
            GsonHelper.writeJsonFile(this.definitions, root.resolve("blocks.json"));
        }
    }
}
