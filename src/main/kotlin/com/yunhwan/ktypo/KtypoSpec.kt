package com.yunhwan.ktypo

import com.yunhwan.ktypo.config.KtypoConfig
import com.yunhwan.ktypo.model.DocumentModel
import com.yunhwan.ktypo.openapi.OpenApiGenerator
import com.yunhwan.ktypo.openapi.OpenApiWriter
import com.yunhwan.ktypo.restdocs.RestDocsGenerator
import com.yunhwan.ktypo.schema.SchemaRegistry
import io.swagger.v3.oas.models.OpenAPI

class KtypoSpec(
    val title: String,
    val version: String,
    val description: String?,
    val documents: List<DocumentModel>,
    val config: KtypoConfig,
    val registry: SchemaRegistry,
) {
    fun generate() {
        val openApi = generateOpenApi()

        when (config.format) {
            KtypoConfig.OutputFormat.JSON -> OpenApiWriter.writeJson(openApi, config.outputDir)
            KtypoConfig.OutputFormat.YAML -> OpenApiWriter.writeYaml(openApi, config.outputDir)
            KtypoConfig.OutputFormat.BOTH -> {
                OpenApiWriter.writeJson(openApi, config.outputDir)
                OpenApiWriter.writeYaml(openApi, config.outputDir)
            }
        }

        if (config.generateRestDocs) {
            generateRestDocs()
        }
    }

    fun generateOpenApi(): OpenAPI {
        val generator = OpenApiGenerator(
            title = title,
            version = version,
            description = description,
            registry = registry,
        )
        return generator.generate(documents)
    }

    fun toJson(): String = OpenApiWriter.toJson(generateOpenApi())

    fun toYaml(): String = OpenApiWriter.toYaml(generateOpenApi())

    private fun generateRestDocs() {
        val generator = RestDocsGenerator(registry)
        generator.generate(documents, config.snippetDir)
    }
}
