package com.yunhwan.ktypo.dsl

import com.yunhwan.ktypo.model.HttpMethod
import com.yunhwan.ktypo.model.OperationModel
import com.yunhwan.ktypo.model.ParameterLocation
import com.yunhwan.ktypo.model.ParameterModel
import com.yunhwan.ktypo.model.RequestModel
import com.yunhwan.ktypo.model.ResponseModel
import com.yunhwan.ktypo.schema.TypeResolver
import kotlin.reflect.typeOf

@KtypoDslMarker
class OperationBuilder(
    private val method: HttpMethod,
    private val path: String,
    @PublishedApi internal val typeResolver: TypeResolver,
    private val commonResponses: List<ResponseModel> = emptyList(),
) {
    private var summary: String? = null
    private var description: String? = null
    private var tags: List<String> = emptyList()
    private var operationId: String? = null
    @PublishedApi internal var request: RequestModel? = null
    @PublishedApi internal val responses = mutableListOf<ResponseModel>()
    private val parameters = mutableListOf<ParameterModel>()
    private var deprecated: Boolean = false
    private val excludedStatusCodes = mutableSetOf<Int>()

    fun summary(value: String) { summary = value }
    fun description(value: String) { description = value }
    fun tags(vararg values: String) { tags = values.toList() }
    fun operationId(value: String) { operationId = value }
    fun deprecated(value: Boolean = true) { deprecated = value }
    fun excludeCommonResponses(vararg statusCodes: Int) { excludedStatusCodes.addAll(statusCodes.toSet()) }

    inline fun <reified T> requestBody(noinline block: RequestBodyBuilder.() -> Unit = {}) {
        val builder = RequestBodyBuilder(typeOf<T>(), typeResolver)
        builder.block()
        request = builder.build()
    }

    inline fun <reified T> responseBody(noinline block: ResponseBodyBuilder.() -> Unit = {}) {
        val builder = ResponseBodyBuilder(typeOf<T>(), typeResolver)
        builder.block()
        responses.add(builder.build())
    }

    fun pathParameter(name: String, block: ParameterBuilder.() -> Unit = {}) {
        val builder = ParameterBuilder(name, ParameterLocation.PATH)
        builder.block()
        parameters.add(builder.build())
    }

    fun queryParameter(name: String, block: ParameterBuilder.() -> Unit = {}) {
        val builder = ParameterBuilder(name, ParameterLocation.QUERY)
        builder.block()
        parameters.add(builder.build())
    }

    fun headerParameter(name: String, block: ParameterBuilder.() -> Unit = {}) {
        val builder = ParameterBuilder(name, ParameterLocation.HEADER)
        builder.block()
        parameters.add(builder.build())
    }

    fun build(): OperationModel {
        val operationStatusCodes = responses.map { it.statusCode }.toSet()
        val filteredCommon = commonResponses.filter { common ->
            common.statusCode !in operationStatusCodes && common.statusCode !in excludedStatusCodes
        }
        val mergedResponses = responses + filteredCommon

        return OperationModel(
            method = method,
            path = path,
            summary = summary,
            description = description,
            tags = tags,
            operationId = operationId,
            request = request,
            responses = mergedResponses,
            parameters = parameters.toList(),
            deprecated = deprecated,
        )
    }
}
