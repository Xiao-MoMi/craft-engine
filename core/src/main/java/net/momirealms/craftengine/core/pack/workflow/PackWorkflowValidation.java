package net.momirealms.craftengine.core.pack.workflow;

import net.momirealms.craftengine.core.pack.host.ResourcePackHost;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.nio.file.Path;
import java.util.Map;

public final class PackWorkflowValidation {
    private final Path directory;
    private final Map<String, ResourcePackHost> hosts;
    private boolean generated;
    private boolean packed;
    private boolean protection;

    public PackWorkflowValidation(Path directory, Map<String, ResourcePackHost> hosts) {
        this.directory = directory;
        this.hosts = hosts;
    }

    public Path resolvePath(String path) {
        return this.directory.resolve(path).toAbsolutePath().normalize();
    }

    public void generate() {
        if (this.generated) throw new IllegalArgumentException("A workflow cannot generate more than one resource pack");
        this.generated = true;
    }

    public void requireGeneratedPack() {
        if (!this.generated || this.packed) throw new IllegalArgumentException("This workflow requires generated assets before ZIP packaging");
    }

    public void loadZip(String path) {
        resolvePath(path);
        // 读取已有 ZIP 后重新进入可处理资源的阶段，允许继续校验、优化和打包。
        this.generated = true;
        this.packed = false;
    }

    public void export(String path) {
        requireGeneratedPack();
        Path output = resolvePath(path);
        // 导出会重建目标目录，不能将插件数据目录或其父目录当作导出目录。
        if (this.directory.toAbsolutePath().normalize().startsWith(output)) {
            throw new IllegalArgumentException("Cannot export a resource pack over the plugin data directory: " + output);
        }
    }

    public void zip(String path, boolean protection) {
        requireGeneratedPack();
        this.packed = true;
        this.protection = protection && VersionHelper.PREMIUM;
        resolvePath(path);
    }

    public ResourcePackHost requireHost(String pack) {
        ResourcePackHost host = this.hosts.get(pack);
        if (host == null) throw new IllegalArgumentException("Unknown resource pack: " + pack);
        return host;
    }

    public void upload(String pack) {
        if (!requireHost(pack).canUpload()) {
            throw new IllegalArgumentException("Host does not support uploads: " + pack);
        }
    }

    public boolean usesProtection() {
        return this.protection;
    }
}
