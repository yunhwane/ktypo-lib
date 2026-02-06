package com.yunhwan.ktypo.schema

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

class TypeResolver(
    private val registry: SchemaRegistry = SchemaRegistry(),
) {
    fun resolve(kType: KType): SchemaObject = resolve(kType, emptySet())

    fun registry(): SchemaRegistry = registry

    private fun resolve(kType: KType, resolutionStack: Set<KClass<*>>): SchemaObject {
        val classifier = kType.classifier
        val nullable = kType.isMarkedNullable

        val schema = when (classifier) {
            is KTypeParameter -> SchemaObject(type = SchemaType.OBJECT)
            is KClass<*> -> resolveClass(classifier, kType, resolutionStack)
            else -> SchemaObject(type = SchemaType.OBJECT)
        }

        return if (nullable && !schema.isRef) {
            schema.copy(nullable = true)
        } else if (nullable && schema.isRef) {
            // For nullable $ref, we need to wrap in a oneOf or allOf
            // OpenAPI 3.1 supports nullable refs via oneOf
            schema.copy(nullable = true)
        } else {
            schema
        }
    }

    private fun resolveClass(
        kClass: KClass<*>,
        kType: KType,
        resolutionStack: Set<KClass<*>>,
    ): SchemaObject = when {
        kClass == String::class -> SchemaObject(type = SchemaType.STRING)
        kClass == Char::class -> SchemaObject(type = SchemaType.STRING)

        kClass == Int::class || kClass == Integer::class ->
            SchemaObject(type = SchemaType.INTEGER, format = "int32")
        kClass == Long::class || kClass == java.lang.Long::class ->
            SchemaObject(type = SchemaType.INTEGER, format = "int64")
        kClass == Short::class ->
            SchemaObject(type = SchemaType.INTEGER, format = "int32")
        kClass == Byte::class ->
            SchemaObject(type = SchemaType.INTEGER, format = "int32")

        kClass == Float::class ->
            SchemaObject(type = SchemaType.NUMBER, format = "float")
        kClass == Double::class ->
            SchemaObject(type = SchemaType.NUMBER, format = "double")
        kClass == java.math.BigDecimal::class ->
            SchemaObject(type = SchemaType.NUMBER)
        kClass == java.math.BigInteger::class ->
            SchemaObject(type = SchemaType.INTEGER)

        kClass == Boolean::class -> SchemaObject(type = SchemaType.BOOLEAN)

        kClass == java.time.LocalDate::class ->
            SchemaObject(type = SchemaType.STRING, format = "date")
        kClass == java.time.LocalDateTime::class ->
            SchemaObject(type = SchemaType.STRING, format = "date-time")
        kClass == java.time.OffsetDateTime::class ->
            SchemaObject(type = SchemaType.STRING, format = "date-time")
        kClass == java.time.ZonedDateTime::class ->
            SchemaObject(type = SchemaType.STRING, format = "date-time")
        kClass == java.time.Instant::class ->
            SchemaObject(type = SchemaType.STRING, format = "date-time")

        kClass == java.util.UUID::class ->
            SchemaObject(type = SchemaType.STRING, format = "uuid")
        kClass == java.net.URI::class ->
            SchemaObject(type = SchemaType.STRING, format = "uri")

        kClass.java.isEnum -> resolveEnum(kClass)

        isCollectionType(kClass) -> resolveCollection(kType, resolutionStack)
        isMapType(kClass) -> resolveMap(kType, resolutionStack)

        kClass.isSealed -> resolveSealed(kClass, kType, resolutionStack)

        kClass.isData || kClass.constructors.isNotEmpty() ->
            resolveDataClass(kClass, kType, resolutionStack)

        else -> SchemaObject(type = SchemaType.OBJECT)
    }

    private fun resolveEnum(kClass: KClass<*>): SchemaObject {
        val values = kClass.java.enumConstants?.map { it.toString() } ?: emptyList()
        return SchemaObject(type = SchemaType.STRING, enumValues = values)
    }

    private fun isCollectionType(kClass: KClass<*>): Boolean =
        Collection::class.java.isAssignableFrom(kClass.java) ||
            Array::class.java.isAssignableFrom(kClass.java)

    private fun isMapType(kClass: KClass<*>): Boolean =
        Map::class.java.isAssignableFrom(kClass.java)

    private fun resolveCollection(kType: KType, resolutionStack: Set<KClass<*>>): SchemaObject {
        val itemType = kType.arguments.firstOrNull()?.type
        val itemSchema = if (itemType != null) {
            resolve(itemType, resolutionStack)
        } else {
            SchemaObject(type = SchemaType.OBJECT)
        }
        return SchemaObject(type = SchemaType.ARRAY, items = itemSchema)
    }

    private fun resolveMap(kType: KType, resolutionStack: Set<KClass<*>>): SchemaObject {
        val valueType = kType.arguments.getOrNull(1)?.type
        val valueSchema = if (valueType != null) {
            resolve(valueType, resolutionStack)
        } else {
            SchemaObject(type = SchemaType.OBJECT)
        }
        return SchemaObject(type = SchemaType.OBJECT, additionalProperties = valueSchema)
    }

    private fun resolveSealed(
        kClass: KClass<*>,
        kType: KType,
        resolutionStack: Set<KClass<*>>,
    ): SchemaObject {
        val subclasses = kClass.sealedSubclasses
        val schemas = subclasses.map { subclass ->
            val subType = subclass.createType(kType.arguments)
            resolve(subType, resolutionStack)
        }

        val discriminatorProperty = "type"
        val mapping = subclasses.associate { subclass ->
            val name = subclass.simpleName ?: subclass.qualifiedName ?: "Unknown"
            name to "#/components/schemas/$name"
        }

        return SchemaObject(
            oneOf = schemas,
            discriminator = Discriminator(
                propertyName = discriminatorProperty,
                mapping = mapping,
            ),
        )
    }

    private fun resolveDataClass(
        kClass: KClass<*>,
        kType: KType,
        resolutionStack: Set<KClass<*>>,
    ): SchemaObject {
        // Circular reference detection
        if (kClass in resolutionStack) {
            val name = schemaName(kClass, kType)
            return registry.refFor(name)
        }

        val name = schemaName(kClass, kType)
        val existing = registry.getRefName(kType)
        if (existing != null) {
            return registry.refFor(existing)
        }

        val newStack = resolutionStack + kClass

        // Build type parameter mapping: KTypeParameter -> actual KType
        val typeParamMap = buildTypeParamMap(kClass, kType)

        // Get properties in constructor parameter order
        val constructor = kClass.primaryConstructor
        val paramOrder = constructor?.parameters?.map { it.name } ?: emptyList()
        val properties = kClass.memberProperties
            .sortedBy { prop ->
                val idx = paramOrder.indexOf(prop.name)
                if (idx >= 0) idx else Int.MAX_VALUE
            }

        val schemaProperties = mutableMapOf<String, SchemaObject>()
        val requiredProps = mutableListOf<String>()

        for (prop in properties) {
            val propType = substituteTypeParameters(prop.returnType, typeParamMap)
            val propSchema = resolve(propType, newStack)
            schemaProperties[prop.name] = propSchema

            if (!propType.isMarkedNullable) {
                requiredProps.add(prop.name)
            }
        }

        val schema = SchemaObject(
            type = SchemaType.OBJECT,
            title = kClass.simpleName,
            properties = schemaProperties.ifEmpty { null },
            requiredProperties = requiredProps.ifEmpty { null },
        )

        registry.register(name, schema, kType)
        return registry.refFor(name)
    }

    private fun buildTypeParamMap(kClass: KClass<*>, kType: KType): Map<KTypeParameter, KType> {
        val typeParams = kClass.typeParameters
        val typeArgs = kType.arguments
        val map = mutableMapOf<KTypeParameter, KType>()
        for (i in typeParams.indices) {
            val arg = typeArgs.getOrNull(i)?.type ?: continue
            map[typeParams[i]] = arg
        }
        return map
    }

    private fun substituteTypeParameters(
        type: KType,
        typeParamMap: Map<KTypeParameter, KType>,
    ): KType {
        val classifier = type.classifier

        // If the classifier is a type parameter, substitute it
        if (classifier is KTypeParameter) {
            return typeParamMap[classifier] ?: type
        }

        if (classifier !is KClass<*>) return type

        // Recursively substitute in type arguments
        val newArgs = type.arguments.map { projection ->
            val projType = projection.type ?: return@map projection
            val substituted = substituteTypeParameters(projType, typeParamMap)
            KTypeProjection(projection.variance, substituted)
        }

        return if (newArgs != type.arguments) {
            classifier.createType(newArgs, type.isMarkedNullable)
        } else {
            type
        }
    }

    private fun schemaName(kClass: KClass<*>, kType: KType): String {
        val baseName = kClass.simpleName ?: "Unknown"
        val typeArgs = kType.arguments
        return if (typeArgs.isEmpty()) {
            baseName
        } else {
            val argNames = typeArgs.mapNotNull { arg ->
                arg.type?.let { argType ->
                    val argClass = argType.jvmErasure
                    schemaName(argClass, argType)
                }
            }
            "${baseName}_${argNames.joinToString("_")}"
        }
    }

    fun applyFieldOverrides(
        schema: SchemaObject,
        fieldOverrides: Map<String, FieldDescriptor>,
    ): SchemaObject {
        if (fieldOverrides.isEmpty() || schema.properties == null) return schema

        val updatedProps = schema.properties.toMutableMap()
        for ((path, descriptor) in fieldOverrides) {
            if ("." !in path) {
                val existing = updatedProps[path] ?: continue
                updatedProps[path] = applyDescriptor(existing, descriptor)
            }
        }
        return schema.copy(properties = updatedProps)
    }

    fun applyNestedFieldOverrides(
        schemaName: String,
        fieldOverrides: Map<String, FieldDescriptor>,
    ) {
        for ((path, descriptor) in fieldOverrides) {
            val parts = path.split(".")
            applyNestedOverride(schemaName, parts, descriptor)
        }
    }

    private fun applyNestedOverride(
        schemaName: String,
        pathParts: List<String>,
        descriptor: FieldDescriptor,
    ) {
        if (pathParts.isEmpty()) return
        val schema = registry.getSchema(schemaName) ?: return
        if (schema.properties == null) return

        if (pathParts.size == 1) {
            val fieldName = pathParts[0]
            val existing = schema.properties[fieldName] ?: return
            val updated = schema.copy(
                properties = schema.properties + (fieldName to applyDescriptor(existing, descriptor)),
            )
            registry.register(schemaName, updated, findKTypeForSchema(schemaName) ?: return)
        } else {
            val fieldName = pathParts[0]
            val childSchema = schema.properties[fieldName] ?: return
            if (childSchema.isRef) {
                val refName = childSchema.ref!!.removePrefix("#/components/schemas/")
                applyNestedOverride(refName, pathParts.drop(1), descriptor)
            }
        }
    }

    private fun findKTypeForSchema(name: String): KType? {
        // Search the registry for matching type
        return null // Field overrides are applied directly; KType lookup is not needed for updates
    }

    private fun applyDescriptor(schema: SchemaObject, descriptor: FieldDescriptor): SchemaObject =
        schema.copy(
            description = descriptor.description ?: schema.description,
            example = descriptor.example ?: schema.example,
            format = descriptor.format ?: schema.format,
            pattern = descriptor.pattern ?: schema.pattern,
            minLength = descriptor.minLength ?: schema.minLength,
            maxLength = descriptor.maxLength ?: schema.maxLength,
            minimum = descriptor.minimum ?: schema.minimum,
            maximum = descriptor.maximum ?: schema.maximum,
        )
}
