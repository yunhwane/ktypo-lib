package com.yunhwan.ktypo.openapi

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.swagger.v3.oas.models.OpenAPI
import java.io.File

object OpenApiWriter {

    private val jsonMapper = ObjectMapper()
        .registerKotlinModule()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .enable(SerializationFeature.INDENT_OUTPUT)

    private val yamlMapper = ObjectMapper(
        YAMLFactory()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
    )
        .registerKotlinModule()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)

    fun writeJson(openApi: OpenAPI, outputDir: File) {
        outputDir.mkdirs()
        val file = File(outputDir, "openapi.json")
        jsonMapper.writeValue(file, openApi)
    }

    fun writeYaml(openApi: OpenAPI, outputDir: File) {
        outputDir.mkdirs()
        val file = File(outputDir, "openapi.yaml")
        yamlMapper.writeValue(file, openApi)
    }

    fun toJson(openApi: OpenAPI): String =
        jsonMapper.writeValueAsString(openApi)

    fun toYaml(openApi: OpenAPI): String =
        yamlMapper.writeValueAsString(openApi)
}
