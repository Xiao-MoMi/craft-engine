package net.momirealms.craftengine.bedrock.geyser.item;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;
import net.momirealms.craftengine.bedrock.geyser.TestPackFiles;
import net.momirealms.craftengine.bedrock.geyser.block.GeyserBlockManager;
import net.momirealms.craftengine.bedrock.geyser.item.component.GeyserItemComponentPatch;
import net.momirealms.craftengine.bedrock.geyser.item.component.GeyserItemComponentPatcher;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.session.GeyserSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "UnstableApiUsage"})
class GeyserItemManagerTest {
    @TempDir
    private Path directory;

    @Test
    void preservesVariantOrderAndPublishesSessionDataAfterSkippedAndFailedItems() throws Exception {
        Path plan = this.directory.resolve("items.cedata");
        TestPackFiles.writePlan(plan, RegistrationPlanKind.ITEM, """
                [
                  {"item_type":"minecraft:stone","bedrock_identifier":"broken"},
                  {"item_type":"minecraft:stone","bedrock_identifier":"unsupported"},
                  {"item_type":"minecraft:stone","bedrock_identifier":"valid","client_behavior":{"places_block":true}}
                ]
                """);
        ExtensionLogger logger = mock(ExtensionLogger.class);
        GeyserBlockManager blocks = mock(GeyserBlockManager.class);
        when(blocks.displayBlocks()).thenReturn(Map.of());
        GeyserItemManager manager = new GeyserItemManager(logger, blocks);
        GeyserDefineCustomItemsEvent event = mock(GeyserDefineCustomItemsEvent.class);
        CustomItemDefinition first = this.definition("craftengine:first");
        CustomItemDefinition second = this.definition("craftengine:second");
        GeyserItemComponentPatch patch = (data, mappings) -> data.toBuilder().putInt("patched", 1).build();
        GeyserItemRegistrationAdapter.ConvertedItem converted = new GeyserItemRegistrationAdapter.ConvertedItem(
                List.of(first, second), true, patch, Map.of("craftengine:first", "craftengine:head"));
        try (MockedStatic<Identifier> identifiers = mockStatic(Identifier.class);
             MockedConstruction<GeyserItemRegistrationAdapter> adapters = mockConstruction(GeyserItemRegistrationAdapter.class,
                     (adapter, context) -> when(adapter.convert(any(), anyMap())).thenAnswer(invocation -> {
                         JsonObject entry = invocation.getArgument(0);
                         return switch (entry.get("bedrock_identifier").getAsString()) {
                             case "broken" -> throw new IllegalArgumentException("Invalid item");
                             case "unsupported" -> throw new UnsupportedOperationException("Unsupported item");
                             default -> converted;
                         };
                     }));
             MockedStatic<GeyserItemComponentPatcher> patcher = mockStatic(GeyserItemComponentPatcher.class);
             MockedStatic<GeyserItemRuntime> runtime = mockStatic(GeyserItemRuntime.class);
             MockedStatic<GeyserHeldDye> dye = mockStatic(GeyserHeldDye.class)) {
            identifiers.when(() -> Identifier.of(anyString())).thenAnswer(invocation -> this.identifier(invocation.getArgument(0)));
            manager.registerItems(List.of(plan), event);
            InOrder order = inOrder(event);
            order.verify(event).register(any(Identifier.class), same(first));
            order.verify(event).register(any(Identifier.class), same(second));
            verify(blocks).registerBlockItem("craftengine:first");
            verify(blocks).registerBlockItem("craftengine:second");
            verify(logger).warning(contains("Skipped custom item"));
            verify(logger).error(contains("Failed to register custom item"), any(RuntimeException.class));

            GeyserSession session = mock(GeyserSession.class);
            manager.registerProperties(mock(GeyserDefineEntityPropertiesEvent.class));
            manager.initSessionDye(session);
            dye.verify(() -> GeyserHeldDye.install(eq(session), any(GeyserDyeProperties.class), any(GeyserDyeProperties.class)));
            manager.initSession(session);
            ArgumentCaptor<Map<String, GeyserItemComponentPatch>> patches = ArgumentCaptor.forClass(Map.class);
            patcher.verify(() -> GeyserItemComponentPatcher.install(eq(session), patches.capture()));
            assertEquals(Set.of("craftengine:first", "craftengine:second"), patches.getValue().keySet());
            NbtMap updated = patches.getValue().get("craftengine:second").apply(NbtMap.EMPTY, mock(ItemMappings.class));
            assertEquals(1, updated.getInt("patched"));
            runtime.verify(() -> GeyserItemRuntime.install(session,
                    Map.of("craftengine:first", first, "craftengine:second", second), Map.of("craftengine:first", "craftengine:head")));

            patcher.clearInvocations();
            runtime.clearInvocations();
            dye.clearInvocations();
            manager.registerItems(List.of(), event);
            manager.initSessionDye(session);
            manager.initSession(session);
            patcher.verifyNoInteractions();
            runtime.verifyNoInteractions();
            dye.verifyNoInteractions();
            verify(blocks, times(2)).clearBlockItems();
        }
    }

    private CustomItemDefinition definition(String name) {
        CustomItemDefinition definition = mock(CustomItemDefinition.class);
        CustomItemBedrockOptions options = mock(CustomItemBedrockOptions.class);
        Identifier identifier = this.identifier(name);
        when(definition.bedrockIdentifier()).thenReturn(identifier);
        when(definition.bedrockOptions()).thenReturn(options);
        when(options.defaultDyeableColor()).thenReturn(0xffffff);
        return definition;
    }

    private Identifier identifier(String name) {
        Identifier identifier = mock(Identifier.class);
        when(identifier.toString()).thenReturn(name);
        return identifier;
    }
}
