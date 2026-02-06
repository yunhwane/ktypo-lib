package com.yunhwan.ktypo.schema

data class FieldDescriptor(
    val path: String,
    val description: String? = null,
    val example: Any? = null,
    val format: String? = null,
    val pattern: String? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val minimum: Number? = null,
    val maximum: Number? = null,
    val deprecated: Boolean = false,
)
