package net.momirealms.craftengine.core.item.processor;

import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.Map;

public final class GetArgumentsProcessor implements ItemProcessor {
    public static final GetArgumentsProcessor INSTANCE = new GetArgumentsProcessor();
    public static final ItemProcessorFactory<GetArgumentsProcessor> FACTORY = v -> INSTANCE;

    @Override
    public void apply(ItemBuildContext context) {
        Tag sparrowTag = context.item().getSparrowTag(SetArgumentsProcessor.ARGUMENTS_TAG);
        if (sparrowTag instanceof CompoundTag compoundTag) {
            for (Map.Entry<String, Tag> entry : compoundTag.entrySet()) {
                String key = entry.getKey();
                Tag value = entry.getValue();
                context.contexts().withParameter(ContextKey.direct(key), value.getAsString());
            }
        }
    }
}
