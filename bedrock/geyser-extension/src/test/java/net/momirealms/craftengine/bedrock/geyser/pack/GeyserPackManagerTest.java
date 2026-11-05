package net.momirealms.craftengine.bedrock.geyser.pack;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockPackFiles;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;
import net.momirealms.craftengine.bedrock.geyser.TestPackFiles;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GeyserPackManagerTest {
    @TempDir
    private Path directory;
    private long timestamp = 1000;

    @Test
    void refreshKeepsExistingSessionsAndStartupRegistrationsOnTheirSnapshots() throws Exception {
        Path source = this.writePair("first", UUID.randomUUID(), 0xe001, false, false);
        try (MockedStatic<PackCodec> codecs = this.mockPackCodecs(); GeyserPackManager manager = this.manager()) {
            manager.prepare();
            List<Path> registrations = manager.registrationFiles();
            GeyserConnection original = mock(GeyserConnection.class);
            manager.load(this.event(original));

            UUID refreshed = UUID.randomUUID();
            this.writePair("first", refreshed, 0xe002, false, false);
            manager.refresh();
            GeyserConnection joined = mock(GeyserConnection.class);
            manager.load(this.event(joined));

            assertEquals(0xe001, manager.text(original).fonts().get("first").get(0xe000));
            assertEquals(0xe002, manager.text(joined).fonts().get("first").get(0xe000));
            assertEquals(registrations, manager.registrationFiles());
            assertEquals(refreshed, manager.statuses().get(source).served());
            assertEquals(GeyserPackStatus.State.READY, manager.statuses().get(source).state());
            manager.disconnect(joined);
            assertEquals(0xe001, manager.text(joined).fonts().get("first").get(0xe000));
        }
    }

    @Test
    void registrationChangesRequireRestartAndKeepTheServedSnapshot() throws Exception {
        UUID registered = UUID.randomUUID();
        Path source = this.writePair("first", registered, 0xe001, false, false);
        try (MockedStatic<PackCodec> codecs = this.mockPackCodecs(); GeyserPackManager manager = this.manager()) {
            manager.prepare();
            UUID generated = UUID.randomUUID();
            this.writePair("first", generated, 0xe002, true, false);
            manager.refresh();
            GeyserPackStatus status = manager.statuses().get(source);
            assertEquals(GeyserPackStatus.State.RESTART_REQUIRED, status.state());
            assertEquals(generated, status.generated());
            assertEquals(registered, status.served());
            GeyserConnection joined = mock(GeyserConnection.class);
            manager.load(this.event(joined));
            assertEquals(0xe001, manager.text(joined).fonts().get("first").get(0xe000));
        }
    }

    @Test
    void failedTextRefreshDoesNotContaminateTheNextPackPublication() throws Exception {
        Path first = this.writePair("first", UUID.randomUUID(), 0xe001, false, false);
        Path second = this.writePair("second", UUID.randomUUID(), 0xe010, false, false);
        try (MockedStatic<PackCodec> codecs = this.mockPackCodecs(); GeyserPackManager manager = this.manager()) {
            manager.prepare();
            this.writePair("first", UUID.randomUUID(), 0xe002, false, true);
            this.writePair("second", UUID.randomUUID(), 0xe020, false, false);
            manager.refresh();
            GeyserConnection joined = mock(GeyserConnection.class);
            manager.load(this.event(joined));
            assertEquals(GeyserPackStatus.State.WAITING_FOR_PAIR, manager.statuses().get(first).state());
            assertEquals(GeyserPackStatus.State.READY, manager.statuses().get(second).state());
            assertEquals(0xe001, manager.text(joined).fonts().get("first").get(0xe000));
            assertEquals(0xe020, manager.text(joined).fonts().get("second").get(0xe000));
        }
    }

    private GeyserPackManager manager() throws IOException {
        return new GeyserPackManager(this.directory.resolve("packs"), this.directory.resolve("data"),
                this.directory.resolve("snapshots"), mock(ExtensionLogger.class));
    }

    private SessionLoadResourcePacksEvent event(GeyserConnection connection) {
        SessionLoadResourcePacksEvent event = mock(SessionLoadResourcePacksEvent.class);
        when(event.connection()).thenReturn(connection);
        when(event.resourcePacks()).thenReturn(List.of());
        return event;
    }

    private MockedStatic<PackCodec> mockPackCodecs() {
        MockedStatic<PackCodec> codecs = mockStatic(PackCodec.class);
        codecs.when(() -> PackCodec.path(any(Path.class))).thenAnswer(invocation -> {
            ResourcePack pack = mock(ResourcePack.class);
            PackCodec codec = mock(PackCodec.class, call -> call.getMethod().getName().equals("create")
                    ? pack : RETURNS_DEFAULTS.answer(call));
            when(pack.codec()).thenReturn(codec);
            return codec;
        });
        return codecs;
    }

    private Path writePair(String name, UUID uuid, int character, boolean changedRegistration, boolean invalidGlyphs) throws IOException {
        Path pack = this.directory.resolve("packs").resolve(name + ".mcpack");
        Path data = BedrockPackFiles.data(pack, this.directory.resolve("data"));
        String manifest = "{\"header\":{\"uuid\":\"" + uuid + "\",\"version\":[1,0,0]}}";
        JsonObject items = TestPackFiles.plan(RegistrationPlanKind.ITEM, "[]");
        if (changedRegistration) {
            items.addProperty("source_version", "1.21.5");
        }
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("manifest.json", manifest);
        entries.put(RegistrationPlanKind.ITEM.file(), items.toString());
        entries.put(RegistrationPlanKind.BLOCK.file(), TestPackFiles.plan(RegistrationPlanKind.BLOCK, "[]").toString());
        entries.put(BedrockPackLayout.GLYPHS, invalidGlyphs ? "{}" : "{\"metrics\":{\"" + name
                + "\":{\"E000\":{\"character\":" + character + ",\"advance\":8,\"bearing\":0}}}}");
        TestPackFiles.writeArchive(pack, Map.of("manifest.json", manifest));
        TestPackFiles.writeArchive(data, entries);
        Files.setLastModifiedTime(pack, FileTime.fromMillis(this.timestamp++));
        Files.setLastModifiedTime(data, FileTime.fromMillis(this.timestamp++));
        return pack;
    }
}
