package com.yunhwan.ktypo.model

data class ParameterModel(
    val name: String,
    val location: ParameterLocation,
    val description: String? = null,
    val required: Boolean = true,
    val schema: com.yunhwan.ktypo.schema.SchemaObject? = null,
    val example: Any? = null,
)

enum class ParameterLocation(val value: String) {
    PATH("path"),
    QUERY("query"),
    HEADER("header"),
}
