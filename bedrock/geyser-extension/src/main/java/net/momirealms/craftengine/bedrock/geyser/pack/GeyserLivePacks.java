package net.momirealms.craftengine.bedrock.geyser.pack;

import net.momirealms.craftengine.bedrock.BedrockPackFiles;
import net.momirealms.craftengine.bedrock.geyser.text.GeyserGlyphs;
import net.momirealms.craftengine.bedrock.geyser.text.GeyserTextData;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.PathPackCodec;
import org.geysermc.geyser.api.pack.option.ResourcePackOption;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public final class GeyserLivePacks implements AutoCloseable {
    private final Path directory;
    private final Path dataDirectory;
    private final Path store;
    private final ExtensionLogger logger;
    private final ScheduledExecutorService executor;
    private final Map<Path, Stamp> observed = new HashMap<>();
    private final Map<Path, Set<UUID>> identifiers = new HashMap<>();
    private final Map<GeyserConnection, State> sessions = new HashMap<>();
    private final Set<GeyserPackSnapshot> snapshots = new HashSet<>();
    private final Set<String> reported = new HashSet<>();
    private final Map<Path, GeyserPackStatus> statuses = new LinkedHashMap<>();
    private final State startup;
    private State current;

    public GeyserLivePacks(Path directory, Path dataDirectory, Path store, ExtensionLogger logger) throws IOException {
        this.directory = directory;
        this.dataDirectory = dataDirectory;
        this.store = store;
        this.logger = logger;
        this.removePreviousSnapshots();
        Map<Path, GeyserPackSnapshot> packs = new LinkedHashMap<>();
        for (Path path : this.sources()) {
            Stamp before = this.stamp(path);
            GeyserPackSnapshot snapshot = GeyserPackSnapshot.read(path, BedrockPackFiles.data(path, this.dataDirectory), store);
            if (!before.equals(this.stamp(path))) {
                snapshot.delete();
                throw new IOException("Pack changed while preparing startup registrations: " + path);
            }
            packs.put(path, snapshot);
            this.observed.put(path, before);
            this.identifiers.put(path, new HashSet<>(Set.of(snapshot.uuid())));
            this.snapshots.add(snapshot);
        }
        this.startup = state(packs);
        this.current = this.startup;
        this.executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "craftengine-pack-refresh");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        this.prepare();
        this.startup.packs.forEach((path, pack) -> this.status(path, pack.uuid(), GeyserPackStatus.State.READY, ""));
        this.executor.scheduleWithFixedDelay(this::refresh, 1, 1, TimeUnit.SECONDS);
    }

    public synchronized Map<Path, GeyserPackStatus> statuses() {
        return Map.copyOf(this.statuses);
    }

    private synchronized void status(Path path, UUID generated, GeyserPackStatus.State state, String reason) {
        GeyserPackSnapshot served = this.current.packs.get(path);
        GeyserPackSnapshot registered = this.startup.packs.get(path);
        GeyserPackStatus status = new GeyserPackStatus(generated, served == null ? null : served.uuid(),
                registered == null ? null : registered.uuid(), state, reason);
        if (!status.equals(this.statuses.put(path, status))) {
            String message = "CraftEngine pack " + path + ": " + status.message();
            if (state == GeyserPackStatus.State.READY) {
                this.logger.info(message);
            } else {
                this.logger.warning(message);
            }
        }
    }

    void prepare() {
        // ResourcePack providers initialize Geyser registries. Only call them after custom registration events finish.
        this.startup.packs.values().forEach(GeyserPackSnapshot::prepare);
    }

    public List<Path> registrationFiles() {
        return this.startup.packs.values().stream().map(GeyserPackSnapshot::data).toList();
    }

    public synchronized GeyserTextData text(GeyserConnection session) {
        return this.sessions.getOrDefault(session, this.startup).text;
    }

    public synchronized void load(SessionLoadResourcePacksEvent event) {
        State state = this.current;
        for (Map.Entry<Path, GeyserPackSnapshot> entry : state.packs.entrySet()) {
            Set<UUID> known = this.identifiers.get(entry.getKey());
            ResourcePackOption<?>[] options = new ResourcePackOption<?>[0];
            for (ResourcePack offered : List.copyOf(event.resourcePacks())) {
                if (known.contains(offered.uuid()) || (offered.codec() instanceof PathPackCodec file
                        && file.path().toAbsolutePath().normalize().equals(entry.getKey().toAbsolutePath().normalize()))) {
                    options = event.options(offered.uuid()).toArray(ResourcePackOption<?>[]::new);
                    event.unregister(offered.uuid());
                }
            }
            // Always offer the immutable snapshot, including the startup version. The output file may be replaced.
            event.register(entry.getValue().pack(), options);
        }
        this.sessions.put(event.connection(), state);
    }

    public synchronized void disconnect(GeyserConnection connection) {
        this.sessions.remove(connection);
    }

    public void refresh() {
        try {
            Map<Path, GeyserPackSnapshot> updated;
            synchronized (this) {
                updated = new LinkedHashMap<>(this.current.packs);
            }
            boolean changed = false;
            List<Path> sources = this.sources();
            for (Path path : this.startup.packs.keySet()) {
                if (!sources.contains(path)) {
                    this.observed.remove(path);
                    boolean removed = !Files.isRegularFile(path);
                    this.status(path, null, removed ? GeyserPackStatus.State.RESTART_REQUIRED : GeyserPackStatus.State.WAITING_FOR_PAIR,
                            removed ? "Resource pack removed; restart Geyser to update registrations." : "Missing extension data; keeping the previous pack.");
                }
            }
            for (Path path : sources) {
                Stamp before = this.stamp(path);
                if (before.equals(this.observed.get(path))) {
                    continue;
                }
                GeyserPackSnapshot next;
                try {
                    next = GeyserPackSnapshot.read(path, BedrockPackFiles.data(path, this.dataDirectory), this.store);
                } catch (IOException | RuntimeException exception) {
                    this.status(path, null, GeyserPackStatus.State.WAITING_FOR_PAIR, exception.toString());
                    continue;
                }
                boolean retained = false;
                try {
                    if (!before.equals(this.stamp(path))) {
                        continue;
                    }
                    GeyserPackSnapshot initial = this.startup.packs.get(path);
                    GeyserRegistrationChanges changes = initial == null ? null : initial.registrationChanges(next);
                    if (initial == null || changes.requiresRestart()) {
                        this.observed.put(path, before);
                        this.status(path, next.uuid(), GeyserPackStatus.State.RESTART_REQUIRED,
                                initial == null ? "New pack; restart Geyser to register it." : changes.summary());
                        continue;
                    }
                    updated.put(path, next);
                    next.prepare();
                    // Parse all text data before publishing; a failed refresh leaves every session on the previous state.
                    State state = state(updated);
                    synchronized (this) {
                        this.snapshots.add(next);
                        this.identifiers.get(path).add(next.uuid());
                        this.current = state;
                    }
                    this.observed.put(path, before);
                    retained = true;
                    changed = true;
                    this.status(path, next.uuid(), GeyserPackStatus.State.READY, "Reconnect to load this version.");
                } catch (IOException | RuntimeException exception) {
                    synchronized (this) {
                        updated = new LinkedHashMap<>(this.current.packs);
                    }
                    this.status(path, next.uuid(), GeyserPackStatus.State.WAITING_FOR_PAIR, exception.toString());
                } finally {
                    if (!retained) {
                        next.delete();
                    }
                }
            }
            if (changed) {
                this.logger.info("Updated CraftEngine resource pack snapshots. Reconnecting players receive the new pack and matching text data.");
            }
            this.retire();
        } catch (IOException | RuntimeException exception) {
            this.report("Could not refresh CraftEngine pack pair: " + exception.getMessage());
        }
    }

    private synchronized void retire() throws IOException {
        Set<GeyserPackSnapshot> used = new HashSet<>(this.startup.packs.values());
        used.addAll(this.current.packs.values());
        for (State state : this.sessions.values()) {
            used.addAll(state.packs.values());
        }
        for (GeyserPackSnapshot snapshot : new ArrayList<>(this.snapshots)) {
            if (!used.contains(snapshot)) {
                snapshot.delete();
                this.snapshots.remove(snapshot);
            }
        }
    }

    private void report(String message) {
        if (this.reported.add(message)) {
            this.logger.warning(message);
        }
    }

    private List<Path> sources() throws IOException {
        if (!Files.isDirectory(this.directory)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(this.directory)) {
            return paths.filter(Files::isRegularFile).filter(path -> {
                String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                return (name.endsWith(".mcpack") || name.endsWith(".zip")) && Files.isRegularFile(BedrockPackFiles.data(path, this.dataDirectory));
            }).sorted().toList();
        }
    }

    private void removePreviousSnapshots() throws IOException {
        if (!Files.isDirectory(this.store)) {
            return;
        }
        try (Stream<Path> paths = Files.list(this.store)) {
            for (Path path : paths.filter(Files::isDirectory).filter(path -> path.getFileName().toString().startsWith("pack-")).toList()) {
                Files.deleteIfExists(path.resolve("pack.mcpack"));
                Files.deleteIfExists(path.resolve("pack.ce-data"));
                Files.delete(path);
            }
        }
    }

    private static State state(Map<Path, GeyserPackSnapshot> packs) throws IOException {
        return new State(java.util.Collections.unmodifiableMap(new LinkedHashMap<>(packs)),
                GeyserGlyphs.read(packs.values().stream().map(GeyserPackSnapshot::data).toList()));
    }

    private Stamp stamp(Path pack) throws IOException {
        BasicFileAttributes resource = Files.readAttributes(pack, BasicFileAttributes.class);
        BasicFileAttributes data = Files.readAttributes(BedrockPackFiles.data(pack, this.dataDirectory), BasicFileAttributes.class);
        return new Stamp(resource.size(), resource.lastModifiedTime(), resource.fileKey(), data.size(), data.lastModifiedTime(), data.fileKey());
    }

    @Override
    public void close() {
        this.executor.shutdownNow();
    }

    private record State(Map<Path, GeyserPackSnapshot> packs, GeyserTextData text) {
    }

    private record Stamp(long packSize, java.nio.file.attribute.FileTime packTime, Object packKey,
                         long dataSize, java.nio.file.attribute.FileTime dataTime, Object dataKey) {
    }
}
