package com.yunhwan.ktypo.model

import com.yunhwan.ktypo.schema.FieldDescriptor
import com.yunhwan.ktypo.schema.SchemaObject
import kotlin.reflect.KType

data class RequestModel(
    val kType: KType,
    val schema: SchemaObject,
    val fieldOverrides: Map<String, FieldDescriptor> = emptyMap(),
    val contentType: String = "application/json",
)
