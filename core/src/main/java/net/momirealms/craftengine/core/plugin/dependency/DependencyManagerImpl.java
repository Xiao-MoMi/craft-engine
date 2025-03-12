package net.momirealms.craftengine.core.plugin.dependency;

import net.momirealms.craftengine.core.plugin.Plugin;
import net.momirealms.craftengine.core.plugin.classpath.ClassPathAppender;
import net.momirealms.craftengine.core.plugin.dependency.classloader.IsolatedClassLoader;
import net.momirealms.craftengine.core.plugin.dependency.relocation.Relocation;
import net.momirealms.craftengine.core.plugin.dependency.relocation.RelocationHandler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

public class DependencyManagerImpl implements DependencyManager {
    private final DependencyRegistry registry;
    private final Path cacheDirectory;
    private final ClassPathAppender classPathAppender;
    private final Map<Dependency, Path> loaded = Collections.synchronizedMap(new HashMap<>());
    private final Map<Set<Dependency>, IsolatedClassLoader> loaders = new HashMap<>();
    private final RelocationHandler relocationHandler;
    private final Executor loadingExecutor;
    private final Plugin plugin;

    public DependencyManagerImpl(Plugin plugin) {
        this.plugin = plugin;
        this.registry = new DependencyRegistry();
        this.cacheDirectory = setupCacheDirectory(plugin);
        this.classPathAppender = plugin.classPathAppender();
        this.loadingExecutor = plugin.scheduler().async();
        this.relocationHandler = new RelocationHandler(this);
    }

    @Override
    public ClassLoader obtainClassLoaderWith(Set<Dependency> dependencies) {
        Set<Dependency> set = new HashSet<>(dependencies);

        for (Dependency dependency : dependencies) {
            if (!this.loaded.containsKey(dependency)) {
                throw new IllegalStateException("Dependency " + dependency.id() + " is not loaded.");
            }
        }

        synchronized (this.loaders) {
            IsolatedClassLoader classLoader = this.loaders.get(set);
            if (classLoader != null) {
                return classLoader;
            }

            URL[] urls = set.stream()
                    .map(this.loaded::get)
                    .map(file -> {
                        try {
                            return file.toUri().toURL();
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toArray(URL[]::new);

            classLoader = new IsolatedClassLoader(urls);
            this.loaders.put(set, classLoader);
            return classLoader;
        }
    }

    @Override
    public void loadDependencies(Collection<Dependency> dependencies) {
        CountDownLatch latch = new CountDownLatch(dependencies.size());

        for (Dependency dependency : dependencies) {
            if (this.loaded.containsKey(dependency)) {
                latch.countDown();
                continue;
            }

            this.loadingExecutor.execute(() -> {
                try {
                    loadDependency(dependency);
                } catch (Throwable e) {
                    this.plugin.logger().warn("Unable to load dependency " + dependency.id(), e);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void loadDependency(Dependency dependency) throws Exception {
        if (this.loaded.containsKey(dependency)) {
            return;
        }

        Path file = remapDependency(dependency, downloadDependency(dependency));

        this.loaded.put(dependency, file);

        if (this.classPathAppender != null && this.registry.shouldAutoLoad(dependency)) {
            this.classPathAppender.addJarToClasspath(file);
        }
    }

    private Path downloadDependency(Dependency dependency) throws DependencyDownloadException {
        String fileName = dependency.fileName(null);
        Path file = this.cacheDirectory.resolve(fileName);
        // if the file already exists, don't attempt to re-download it.
        if (Files.exists(file)) {
            return file;
        }
        DependencyDownloadException lastError = null;
        List<DependencyRepository> repository = DependencyRepository.getByID("maven");
        if (!repository.isEmpty()) {
            int i = 0;
            while (i < repository.size()) {
                try {
                    plugin.logger().info("Downloading dependency(" + fileName + ")[" + repository.get(i).getUrl() + dependency.mavenPath() + "]");
                    repository.get(i).download(dependency, file);
                    plugin.logger().info("Successfully downloaded " + fileName);
                    return file;
                } catch (DependencyDownloadException e) {
                    lastError = e;
                    i++;
                }
            }
        }
        throw Objects.requireNonNull(lastError);
    }

    private Path remapDependency(Dependency dependency, Path normalFile) throws Exception {
        List<Relocation> rules = new ArrayList<>(dependency.relocations());
        if (rules.isEmpty()) {
            return normalFile;
        }

        Path remappedFile = this.cacheDirectory.resolve(dependency.fileName(DependencyRegistry.isGsonRelocated() ? "remapped-legacy" : "remapped"));

        // if the remapped source exists already, just use that.
        if (Files.exists(remappedFile)) {
            return remappedFile;
        }

        plugin.logger().info("Remapping " + dependency.fileName(null));
        relocationHandler.remap(normalFile, remappedFile, rules);
        plugin.logger().info("Successfully remapped " + dependency.fileName(null));
        return remappedFile;
    }

    private static Path setupCacheDirectory(Plugin plugin) {
        Path cacheDirectory = plugin.dataFolderPath().resolve("libs");
        try {
            if (Files.exists(cacheDirectory) && (Files.isDirectory(cacheDirectory) || Files.isSymbolicLink(cacheDirectory))) {
                return cacheDirectory;
            }

            try {
                Files.createDirectories(cacheDirectory);
            } catch (FileAlreadyExistsException e) {
                // ignore
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to create libs directory", e);
        }

        return cacheDirectory;
    }

    @Override
    public void close() {
        IOException firstEx = null;

        for (IsolatedClassLoader loader : this.loaders.values()) {
            try {
                loader.close();
            } catch (IOException ex) {
                if (firstEx == null) {
                    firstEx = ex;
                } else {
                    firstEx.addSuppressed(ex);
                }
            }
        }

        if (firstEx != null) {
            plugin.logger().severe(firstEx.getMessage(), firstEx);
        }
    }
}
