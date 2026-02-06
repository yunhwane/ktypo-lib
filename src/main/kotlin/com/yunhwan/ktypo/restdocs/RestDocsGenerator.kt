package com.yunhwan.ktypo.restdocs

import com.yunhwan.ktypo.model.DocumentModel
import com.yunhwan.ktypo.model.ParameterLocation
import com.yunhwan.ktypo.schema.SchemaRegistry
import java.io.File

class RestDocsGenerator(
    private val registry: SchemaRegistry,
) {
    private val flattener = FieldFlattener(registry)

    fun generate(documents: List<DocumentModel>, outputDir: File) {
        for (doc in documents) {
            generateDocument(doc, outputDir)
        }
    }

    private fun generateDocument(doc: DocumentModel, outputDir: File) {
        val op = doc.operation

        // Request fields
        op.request?.let { req ->
            val fields = flattener.flatten(req.schema, req.fieldOverrides)
            val content = SnippetTemplates.requestFieldsTable(fields)
            SnippetWriter.write(outputDir, doc.identifier, "request-fields", content)
        }

        // Response fields
        for (resp in op.responses) {
            val fields = flattener.flatten(resp.schema, resp.fieldOverrides)
            val content = SnippetTemplates.responseFieldsTable(fields)
            SnippetWriter.write(outputDir, doc.identifier, "response-fields", content)
        }

        // Path parameters
        val pathParams = op.parameters.filter { it.location == ParameterLocation.PATH }
        if (pathParams.isNotEmpty()) {
            val content = SnippetTemplates.parametersTable("Path Parameters", pathParams)
            SnippetWriter.write(outputDir, doc.identifier, "path-parameters", content)
        }

        // Query parameters
        val queryParams = op.parameters.filter { it.location == ParameterLocation.QUERY }
        if (queryParams.isNotEmpty()) {
            val content = SnippetTemplates.parametersTable("Query Parameters", queryParams)
            SnippetWriter.write(outputDir, doc.identifier, "query-parameters", content)
        }

        // Header parameters
        val headerParams = op.parameters.filter { it.location == ParameterLocation.HEADER }
        if (headerParams.isNotEmpty()) {
            val content = SnippetTemplates.parametersTable("Header Parameters", headerParams)
            SnippetWriter.write(outputDir, doc.identifier, "header-parameters", content)
        }
    }
}
