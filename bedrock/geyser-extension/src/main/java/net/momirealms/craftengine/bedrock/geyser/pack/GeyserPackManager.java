package net.momirealms.craftengine.bedrock.geyser.pack;

import net.momirealms.craftengine.bedrock.BedrockPackFiles;
import net.momirealms.craftengine.bedrock.geyser.text.GeyserGlyphs;
import net.momirealms.craftengine.bedrock.geyser.text.GeyserTextData;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.geysermc.geyser.api.pack.PathPackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.option.ResourcePackOption;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public final class GeyserPackManager implements AutoCloseable {
    private final Path packDirectory;
    private final Path dataDirectory;
    private final Path snapshotDirectory;
    private final ExtensionLogger logger;
    private final ScheduledExecutorService executor;
    private final Map<Path, PackFileStamp> observedFiles = new HashMap<>();
    private final Map<Path, Set<UUID>> packIdentifiers = new HashMap<>();
    private final Map<GeyserConnection, PackState> sessionStates = new HashMap<>();
    private final Set<GeyserPackSnapshot> activeSnapshots = new HashSet<>();
    private final Set<String> reportedErrors = new HashSet<>();
    private final Map<Path, GeyserPackStatus> packStatuses = new LinkedHashMap<>();
    private final PackState startupState;
    private PackState currentState;

    public GeyserPackManager(Path packDirectory, Path dataDirectory, Path snapshotDirectory, ExtensionLogger logger) throws IOException {
        this.packDirectory = packDirectory;
        this.dataDirectory = dataDirectory;
        this.snapshotDirectory = snapshotDirectory;
        this.logger = logger;
        this.removePreviousSnapshots();
        this.startupState = this.readStartupState();
        this.currentState = this.startupState;
        this.executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "craftengine-pack-refresh");
            thread.setDaemon(true);
            return thread;
        });
    }

    private PackState readStartupState() throws IOException {
        Map<Path, GeyserPackSnapshot> packs = new LinkedHashMap<>();
        for (Path path : this.sources()) {
            PackFileStamp before = this.stamp(path);
            GeyserPackSnapshot snapshot = GeyserPackSnapshot.read(path, BedrockPackFiles.data(path, this.dataDirectory), this.snapshotDirectory);
            if (!before.equals(this.stamp(path))) {
                snapshot.delete();
                throw new IOException("Pack changed while preparing startup registrations: " + path);
            }
            packs.put(path, snapshot);
            this.observedFiles.put(path, before);
            this.packIdentifiers.put(path, new HashSet<>(Set.of(snapshot.uuid())));
            this.activeSnapshots.add(snapshot);
        }
        return createState(packs);
    }

    public void start() {
        this.prepare();
        this.startupState.packs.forEach((path, pack) -> this.status(path, pack.uuid(), GeyserPackStatus.State.READY, ""));
        this.executor.scheduleWithFixedDelay(this::refresh, 1, 1, TimeUnit.SECONDS);
    }

    public synchronized Map<Path, GeyserPackStatus> statuses() {
        return Map.copyOf(this.packStatuses);
    }

    private synchronized void status(Path path, UUID generated, GeyserPackStatus.State state, String reason) {
        GeyserPackSnapshot served = this.currentState.packs.get(path);
        GeyserPackSnapshot registered = this.startupState.packs.get(path);
        GeyserPackStatus status = new GeyserPackStatus(generated, served == null ? null : served.uuid(),
                registered == null ? null : registered.uuid(), state, reason);
        if (!status.equals(this.packStatuses.put(path, status))) {
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
        this.startupState.packs.values().forEach(GeyserPackSnapshot::prepare);
    }

    public List<Path> registrationFiles() {
        return this.startupState.packs.values().stream().map(GeyserPackSnapshot::data).toList();
    }

    public synchronized GeyserTextData text(GeyserConnection session) {
        return this.sessionStates.getOrDefault(session, this.startupState).text;
    }

    public synchronized void load(SessionLoadResourcePacksEvent event) {
        PackState state = this.currentState;
        for (Map.Entry<Path, GeyserPackSnapshot> entry : state.packs.entrySet()) {
            Set<UUID> known = this.packIdentifiers.get(entry.getKey());
            ResourcePackOption<?>[] options = new ResourcePackOption<?>[0];
            for (ResourcePack offered : List.copyOf(event.resourcePacks())) {
                if (known.contains(offered.uuid()) || (offered.codec() instanceof PathPackCodec file && file.path().toAbsolutePath().normalize().equals(entry.getKey().toAbsolutePath().normalize()))) {
                    options = event.options(offered.uuid()).toArray(ResourcePackOption<?>[]::new);
                    event.unregister(offered.uuid());
                }
            }
            // Always offer the immutable snapshot, including the startup version. The output file may be replaced.
            event.register(entry.getValue().pack(), options);
        }
        this.sessionStates.put(event.connection(), state);
    }

    public synchronized void disconnect(GeyserConnection connection) {
        this.sessionStates.remove(connection);
    }

    public void refresh() {
        try {
            Map<Path, GeyserPackSnapshot> updated;
            synchronized (this) {
                updated = new LinkedHashMap<>(this.currentState.packs);
            }
            List<Path> sources = this.sources();
            this.reportMissingSources(sources);
            boolean changed = false;
            for (Path path : sources) {
                if (this.refreshPack(path, updated)) {
                    changed = true;
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

    private void reportMissingSources(List<Path> sources) {
        for (Path path : this.startupState.packs.keySet()) {
            if (!sources.contains(path)) {
                this.observedFiles.remove(path);
                boolean removed = !Files.isRegularFile(path);
                this.status(path, null, removed ? GeyserPackStatus.State.RESTART_REQUIRED : GeyserPackStatus.State.WAITING_FOR_PAIR, removed ? "Resource pack removed; restart Geyser to update registrations." : "Missing extension data; keeping the previous pack.");
            }
        }
    }

    private boolean refreshPack(Path path, Map<Path, GeyserPackSnapshot> updated) throws IOException {
        PackFileStamp before = this.stamp(path);
        if (before.equals(this.observedFiles.get(path))) {
            return false;
        }
        GeyserPackSnapshot next;
        try {
            next = GeyserPackSnapshot.read(path, BedrockPackFiles.data(path, this.dataDirectory), this.snapshotDirectory);
        } catch (IOException | RuntimeException exception) {
            this.status(path, null, GeyserPackStatus.State.WAITING_FOR_PAIR, exception.toString());
            return false;
        }
        boolean retained = false;
        try {
            if (!before.equals(this.stamp(path))) {
                return false;
            }
            GeyserPackSnapshot initial = this.startupState.packs.get(path);
            GeyserRegistrationChanges changes = initial == null ? null : initial.registrationChanges(next);
            if (initial == null || changes.requiresRestart()) {
                this.observedFiles.put(path, before);
                this.status(path, next.uuid(), GeyserPackStatus.State.RESTART_REQUIRED, initial == null ? "New pack; restart Geyser to register it." : changes.summary());
                return false;
            }
            updated.put(path, next);
            this.publish(path, next, updated);
            this.observedFiles.put(path, before);
            retained = true;
            this.status(path, next.uuid(), GeyserPackStatus.State.READY, "Reconnect to load this version.");
            return true;
        } catch (IOException | RuntimeException exception) {
            synchronized (this) {
                updated.clear();
                updated.putAll(this.currentState.packs);
            }
            this.status(path, next.uuid(), GeyserPackStatus.State.WAITING_FOR_PAIR, exception.toString());
            return false;
        } finally {
            if (!retained) {
                next.delete();
            }
        }
    }

    private void publish(Path path, GeyserPackSnapshot next, Map<Path, GeyserPackSnapshot> updated) throws IOException {
        next.prepare();
        // Parse all text data before publishing; a failed refresh leaves every session on the previous state.
        PackState state = createState(updated);
        synchronized (this) {
            this.activeSnapshots.add(next);
            this.packIdentifiers.get(path).add(next.uuid());
            this.currentState = state;
        }
    }

    private synchronized void retire() throws IOException {
        Set<GeyserPackSnapshot> used = new HashSet<>(this.startupState.packs.values());
        used.addAll(this.currentState.packs.values());
        for (PackState state : this.sessionStates.values()) {
            used.addAll(state.packs.values());
        }
        for (GeyserPackSnapshot snapshot : new ArrayList<>(this.activeSnapshots)) {
            if (!used.contains(snapshot)) {
                snapshot.delete();
                this.activeSnapshots.remove(snapshot);
            }
        }
    }

    private void report(String message) {
        if (this.reportedErrors.add(message)) {
            this.logger.warning(message);
        }
    }

    private List<Path> sources() throws IOException {
        if (!Files.isDirectory(this.packDirectory)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(this.packDirectory)) {
            return paths.filter(Files::isRegularFile).filter(path -> {
                String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                return (name.endsWith(".mcpack") || name.endsWith(".zip")) && Files.isRegularFile(BedrockPackFiles.data(path, this.dataDirectory));
            }).sorted().toList();
        }
    }

    private void removePreviousSnapshots() throws IOException {
        if (!Files.isDirectory(this.snapshotDirectory)) {
            return;
        }
        try (Stream<Path> paths = Files.list(this.snapshotDirectory)) {
            for (Path path : paths.filter(Files::isDirectory).filter(path -> path.getFileName().toString().startsWith("pack-")).toList()) {
                Files.deleteIfExists(path.resolve("pack.mcpack"));
                Files.deleteIfExists(path.resolve("pack.ce-data"));
                Files.delete(path);
            }
        }
    }

    private static PackState createState(Map<Path, GeyserPackSnapshot> packs) throws IOException {
        return new PackState(Collections.unmodifiableMap(new LinkedHashMap<>(packs)),
                GeyserGlyphs.read(packs.values().stream().map(GeyserPackSnapshot::data).toList()));
    }

    private PackFileStamp stamp(Path pack) throws IOException {
        BasicFileAttributes resource = Files.readAttributes(pack, BasicFileAttributes.class);
        BasicFileAttributes data = Files.readAttributes(BedrockPackFiles.data(pack, this.dataDirectory), BasicFileAttributes.class);
        return new PackFileStamp(resource.size(), resource.lastModifiedTime(), resource.fileKey(), data.size(), data.lastModifiedTime(), data.fileKey());
    }

    @Override
    public void close() {
        this.executor.shutdownNow();
    }

    private record PackState(Map<Path, GeyserPackSnapshot> packs, GeyserTextData text) {
    }

    private record PackFileStamp(long packSize, FileTime packTime, Object packKey,
                                 long dataSize, FileTime dataTime, Object dataKey) {
    }
}
