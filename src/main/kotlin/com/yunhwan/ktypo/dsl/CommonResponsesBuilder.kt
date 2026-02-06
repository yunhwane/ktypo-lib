package com.yunhwan.ktypo.dsl

import com.yunhwan.ktypo.model.ResponseModel
import com.yunhwan.ktypo.schema.TypeResolver
import kotlin.reflect.typeOf

@KtypoDslMarker
class CommonResponsesBuilder(
    @PublishedApi internal val typeResolver: TypeResolver,
) {
    @PublishedApi internal val responses = mutableListOf<ResponseModel>()

    inline fun <reified T> response(statusCode: Int, noinline block: ResponseBodyBuilder.() -> Unit = {}) {
        val builder = ResponseBodyBuilder(typeOf<T>(), typeResolver)
        builder.statusCode(statusCode)
        builder.block()
        responses.add(builder.build())
    }

    fun response(statusCode: Int, description: String) {
        responses.add(
            ResponseModel(
                statusCode = statusCode,
                description = description,
            )
        )
    }

    fun build(): List<ResponseModel> = responses.toList()
}
