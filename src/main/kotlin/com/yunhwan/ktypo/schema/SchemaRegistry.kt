package com.yunhwan.ktypo.schema

import kotlin.reflect.KType

class SchemaRegistry {
    private val schemas = mutableMapOf<String, SchemaObject>()
    private val typeNameMap = mutableMapOf<KType, String>()

    fun register(name: String, schema: SchemaObject, kType: KType) {
        schemas[name] = schema
        typeNameMap[kType] = name
    }

    fun getRefName(kType: KType): String? = typeNameMap[kType]

    fun getSchema(name: String): SchemaObject? = schemas[name]

    fun refFor(name: String): SchemaObject =
        SchemaObject(ref = "#/components/schemas/$name")

    fun allSchemas(): Map<String, SchemaObject> = schemas.toMap()

    fun clear() {
        schemas.clear()
        typeNameMap.clear()
    }
}
