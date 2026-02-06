package com.yunhwan.ktypo.restdocs

import java.io.File

object SnippetWriter {

    fun write(outputDir: File, identifier: String, snippetName: String, content: String) {
        if (content.isBlank()) return
        val dir = File(outputDir, identifier)
        dir.mkdirs()
        val file = File(dir, "$snippetName.adoc")
        file.writeText(content)
    }
}
