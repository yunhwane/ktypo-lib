package com.yunhwan.ktypo.dsl

import com.yunhwan.ktypo.model.DocumentModel
import com.yunhwan.ktypo.model.HttpMethod
import com.yunhwan.ktypo.model.OperationModel
import com.yunhwan.ktypo.schema.TypeResolver

@KtypoDslMarker
class DocumentBuilder(
    private val identifier: String,
    private val typeResolver: TypeResolver,
) {
    private var operation: OperationModel? = null

    fun get(path: String, block: OperationBuilder.() -> Unit) {
        operation = buildOperation(HttpMethod.GET, path, block)
    }

    fun post(path: String, block: OperationBuilder.() -> Unit) {
        operation = buildOperation(HttpMethod.POST, path, block)
    }

    fun put(path: String, block: OperationBuilder.() -> Unit) {
        operation = buildOperation(HttpMethod.PUT, path, block)
    }

    fun delete(path: String, block: OperationBuilder.() -> Unit) {
        operation = buildOperation(HttpMethod.DELETE, path, block)
    }

    fun patch(path: String, block: OperationBuilder.() -> Unit) {
        operation = buildOperation(HttpMethod.PATCH, path, block)
    }

    private fun buildOperation(
        method: HttpMethod,
        path: String,
        block: OperationBuilder.() -> Unit,
    ): OperationModel {
        val builder = OperationBuilder(method, path, typeResolver)
        builder.block()
        return builder.build()
    }

    fun build(): DocumentModel {
        val op = operation ?: throw IllegalStateException(
            "Document '$identifier' must define an HTTP operation (get, post, put, delete, patch)"
        )
        return DocumentModel(identifier = identifier, operation = op)
    }
}
