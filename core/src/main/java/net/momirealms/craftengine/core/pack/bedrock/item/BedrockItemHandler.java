package net.momirealms.craftengine.core.pack.bedrock.item;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

@FunctionalInterface
public interface BedrockItemHandler {

    // null 表示当前处理器不适用，允许下一个处理器尝试。
    @Nullable
    List<GeneratedItemVariant> write(ItemRegistrationPlan.Entry entry, BedrockItemGenerationContext context) throws IOException;
}
