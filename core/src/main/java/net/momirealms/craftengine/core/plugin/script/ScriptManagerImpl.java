package net.momirealms.craftengine.core.plugin.script;

import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.Plugin;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.dependency.Dependencies;
import net.momirealms.craftengine.core.plugin.dependency.Dependency;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.plugin.script.annotation.*;
import net.momirealms.craftengine.core.plugin.script.binding.LogBinding;
import net.momirealms.craftengine.core.plugin.script.binding.ScriptBinding;
import net.momirealms.craftengine.core.plugin.script.event.ScriptEventSubscriber;
import net.momirealms.craftengine.core.plugin.script.placeholder.ScriptPlaceholderManager;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class ScriptManagerImpl implements ScriptManager {
    private static final List<Dependency> GRAALJS_DEPENDENCIES = List.of(
            Dependencies.GRAALJS_POLYGLOT,
            Dependencies.GRAALJS_JS_LANGUAGE,
            Dependencies.GRAALJS_TRUFFLE_RUNTIME,
            Dependencies.GRAALJS_TRUFFLE_COMPILER,
            Dependencies.GRAALJS_TRUFFLE_API,
            Dependencies.GRAALJS_REGEX,
            Dependencies.GRAALJS_COLLECTIONS,
            Dependencies.GRAALJS_NATIVEIMAGE,
            Dependencies.GRAALJS_ICU4J
    );
    private static final List<Dependency> NASHORN_DEPENDENCIES = List.of(
            Dependencies.NASHORN_CORE,
            Dependencies.ASM_UTIL
    );
    private final Plugin plugin;
    private final Map<String, ScriptBinding> bindings = new LinkedHashMap<>();
    private final Map<String, ScriptAnnotationHandler> annotationHandlers = new LinkedHashMap<>();
    private final Map<Key, ScriptFile> scripts = new ConcurrentHashMap<>();
    private final Map<String, ScriptFile> scriptsByName = new ConcurrentHashMap<>();
    private final List<PendingEnable> pendingEnables = new ArrayList<>();
    private JsEngine engine;
    private ScriptEventSubscriber eventSubscriber;
    private ScriptPlaceholderManager placeholderManager;
    private boolean available;

    public ScriptManagerImpl(Plugin plugin) {
        this.plugin = plugin;
        registerBinding(new LogBinding(plugin));
        registerAnnotationHandler(new SubscribeAnnotationHandler(this));
        registerAnnotationHandler(new EnableAnnotationHandler(this));
        registerAnnotationHandler(new DisableAnnotationHandler());
        registerAnnotationHandler(new PlaceholderAnnotationHandler(this));
        registerAnnotationHandler(new RelationalPlaceholderAnnotationHandler(this));
        registerAnnotationHandler(new TaskAnnotationHandler(this));
        if (!Config.enableJsScripting()) {
            this.available = false;
            return;
        }
        String engineName = Config.jsEngine().toLowerCase(java.util.Locale.ENGLISH);
        try {
            if (engineName.equalsIgnoreCase("nashorn")) {
                this.plugin.dependencyManager().loadDependencies(NASHORN_DEPENDENCIES);
                this.engine = new NashornScriptEngine();
            } else if (engineName.equalsIgnoreCase("graaljs")) {
                this.plugin.dependencyManager().loadDependencies(GRAALJS_DEPENDENCIES);
                this.engine = new GraalScriptEngine();
            } else {
                this.available = false;
                this.plugin.logger().warn("JS engine " + engineName + " not found.");
                return;
            }
            this.available = true;
            this.plugin.logger().info("JS engine: " + engineName);
        } catch (Throwable t) {
            this.plugin.logger().warn("JS engine '" + engineName + "' is unavailable, js scripting is disabled", t);
            this.available = false;
        }
    }

    public Plugin plugin() {
        return this.plugin;
    }

    Map<String, ScriptBinding> bindings() {
        return this.bindings;
    }

    @Override
    public boolean isAvailable() {
        return this.available;
    }

    @Override
    public void registerBinding(ScriptBinding binding) {
        this.bindings.put(binding.name(), binding);
    }

    public void registerAnnotationHandler(ScriptAnnotationHandler handler) {
        this.annotationHandlers.put(handler.name(), handler);
    }

    @Override
    public Optional<ScriptFile> script(String id) {
        if (id.indexOf(':') != -1) {
            Key key = Key.of(id);
            if (this.scripts.containsKey(key)) {
                return Optional.of(this.scripts.get(key));
            }
        }
        return Optional.ofNullable(this.scriptsByName.get(id));
    }

    @Override
    public Optional<ScriptFile> script(Key id) {
        return Optional.ofNullable(this.scripts.get(id));
    }

    @Override
    public void load() {
        if (!this.available) return;
        long t1 = System.nanoTime();
        for (Pack pack : this.plugin.packManager().loadedPacks()) {
            if (!pack.enabled()) continue;
            for (Path scriptDir : pack.scriptFolders()) {
                try {
                    scanScripts(scriptDir, pack.namespace());
                } catch (Throwable t) {
                    this.plugin.logger().warn("Failed to scan scripts of pack '" + pack.name() + "' (" + scriptDir + ")", t);
                }
            }
        }
        this.scripts.values().forEach(ScriptFile::warmUp);
        if (!this.scripts.isEmpty()) {
            long t2 = System.nanoTime();
            TranslationManager.info("resource.config_loaded", "scripts", String.format("%.2f", ((t2 - t1) / 1_000_000.0)), String.valueOf(this.scripts.size()));
        }
        // 全部脚本加载完成后统一触发 //@Enable
        for (PendingEnable pending : this.pendingEnables) {
            try {
                pending.script().invoke(pending.function(), Map.of());
            } catch (Throwable t) {
                this.plugin.logger().warn("Error running //@Enable function '" + pending.function() + "' of script '" + pending.script().id() + "'", t);
            }
        }
        this.pendingEnables.clear();
        this.scripts.values().forEach(ScriptFile::startAutoTasks);
    }

    @Override
    public void unload() {
        if (!this.available) return;
        unloadScripts();
    }

    @Override
    public void disable() {
        unloadScripts();
        if (this.engine != null) {
            this.engine.close();
            this.engine = null;
        }
    }

    public void addPendingEnable(ScriptFile script, String function) {
        this.pendingEnables.add(new PendingEnable(script, function));
    }

    public void setEventSubscriber(ScriptEventSubscriber eventSubscriber) {
        this.eventSubscriber = eventSubscriber;
    }

    public void setPlaceholderManager(ScriptPlaceholderManager placeholderManager) {
        this.placeholderManager = placeholderManager;
    }

    @Override
    public ScriptPlaceholderManager placeholderManager() {
        return this.placeholderManager;
    }

    public void registerPlaceholder(ScriptFile script, String placeholder, String function, boolean relational) {
        if (this.placeholderManager == null) {
            this.plugin.logger().warn("Script '" + script.id() + "' tried to register placeholder '" + placeholder + "', but placeholder registration is not supported on this platform");
            return;
        }
        this.placeholderManager.register(script, placeholder, function, relational);
    }

    public void subscribeEvent(ScriptFile script, String eventClass, String function, @Nullable Map<String, Object> options) {
        if (this.eventSubscriber == null) {
            this.plugin.logger().warn("Script '" + script.id() + "' tried to subscribe event '" + eventClass + "', but event subscription is not supported on this platform");
            return;
        }
        this.eventSubscriber.subscribe(script, eventClass, function, options);
    }

    private void scanScripts(Path directory, String namespace) {
        if (!Files.isDirectory(directory)) return;
        try (Stream<Path> stream = Files.walk(directory)) {
            stream.filter(Files::isRegularFile)
                    .filter(it -> it.getFileName().toString().endsWith(".js"))
                    .sorted()
                    .forEach(path -> {
                        String replaced = directory.relativize(path).toString().replace('\\', '/');
                        String id = replaced.substring(0, replaced.length() - 3);
                        if (!id.isBlank()) {
                            loadScript(path, Key.of(namespace, id));
                        }
                    });
        } catch (IOException | RuntimeException e) {
            this.plugin.logger().warn("Failed to scan scripts directory " + directory, e);
        }
    }

    private void loadScript(Path path, Key id) {
        if (this.scripts.containsKey(id)) {
            this.plugin.logger().warn("Duplicated script id '" + id + "' (" + path + "), skipped");
            return;
        }
        try {
            ScriptFile script = new ScriptFile(this, this.engine, id, Files.readAllBytes(path));
            this.scripts.put(id, script);
            this.scriptsByName.put(id.value(), script);
            for (AnnotatedFunction annotated : script.annotatedFunctions()) {
                for (ScriptAnnotation annotation : annotated.annotations()) {
                    ScriptAnnotationHandler handler = this.annotationHandlers.get(annotation.name());
                    if (handler != null) {
                        try {
                            handler.handle(script, annotated.function(), annotation);
                        } catch (Throwable t) {
                            this.plugin.logger().warn("Error handling annotation '@" + annotation.name() + "' on function '" + annotated.function() + "' of script '" + id + "'", t);
                        }
                    } else {
                        this.plugin.logger().warn("Script '" + id + "' uses unknown annotation '@" + annotation.name() + "' on function '" + annotated.function() + "'");
                    }
                }
            }
        } catch (Throwable t) {
            this.plugin.logger().warn("Failed to load script '" + id + "'", t);
        }
    }

    private void unloadScripts() {
        this.scripts.values().forEach(ScriptFile::unload);
        this.scripts.clear();
        this.scriptsByName.clear();
        this.pendingEnables.clear();
    }

    @Nullable
    @Override
    public Object invoke(String id, String function, Map<String, Object> injected) {
        if (!this.available) return null;
        Optional<ScriptFile> script = this.script(id);
        if (script.isEmpty()) {
            this.plugin.logger().warn("Script '" + id + "' not found");
            return null;
        }
        return script.get().invoke(function, injected);
    }

    @Override
    public boolean test(String id, String function, Map<String, Object> injected, boolean def) {
        Object result = invoke(id, function, injected);
        if (result instanceof Boolean bool) {
            return bool;
        }
        return result != null || def;
    }

    private record PendingEnable(ScriptFile script, String function) {
    }
}
