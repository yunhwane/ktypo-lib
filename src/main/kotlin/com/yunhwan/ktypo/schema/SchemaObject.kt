package com.yunhwan.ktypo.schema

data class SchemaObject(
    val type: SchemaType? = null,
    val format: String? = null,
    val nullable: Boolean = false,
    val properties: Map<String, SchemaObject>? = null,
    val requiredProperties: List<String>? = null,
    val items: SchemaObject? = null,
    val additionalProperties: SchemaObject? = null,
    val enumValues: List<String>? = null,
    val oneOf: List<SchemaObject>? = null,
    val discriminator: Discriminator? = null,
    val ref: String? = null,
    val title: String? = null,
    val description: String? = null,
    val example: Any? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val minimum: Number? = null,
    val maximum: Number? = null,
    val pattern: String? = null,
) {
    val isRef: Boolean get() = ref != null
}

enum class SchemaType(val value: String) {
    STRING("string"),
    INTEGER("integer"),
    NUMBER("number"),
    BOOLEAN("boolean"),
    ARRAY("array"),
    OBJECT("object"),
}

data class Discriminator(
    val propertyName: String,
    val mapping: Map<String, String>,
)
