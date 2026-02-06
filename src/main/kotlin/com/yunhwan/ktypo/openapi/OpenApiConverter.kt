package com.yunhwan.ktypo.openapi

import com.yunhwan.ktypo.schema.SchemaObject
import com.yunhwan.ktypo.schema.SchemaType
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.media.Discriminator as SwaggerDiscriminator

object OpenApiConverter {

    fun convert(schemaObject: SchemaObject): Schema<*> {
        if (schemaObject.isRef) {
            return Schema<Any>().`$ref`(schemaObject.ref).also { schema ->
                if (schemaObject.nullable) {
                    schema.nullable = true
                }
            }
        }

        if (schemaObject.oneOf != null) {
            return convertOneOf(schemaObject)
        }

        val schema: Schema<*> = when (schemaObject.type) {
            SchemaType.STRING -> convertString(schemaObject)
            SchemaType.INTEGER -> convertInteger(schemaObject)
            SchemaType.NUMBER -> convertNumber(schemaObject)
            SchemaType.BOOLEAN -> BooleanSchema()
            SchemaType.ARRAY -> convertArray(schemaObject)
            SchemaType.OBJECT -> convertObject(schemaObject)
            null -> Schema<Any>()
        }

        applyCommonProperties(schema, schemaObject)
        return schema
    }

    private fun convertString(obj: SchemaObject): Schema<*> {
        val schema = StringSchema()
        obj.format?.let { schema.format = it }
        obj.pattern?.let { schema.pattern = it }
        obj.minLength?.let { schema.minLength = it }
        obj.maxLength?.let { schema.maxLength = it }
        obj.enumValues?.let { values ->
            schema.setEnum(values)
        }
        return schema
    }

    private fun convertInteger(obj: SchemaObject): Schema<*> {
        val schema = IntegerSchema()
        obj.format?.let { schema.format = it }
        obj.minimum?.let { schema.minimum = it.toBigDecimal() }
        obj.maximum?.let { schema.maximum = it.toBigDecimal() }
        return schema
    }

    private fun convertNumber(obj: SchemaObject): Schema<*> {
        val schema = NumberSchema()
        obj.format?.let { schema.format = it }
        obj.minimum?.let { schema.minimum = it.toBigDecimal() }
        obj.maximum?.let { schema.maximum = it.toBigDecimal() }
        return schema
    }

    private fun convertArray(obj: SchemaObject): Schema<*> {
        val schema = ArraySchema()
        obj.items?.let { schema.items = convert(it) }
        return schema
    }

    private fun convertObject(obj: SchemaObject): Schema<*> {
        if (obj.additionalProperties != null) {
            val schema = MapSchema()
            schema.additionalProperties = convert(obj.additionalProperties)
            return schema
        }

        val schema = ObjectSchema()
        obj.properties?.forEach { (name, prop) ->
            schema.addProperty(name, convert(prop))
        }
        obj.requiredProperties?.let { schema.required = it }
        return schema
    }

    private fun convertOneOf(obj: SchemaObject): Schema<*> {
        val schema = ComposedSchema()
        obj.oneOf?.forEach { schema.addOneOfItem(convert(it)) }
        obj.discriminator?.let { disc ->
            schema.discriminator = SwaggerDiscriminator().apply {
                propertyName = disc.propertyName
                mapping = disc.mapping
            }
        }
        return schema
    }

    private fun applyCommonProperties(schema: Schema<*>, obj: SchemaObject) {
        obj.title?.let { schema.title = it }
        obj.description?.let { schema.description = it }
        obj.example?.let { schema.example = it }
        if (obj.nullable) {
            schema.nullable = true
        }
    }

    fun Number.toBigDecimal(): java.math.BigDecimal = when (this) {
        is java.math.BigDecimal -> this
        is Double -> java.math.BigDecimal.valueOf(this)
        is Float -> java.math.BigDecimal.valueOf(this.toDouble())
        is Long -> java.math.BigDecimal.valueOf(this)
        is Int -> java.math.BigDecimal.valueOf(this.toLong())
        else -> java.math.BigDecimal(this.toString())
    }
}
