package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.locale.MessageConstants; // Make sure this is imported
import net.momirealms.craftengine.core.util.AdventureHelper; // Make sure this is imported
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class DebugExportPackTreeCommand extends BukkitCommandFeature<CommandSender> {

    public DebugExportPackTreeCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .required("pack", StringParser.stringComponent(StringParser.StringMode.GREEDY).suggestionProvider(new SuggestionProvider<>() {
                    @Override
                    public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(@NonNull CommandContext<Object> context, @NonNull CommandInput input) {
                        Path resourcesFolder = plugin().dataFolderPath().resolve("resources");
                        return CompletableFuture.completedFuture(plugin().packManager().loadedPacks().stream()
                                .map(pack -> {
                                    try {
                                        return Suggestion.suggestion(resourcesFolder.relativize(pack.folder()).toString().replace("\\", "/"));
                                    } catch (IllegalArgumentException e) {
                                        return Suggestion.suggestion(pack.folder().getFileName().toString());
                                    }
                                })
                                .collect(Collectors.toList()));
                    }
                }))
                .handler(context -> {
                    String packRelativePath = context.get("pack");
                    Path packRootPath = plugin().dataFolderPath().resolve("resources").resolve(packRelativePath);

                    if (!Files.exists(packRootPath)) {
                        plugin().logger().severe("Pack folder does not exist: " + packRelativePath);
                        handleFeedback(context, MessageConstants.COMMAND_EXPORT_PACK_TREE_FAILURE, Component.text(packRelativePath));
                        return;
                    }
                    if (!Files.isDirectory(packRootPath)) {
                        plugin().logger().severe("Path is not a directory: " + packRelativePath);
                        handleFeedback(context, MessageConstants.COMMAND_RESOURCE_DISABLE_FAILURE, Component.text(packRelativePath));
                        return;
                    }

                    Path exportFolder = plugin().dataFolderPath().resolve("tree_exports");
                    try {
                        Files.createDirectories(exportFolder);
                        String timestamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm").format(new Date());

                        String sanitizedPackName = packRelativePath.replace("/", "_").replace("\\", "_");
                        Path outputFile = exportFolder.resolve(sanitizedPackName + "_" + timestamp + ".html");

                        StringBuilder htmlContent = new StringBuilder();
                        long[] totalSize = {0};

                        String treeHtml = buildHtmlTreeAndCalculateSize(packRootPath, totalSize, 0, packRootPath);

                        buildHtmlHeader(htmlContent, packRelativePath, totalSize[0]);
                        htmlContent.append(treeHtml);
                        buildHtmlFooter(htmlContent);

                        Files.writeString(outputFile, htmlContent.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                        plugin().logger().info("Successfully exported pack resources for '" + packRelativePath + "' to " + outputFile.toAbsolutePath().toString().replace("\\", "/"));
                        handleFeedback(context, MessageConstants.COMMAND_EXPORT_PACK_TREE_SUCCESS, Component.text(packRelativePath), Component.text(outputFile.getFileName().toString()));
                    } catch (IOException e) {
                        plugin().logger().severe("Error exporting pack tree for " + packRelativePath, e);
                        handleFeedback(context, MessageConstants.COMMAND_EXPORT_PACK_TREE_FAILURE, Component.text(packRelativePath));
                    }
                });
    }

    @Override
    public String getFeatureID() {
        return "export_pack_tree";
    }


    private void buildHtmlHeader(StringBuilder htmlContent, String packName, long totalSize) {
        htmlContent.append("<!DOCTYPE html>\n");
        htmlContent.append("<html lang=\"en\">\n");
        htmlContent.append("<head>\n");
        htmlContent.append("    <meta charset=\"UTF-8\">\n");
        htmlContent.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        htmlContent.append("    <title>Pack Tree: ").append(packName).append("</title>\n");
        htmlContent.append("    <style>\n");
        htmlContent.append("        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #20232A; color: #DCDCDC; margin: 20px; line-height: 1.5; }\n");
        htmlContent.append("        .container { background-color: #282C34; padding: 25px; border-radius: 10px; max-width: 1200px; margin: 30px auto; box-shadow: 0 8px 16px rgba(0,0,0,0.4); }\n");
        htmlContent.append("        h1 { color: #61DAFB; border-bottom: 2px solid #61DAFB; padding-bottom: 12px; margin-bottom: 25px; font-size: 2.2em; font-weight: 600; }\n");
        htmlContent.append("        .total-size { color: #A5D6A7; font-size: 1.15em; margin-bottom: 20px; display: block; font-weight: 500; }\n");
        htmlContent.append("        ul { list-style-type: none; padding-left: 0; margin-top: 0; }\n");
        htmlContent.append("        li { margin: 0; position: relative; padding-left: 25px; }\n"); /* Increased padding for lines */

        htmlContent.append("        li:before { content: ''; position: absolute; left: 0; top: 0.8em; width: 15px; height: 1px; background-color: #666; }\n"); /* Horizontal line */
        htmlContent.append("        ul li:not(:last-child):after { content: ''; position: absolute; left: 0; top: 0; bottom: -0.8em; width: 1px; background-color: #666; }\n"); /* Vertical line */
        htmlContent.append("        ul li:last-child:after { content: ''; position: absolute; left: 0; top: 0; bottom: calc(0.8em + 2px); width: 1px; background-color: #666; }\n"); /* Vertical line for last child in a block, stopping at parent line */

        htmlContent.append("        .folder { color: #FFD700; cursor: pointer; display: block; padding: 4px 0; }\n");
        htmlContent.append("        .folder:before { content: '\\25B6\\0020'; color: #78909C; display: inline-block; width: 1.2em; transition: transform 0.15s ease-out; }\n");
        htmlContent.append("        .folder.expanded:before { content: '\\25BC\\0020'; transform: rotate(0deg); }\n"); // Rotate reset for BC triangle
        htmlContent.append("        .file { color: #BBBBBB; display: inline-block; padding: 4px 0; }\n");
        htmlContent.append("        .file-size { color: #9E9E9E; font-size: 0.85em; margin-left: 8px; }\n");
        htmlContent.append("        .path-info { font-family: 'Consolas', 'Monaco', monospace; background-color: #3A3F49; color: #A9B7C6; padding: 3px 8px; border-radius: 5px; font-size: 0.75em; cursor: copy; margin-left: 15px; border: 1px solid #4D535C; transition: background-color 0.2s; }\n");
        htmlContent.append("        .path-info:hover { background-color: #4A505D; }\n");
        htmlContent.append("    </style>\n");
        htmlContent.append("</head>\n");
        htmlContent.append("<body>\n");
        htmlContent.append("    <div class=\"container\">\n");
        htmlContent.append("        <h1>Pack Tree: ").append(packName).append("</h1>\n");
        htmlContent.append("        <span class=\"total-size\">Total Size: ").append(formatFileSize(totalSize)).append("</span>\n");
        htmlContent.append("        <ul>\n");
    }

    private String buildHtmlTreeAndCalculateSize(Path directory, long[] totalSize, int depth, Path basePackPath) throws IOException {
        StringBuilder currentLevelHtml = new StringBuilder();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            List<Path> sortedPaths = new ArrayList<>();
            for (Path entry : stream) {
                sortedPaths.add(entry);
            }

            sortedPaths.sort((p1, p2) -> {
                boolean isDir1 = Files.isDirectory(p1);
                boolean isDir2 = Files.isDirectory(p2);
                int dirComparison = Boolean.compare(isDir2, isDir1);

                if (dirComparison != 0) {
                    return dirComparison;
                }

                return p1.toString().toLowerCase().compareTo(p2.toString().toLowerCase());
            });

            for (Path entry : sortedPaths) {
                String fileName = entry.getFileName().toString();
                String relativePath = basePackPath.relativize(entry).toString().replace("\\", "/");

                if (Files.isDirectory(entry)) {
                    currentLevelHtml.append("<li><span class=\"folder\" onclick=\"toggleFolder(this)\">").append(fileName).append("</span>\n");
                    currentLevelHtml.append("<ul>\n");

                    currentLevelHtml.append(buildHtmlTreeAndCalculateSize(entry, totalSize, depth + 1, basePackPath));
                    currentLevelHtml.append("</ul></li>\n");
                } else {
                    long fileSize = Files.size(entry);
                    totalSize[0] += fileSize;
                    String fileSizeStr = formatFileSize(fileSize);
                    currentLevelHtml.append("<li><span class=\"file\">").append(fileName).append("</span> <span class=\"file-size\">(").append(fileSizeStr).append(")</span> <span class=\"path-info\" onclick=\"copyPath(this)\" title=\"Click to copy path\">").append(relativePath).append("</span></li>\n");
                }
            }
        }
        return currentLevelHtml.toString();
    }

    private void buildHtmlFooter(StringBuilder htmlContent) {
        htmlContent.append("        </ul>\n");
        htmlContent.append("    </div>\n");
        htmlContent.append("    <script>\n");
        htmlContent.append("        function toggleFolder(element) {\n");
        htmlContent.append("            element.classList.toggle('expanded');\n");
        htmlContent.append("            var ul = element.nextElementSibling;\n");
        htmlContent.append("            if (ul) {\n");
        htmlContent.append("                ul.style.display = ul.style.display === 'none' ? 'block' : 'none';\n");
        htmlContent.append("            }\n");
        htmlContent.append("        }\n");
        htmlContent.append("        // Initially hide all sub-folders except the top level\n");
        htmlContent.append("        document.querySelectorAll('.folder').forEach(function(folder) {\n");
        htmlContent.append("            var ul = folder.nextElementSibling;\n");
        htmlContent.append("            // Only hide if it's not the root-level UL (the UL directly under h1)\n");
        htmlContent.append("            // This check ensures only nested folders are collapsed by default\n");
        htmlContent.append("            if (ul && folder.closest('ul').parentNode.tagName === 'LI') { \n");
        htmlContent.append("                ul.style.display = 'none';\n");
        htmlContent.append("            }\n");
        htmlContent.append("        });\n");
        htmlContent.append("        function copyPath(element) {\n");
        htmlContent.append("            var path = element.textContent;\n");
        htmlContent.append("            navigator.clipboard.writeText(path).then(function() {\n");
        htmlContent.append("                element.style.backgroundColor = '#4CAF50'; // Green for success\n");
        htmlContent.append("                setTimeout(() => element.style.backgroundColor = '#3A3F49', 1000); // Revert after 1s\n");
        htmlContent.append("            }).catch(function(err) {\n");
        htmlContent.append("                console.error('Could not copy text: ', err);\n");
        htmlContent.append("            });\n");
        htmlContent.append("        }\n");
        htmlContent.append("    </script>\n");
        htmlContent.append("</body>\n");
        htmlContent.append("</html>\n");
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = ("KMGTPE").charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}