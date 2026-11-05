package net.momirealms.craftengine.bedrock.geyser.block;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;
import net.momirealms.craftengine.bedrock.geyser.TestPackFiles;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class GeyserBlockManagerTest {
    @TempDir
    private Path directory;

    @Test
    void preservesFirstDefinitionAndContinuesAfterRegistrationFailure() throws Exception {
        Path first = this.directory.resolve("first.cedata");
        Path second = this.directory.resolve("second.cedata");
        TestPackFiles.writePlan(first, RegistrationPlanKind.BLOCK, """
                [{"name":"shared","result":"first"},{"name":"broken"},{"name":"last","result":"last"}]
                """);
        TestPackFiles.writePlan(second, RegistrationPlanKind.BLOCK, """
                [{"name":"shared","result":"second"}]
                """);
        ExtensionLogger logger = mock(ExtensionLogger.class);
        GeyserDefineCustomBlocksEvent event = mock(GeyserDefineCustomBlocksEvent.class);
        GeyserBlockManager manager = new GeyserBlockManager(logger);
        try (MockedStatic<GeyserBlockRegistrationAdapter> adapter = mockStatic(GeyserBlockRegistrationAdapter.class)) {
            adapter.when(() -> GeyserBlockRegistrationAdapter.registerBlock(any(), eq(event))).thenAnswer(invocation -> {
                JsonObject entry = invocation.getArgument(0);
                if (entry.get("name").getAsString().equals("broken")) {
                    throw new IllegalArgumentException("Invalid block");
                }
                return entry.get("result").getAsString();
            });
            manager.registerBlocks(List.of(first, second), event);
            assertEquals(Map.of("shared", "first", "last", "last"), manager.displayBlocks());
            adapter.verify(() -> GeyserBlockRegistrationAdapter.registerBlock(any(), eq(event)), times(3));
            verify(logger).error(contains("Failed to register display block broken"), any(RuntimeException.class));

            manager.registerBlocks(List.of(second), event);
            assertEquals(Map.of("shared", "second"), manager.displayBlocks());
        }
    }
}
