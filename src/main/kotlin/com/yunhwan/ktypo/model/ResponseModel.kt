package com.yunhwan.ktypo.model

import com.yunhwan.ktypo.schema.FieldDescriptor
import com.yunhwan.ktypo.schema.SchemaObject
import kotlin.reflect.KType

data class ResponseModel(
    val statusCode: Int = 200,
    val description: String? = null,
    val kType: KType,
    val schema: SchemaObject,
    val fieldOverrides: Map<String, FieldDescriptor> = emptyMap(),
    val contentType: String = "application/json",
)
