package com.yunhwan.ktypo.restdocs

import com.yunhwan.ktypo.schema.FieldDescriptor
import com.yunhwan.ktypo.schema.SchemaObject
import com.yunhwan.ktypo.schema.SchemaRegistry
import com.yunhwan.ktypo.schema.SchemaType

data class FlatField(
    val path: String,
    val type: String,
    val description: String,
    val optional: Boolean,
)

class FieldFlattener(private val registry: SchemaRegistry) {

    fun flatten(
        schema: SchemaObject,
        fieldOverrides: Map<String, FieldDescriptor> = emptyMap(),
    ): List<FlatField> {
        val fields = mutableListOf<FlatField>()
        flattenRecursive(resolveRef(schema), "", fields, fieldOverrides)
        return fields
    }

    private fun flattenRecursive(
        schema: SchemaObject,
        prefix: String,
        fields: MutableList<FlatField>,
        overrides: Map<String, FieldDescriptor>,
    ) {
        val properties = schema.properties ?: return
        val required = schema.requiredProperties ?: emptyList()

        for ((name, propSchema) in properties) {
            val path = if (prefix.isEmpty()) name else "$prefix.$name"
            val resolved = resolveRef(propSchema)
            val override = overrides[path]

            val type = formatType(resolved)
            val desc = override?.description ?: resolved.description ?: ""
            val optional = name !in required || resolved.nullable

            fields.add(FlatField(path, type, desc, optional))

            // Recurse into nested objects
            when {
                resolved.type == SchemaType.OBJECT && resolved.properties != null ->
                    flattenRecursive(resolved, path, fields, overrides)

                resolved.type == SchemaType.ARRAY && resolved.items != null -> {
                    val itemResolved = resolveRef(resolved.items)
                    if (itemResolved.type == SchemaType.OBJECT && itemResolved.properties != null) {
                        flattenRecursive(itemResolved, "$path[]", fields, overrides)
                    }
                }
            }
        }
    }

    private fun resolveRef(schema: SchemaObject): SchemaObject {
        if (!schema.isRef) return schema
        val refName = schema.ref!!.removePrefix("#/components/schemas/")
        return registry.getSchema(refName) ?: schema
    }

    private fun formatType(schema: SchemaObject): String = when {
        schema.enumValues != null -> "Enum"
        schema.type == SchemaType.ARRAY -> {
            val itemType = schema.items?.let { resolveRef(it) }?.let { formatType(it) } ?: "Object"
            "Array<$itemType>"
        }
        schema.type != null -> schema.type.value.replaceFirstChar { it.uppercase() }
        schema.oneOf != null -> "OneOf"
        else -> "Object"
    }
}
