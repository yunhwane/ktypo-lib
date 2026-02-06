package com.yunhwan.ktypo.dsl

import com.yunhwan.ktypo.schema.FieldDescriptor

@KtypoDslMarker
class FieldBuilder(private val path: String) {
    private var description: String? = null
    private var example: Any? = null
    private var format: String? = null
    private var pattern: String? = null
    private var minLength: Int? = null
    private var maxLength: Int? = null
    private var minimum: Number? = null
    private var maximum: Number? = null
    private var deprecated: Boolean = false

    fun description(value: String) { description = value }
    fun example(value: Any) { example = value }
    fun format(value: String) { format = value }
    fun pattern(value: String) { pattern = value }
    fun minLength(value: Int) { minLength = value }
    fun maxLength(value: Int) { maxLength = value }
    fun minimum(value: Number) { minimum = value }
    fun maximum(value: Number) { maximum = value }
    fun deprecated(value: Boolean = true) { deprecated = value }

    fun build(): FieldDescriptor = FieldDescriptor(
        path = path,
        description = description,
        example = example,
        format = format,
        pattern = pattern,
        minLength = minLength,
        maxLength = maxLength,
        minimum = minimum,
        maximum = maximum,
        deprecated = deprecated,
    )
}
