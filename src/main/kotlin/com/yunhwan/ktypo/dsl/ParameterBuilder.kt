package com.yunhwan.ktypo.dsl

import com.yunhwan.ktypo.model.ParameterLocation
import com.yunhwan.ktypo.model.ParameterModel
import com.yunhwan.ktypo.schema.SchemaObject
import com.yunhwan.ktypo.schema.SchemaType

@KtypoDslMarker
class ParameterBuilder(
    private val name: String,
    private val location: ParameterLocation,
) {
    private var description: String? = null
    private var required: Boolean = location == ParameterLocation.PATH
    private var example: Any? = null
    private var schema: SchemaObject = SchemaObject(type = SchemaType.STRING)

    fun description(value: String) { description = value }
    fun required(value: Boolean) { required = value }
    fun example(value: Any) { example = value }
    fun schema(value: SchemaObject) { schema = value }

    fun build(): ParameterModel = ParameterModel(
        name = name,
        location = location,
        description = description,
        required = required,
        schema = schema,
        example = example,
    )
}
