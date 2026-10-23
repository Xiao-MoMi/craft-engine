package net.momirealms.craftengine.core.pack.bedrock.item;

import java.io.IOException;

@FunctionalInterface
public interface BedrockItemHandler {
    boolean write(ItemRegistrationPlan.Entry entry,
                  BedrockItemGenerationContext context) throws IOException;
}
