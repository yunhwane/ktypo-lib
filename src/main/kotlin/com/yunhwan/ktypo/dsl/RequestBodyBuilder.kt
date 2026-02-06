package com.yunhwan.ktypo.dsl

import com.yunhwan.ktypo.model.RequestModel
import com.yunhwan.ktypo.schema.FieldDescriptor
import com.yunhwan.ktypo.schema.SchemaObject
import com.yunhwan.ktypo.schema.TypeResolver
import kotlin.reflect.KProperty1
import kotlin.reflect.KType

@KtypoDslMarker
class RequestBodyBuilder(
    private val kType: KType,
    private val typeResolver: TypeResolver,
) {
    private val fieldOverrides = mutableMapOf<String, FieldDescriptor>()
    private var contentType: String = "application/json"

    fun <T, V> field(property: KProperty1<T, V>, block: FieldBuilder.() -> Unit = {}) {
        val builder = FieldBuilder(property.name)
        builder.block()
        fieldOverrides[property.name] = builder.build()
    }

    fun field(path: String, block: FieldBuilder.() -> Unit = {}) {
        val builder = FieldBuilder(path)
        builder.block()
        fieldOverrides[path] = builder.build()
    }

    fun contentType(value: String) { contentType = value }

    fun build(): RequestModel {
        val schema = typeResolver.resolve(kType)
        return RequestModel(
            kType = kType,
            schema = schema,
            fieldOverrides = fieldOverrides.toMap(),
            contentType = contentType,
        )
    }
}
