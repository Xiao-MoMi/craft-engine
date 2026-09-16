package net.momirealms.craftengine.core.pack.workflow.impl;

import net.momirealms.craftengine.core.pack.workflow.*;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.Timestamp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public record PackSquashWorkflow(String executable, String config, int timeout) implements PackWorkflow {
    public static final PackWorkflowFactory<PackSquashWorkflow> FACTORY = section -> new PackSquashWorkflow(
            section.getString("executable", "packsquash"),
            section.getNonEmptyString("config"),
            section.getValue("timeout", value -> value.getAsInt(1), 1800)
    );

    @Override
    public PackWorkflowType<PackSquashWorkflow> type() {
        return PackWorkflows.PACKSQUASH;
    }

    @Override
    public void validate(PackWorkflowValidation validation) {
        validation.resolvePath(this.config);
        if (this.executable.isBlank()) throw new IllegalArgumentException("PackSquash executable cannot be empty");
        if (this.timeout <= 0) throw new IllegalArgumentException("PackSquash timeout must be positive");
    }

    @Override
    public void execute(PackWorkflowContext context) throws IOException {
        Path configuration = context.resolvePath(this.config);
        if (!Files.isRegularFile(configuration)) throw new IOException("PackSquash configuration does not exist: " + configuration);
        // 裸命令名从 PATH 查找，带目录的程序路径与其他工作流路径一样，相对于插件数据目录。
        String command = this.executable.contains("/") || this.executable.contains("\\")
                ? context.resolvePath(this.executable).toString() : this.executable;
        ProcessBuilder builder = new ProcessBuilder(command, configuration.toString())
                .directory(context.resolvePath(".").toFile());
        context.plugin().logger().info(TranslationManager.instance().plainTranslation("resource_pack.packsquash_started", configuration.toString()));
        Timestamp timestamp = new Timestamp();
        run(builder, this.timeout);
        context.plugin().logger().info(TranslationManager.instance().plainTranslation("resource_pack.packsquash_finished", String.valueOf(timestamp.deltaMillis())));
    }

    static void run(ProcessBuilder builder, long timeout) throws IOException {
        // 不通过 shell 拼接命令，配置路径中的空格不会被拆成多个参数。
        // 将输出直接交给控制台，避免 PackSquash 的进度输出填满管道、卡住工作流。
        Process process = builder.redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.INHERIT).start();
        try {
            process.getOutputStream().close();
            if (!process.waitFor(timeout, TimeUnit.SECONDS)) throw new IOException("PackSquash timed out after " + TimeUnit.SECONDS.toSeconds(timeout) + " seconds");
            int exitCode = process.exitValue();
            if (exitCode != 0) throw new IOException("PackSquash exited with code " + exitCode + ". Check the console output");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("PackSquash was interrupted", e);
        } finally {
            // 工作流被中断时也要终止外部进程，防止后台继续写入资源包。
            if (process.isAlive()) process.destroyForcibly();
        }
    }
}
