package com.yunhwan.ktypo.openapi

import com.yunhwan.ktypo.model.DocumentModel
import com.yunhwan.ktypo.model.HttpMethod
import com.yunhwan.ktypo.model.ParameterLocation
import com.yunhwan.ktypo.schema.SchemaRegistry
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses

class OpenApiGenerator(
    private val title: String,
    private val version: String,
    private val description: String?,
    private val registry: SchemaRegistry,
) {
    fun generate(documents: List<DocumentModel>): OpenAPI {
        val openApi = OpenAPI().apply {
            openapi = "3.1.0"
            info = Info().apply {
                title = this@OpenApiGenerator.title
                version = this@OpenApiGenerator.version
                this@OpenApiGenerator.description?.let { description = it }
            }
        }

        val paths = Paths()
        for (doc in documents) {
            val op = doc.operation
            val pathItem = paths.getOrDefault(op.path, PathItem())

            val operation = buildOperation(doc)

            when (op.method) {
                HttpMethod.GET -> pathItem.get = operation
                HttpMethod.POST -> pathItem.post = operation
                HttpMethod.PUT -> pathItem.put = operation
                HttpMethod.DELETE -> pathItem.delete = operation
                HttpMethod.PATCH -> pathItem.patch = operation
            }

            paths.addPathItem(op.path, pathItem)
        }
        openApi.paths = paths

        // Add component schemas
        val allSchemas = registry.allSchemas()
        if (allSchemas.isNotEmpty()) {
            val components = Components()
            for ((name, schema) in allSchemas) {
                components.addSchemas(name, OpenApiConverter.convert(schema))
            }
            openApi.components = components
        }

        return openApi
    }

    private fun buildOperation(doc: DocumentModel): Operation {
        val op = doc.operation
        val operation = Operation().apply {
            op.summary?.let { summary = it }
            op.description?.let { description = it }
            if (op.tags.isNotEmpty()) tags = op.tags
            op.operationId?.let { operationId = it }
            if (op.deprecated) deprecated = true
        }

        // Parameters
        if (op.parameters.isNotEmpty()) {
            operation.parameters = op.parameters.map { param ->
                Parameter().apply {
                    name = param.name
                    `in` = param.location.value
                    param.description?.let { description = it }
                    required = param.required
                    param.schema?.let { schema = OpenApiConverter.convert(it) }
                    param.example?.let { example = it }
                }
            }
        }

        // Request body
        op.request?.let { req ->
            operation.requestBody = RequestBody().apply {
                content = Content().apply {
                    addMediaType(req.contentType, MediaType().apply {
                        schema = OpenApiConverter.convert(req.schema)
                    })
                }
                required = true
            }
        }

        // Responses
        val apiResponses = ApiResponses()
        if (op.responses.isEmpty()) {
            apiResponses.addApiResponse("200", ApiResponse().apply {
                description = "OK"
            })
        } else {
            for (resp in op.responses) {
                apiResponses.addApiResponse(resp.statusCode.toString(), ApiResponse().apply {
                    description = resp.description ?: "OK"
                    content = Content().apply {
                        addMediaType(resp.contentType, MediaType().apply {
                            schema = OpenApiConverter.convert(resp.schema)
                        })
                    }
                })
            }
        }
        operation.responses = apiResponses

        return operation
    }
}
