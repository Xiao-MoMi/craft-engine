abstract class GenerateScopedResourceIndexTask : DefaultTask() {

    @get:InputDirectory
    abstract val sourceResources: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun execute() {
        val srcRoot = sourceResources.get().asFile
        val outRoot = outputDir.get().asFile

        outRoot.deleteRecursively()
        outRoot.mkdirs()

        val targets = listOf("resources", "translations")
            .map { File(srcRoot, it) }
            .filter { it.exists() && it.isDirectory }

        targets.forEach { targetRoot ->
            targetRoot.walkBottomUp().filter { it.isDirectory }.forEach { srcDir ->

                val relativePath = srcDir.relativeTo(srcRoot).path
                val targetDir = File(outRoot, relativePath)
                targetDir.mkdirs()

                val children = srcDir.listFiles() ?: arrayOf<File>()
                val directoryNames = children.filter { it.isDirectory }.map { it.name }.sorted()

                val fileNames = children.filter {
                    it.isFile && !it.name.startsWith(".") && it.name != "_index.json"
                }.map { it.name }.sorted()

                val jsonContent = """
                    {
                      "directory": ${directoryNames.toJsonArray()},
                      "file": ${fileNames.toJsonArray()}
                    }
                """.trimIndent()

                File(targetDir, "_index.json").writeText(jsonContent)
            }
        }
    }

    private fun List<String>.toJsonArray() =
        if (isEmpty()) "[]"
        else joinToString("\", \"", "[\"", "\"]")
}

val generateScopedIndex = tasks.register<GenerateScopedResourceIndexTask>("generateScopedResourceIndex") {
    sourceResources.set(layout.projectDirectory.dir("src/main/resources"))
    outputDir.set(layout.buildDirectory.dir("generated/resource-indexes"))
}

sourceSets {
    main {
        resources {
            srcDir(generateScopedIndex.map { it.outputDir })
        }
    }
}