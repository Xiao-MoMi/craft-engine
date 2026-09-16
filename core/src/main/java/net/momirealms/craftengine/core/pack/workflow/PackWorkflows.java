package net.momirealms.craftengine.core.pack.workflow;

import net.momirealms.craftengine.core.pack.workflow.impl.*;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

public final class PackWorkflows {
    public static final PackWorkflowType<GeneratePackWorkflow> GENERATE = register(Key.ce("generate"), GeneratePackWorkflow.FACTORY);
    public static final PackWorkflowType<LoadZipPackWorkflow> LOAD_ZIP = register(Key.ce("load_zip"), LoadZipPackWorkflow.FACTORY);
    public static final PackWorkflowType<ExportPackWorkflow> EXPORT = register(Key.ce("export"), ExportPackWorkflow.FACTORY);
    public static final PackWorkflowType<PackSquashWorkflow> PACKSQUASH = register(Key.ce("packsquash"), PackSquashWorkflow.FACTORY);
    public static final PackWorkflowType<ValidatePackWorkflow> VALIDATE = register(Key.ce("validate"), ValidatePackWorkflow.FACTORY);
    public static final PackWorkflowType<OptimizePackWorkflow> OPTIMIZE = register(Key.ce("optimize"), OptimizePackWorkflow.FACTORY);
    public static final PackWorkflowType<ZipPackWorkflow> ZIP = register(Key.ce("zip"), ZipPackWorkflow.FACTORY);
    public static final PackWorkflowType<UploadPackWorkflow> UPLOAD = register(Key.ce("upload"), UploadPackWorkflow.FACTORY);
    public static final PackWorkflowType<SendPackWorkflow> SEND_PACK = register(Key.ce("send_pack"), SendPackWorkflow.FACTORY);

    private PackWorkflows() {}

    public static <T extends PackWorkflow> PackWorkflowType<T> register(Key key, PackWorkflowFactory<T> factory) {
        PackWorkflowType<T> type = new PackWorkflowType<>(key, factory);
        ((WritableRegistry<PackWorkflowType<? extends PackWorkflow>>) BuiltInRegistries.PACK_WORKFLOW_TYPE)
                .register(ResourceKey.create(Registries.PACK_WORKFLOW_TYPE.location(), key), type);
        return type;
    }

    public static PackWorkflow fromConfig(ConfigSection section) {
        Key key = Key.ce(section.getNonEmptyString("type"));
        PackWorkflowType<? extends PackWorkflow> type = BuiltInRegistries.PACK_WORKFLOW_TYPE.getValue(key);
        if (type == null) {
            throw new KnownResourceException("workflow.unknown_type", section.assemblePath("type"), key.asString());
        }
        return type.factory().create(section);
    }
}
