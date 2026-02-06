package com.yunhwan.ktypo.config

import java.io.File

data class KtypoConfig(
    val outputDir: File = File("build/generated-docs"),
    val snippetDir: File = File("build/generated-snippets"),
    val format: OutputFormat = OutputFormat.BOTH,
    val generateRestDocs: Boolean = true,
) {
    enum class OutputFormat {
        JSON, YAML, BOTH
    }

    class Builder {
        private var outputDir: File = File("build/generated-docs")
        private var snippetDir: File = File("build/generated-snippets")
        private var format: OutputFormat = OutputFormat.BOTH
        private var generateRestDocs: Boolean = true

        fun outputDir(path: String) { outputDir = File(path) }
        fun outputDir(file: File) { outputDir = file }
        fun snippetDir(path: String) { snippetDir = File(path) }
        fun snippetDir(file: File) { snippetDir = file }
        fun format(value: OutputFormat) { format = value }
        fun generateRestDocs(value: Boolean) { generateRestDocs = value }

        fun build(): KtypoConfig = KtypoConfig(
            outputDir = outputDir,
            snippetDir = snippetDir,
            format = format,
            generateRestDocs = generateRestDocs,
        )
    }
}
