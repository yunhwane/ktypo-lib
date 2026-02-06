package com.yunhwan.ktypo.schema

import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TypeResolverTest {

    private val resolver = TypeResolver()

    // -- Primitive types --

    @Test
    fun `resolves String type`() {
        val schema = resolver.resolve(typeOf<String>())
        assertEquals(SchemaType.STRING, schema.type)
    }

    @Test
    fun `resolves Int type`() {
        val schema = resolver.resolve(typeOf<Int>())
        assertEquals(SchemaType.INTEGER, schema.type)
        assertEquals("int32", schema.format)
    }

    @Test
    fun `resolves Long type`() {
        val schema = resolver.resolve(typeOf<Long>())
        assertEquals(SchemaType.INTEGER, schema.type)
        assertEquals("int64", schema.format)
    }

    @Test
    fun `resolves Double type`() {
        val schema = resolver.resolve(typeOf<Double>())
        assertEquals(SchemaType.NUMBER, schema.type)
        assertEquals("double", schema.format)
    }

    @Test
    fun `resolves Float type`() {
        val schema = resolver.resolve(typeOf<Float>())
        assertEquals(SchemaType.NUMBER, schema.type)
        assertEquals("float", schema.format)
    }

    @Test
    fun `resolves Boolean type`() {
        val schema = resolver.resolve(typeOf<Boolean>())
        assertEquals(SchemaType.BOOLEAN, schema.type)
    }

    // -- Nullable --

    @Test
    fun `resolves nullable String`() {
        val schema = resolver.resolve(typeOf<String?>())
        assertEquals(SchemaType.STRING, schema.type)
        assertTrue(schema.nullable)
    }

    @Test
    fun `resolves nullable Int`() {
        val schema = resolver.resolve(typeOf<Int?>())
        assertEquals(SchemaType.INTEGER, schema.type)
        assertTrue(schema.nullable)
    }

    // -- Date/Time types --

    @Test
    fun `resolves LocalDate type`() {
        val schema = resolver.resolve(typeOf<java.time.LocalDate>())
        assertEquals(SchemaType.STRING, schema.type)
        assertEquals("date", schema.format)
    }

    @Test
    fun `resolves LocalDateTime type`() {
        val schema = resolver.resolve(typeOf<java.time.LocalDateTime>())
        assertEquals(SchemaType.STRING, schema.type)
        assertEquals("date-time", schema.format)
    }

    // -- Enum --

    enum class Status { ACTIVE, INACTIVE, PENDING }

    @Test
    fun `resolves enum type`() {
        val schema = resolver.resolve(typeOf<Status>())
        assertEquals(SchemaType.STRING, schema.type)
        assertEquals(listOf("ACTIVE", "INACTIVE", "PENDING"), schema.enumValues)
    }

    // -- Collection types --

    @Test
    fun `resolves List of String`() {
        val schema = resolver.resolve(typeOf<List<String>>())
        assertEquals(SchemaType.ARRAY, schema.type)
        assertNotNull(schema.items)
        assertEquals(SchemaType.STRING, schema.items!!.type)
    }

    @Test
    fun `resolves Set of Int`() {
        val schema = resolver.resolve(typeOf<Set<Int>>())
        assertEquals(SchemaType.ARRAY, schema.type)
        assertNotNull(schema.items)
        assertEquals(SchemaType.INTEGER, schema.items!!.type)
    }

    @Test
    fun `resolves List of nullable elements`() {
        val schema = resolver.resolve(typeOf<List<String?>>())
        assertEquals(SchemaType.ARRAY, schema.type)
        assertNotNull(schema.items)
        assertEquals(SchemaType.STRING, schema.items!!.type)
        assertTrue(schema.items!!.nullable)
    }

    // -- Map types --

    @Test
    fun `resolves Map of String to Int`() {
        val schema = resolver.resolve(typeOf<Map<String, Int>>())
        assertEquals(SchemaType.OBJECT, schema.type)
        assertNotNull(schema.additionalProperties)
        assertEquals(SchemaType.INTEGER, schema.additionalProperties!!.type)
    }

    // -- Data class --

    data class SimpleUser(val id: Long, val name: String, val email: String?)

    @Test
    fun `resolves simple data class`() {
        val resolver = TypeResolver()
        val schema = resolver.resolve(typeOf<SimpleUser>())
        assertTrue(schema.isRef)

        val registered = resolver.registry().allSchemas()
        assertEquals(1, registered.size)
        val userSchema = registered["SimpleUser"]
        assertNotNull(userSchema)
        assertEquals(SchemaType.OBJECT, userSchema.type)
        assertEquals(3, userSchema.properties!!.size)

        // Check property types
        assertEquals(SchemaType.INTEGER, userSchema.properties["id"]!!.type)
        assertEquals("int64", userSchema.properties["id"]!!.format)
        assertEquals(SchemaType.STRING, userSchema.properties["name"]!!.type)
        assertEquals(SchemaType.STRING, userSchema.properties["email"]!!.type)
        assertTrue(userSchema.properties["email"]!!.nullable)

        // Required: id and name (non-nullable), email is nullable
        assertEquals(listOf("id", "name"), userSchema.requiredProperties)
    }

    // -- Nested generic types --

    data class ApiResponse<T>(val code: Int, val message: String, val data: T)
    data class PageResponse<T>(val items: List<T>, val totalCount: Long, val page: Int)
    data class UserDto(val id: Long, val name: String, val email: String)

    @Test
    fun `resolves nested generic type ApiResponse of UserDto`() {
        val resolver = TypeResolver()
        val schema = resolver.resolve(typeOf<ApiResponse<UserDto>>())
        assertTrue(schema.isRef)

        val allSchemas = resolver.registry().allSchemas()
        assertTrue(allSchemas.containsKey("ApiResponse_UserDto"))
        assertTrue(allSchemas.containsKey("UserDto"))

        val apiResponseSchema = allSchemas["ApiResponse_UserDto"]!!
        assertEquals(SchemaType.OBJECT, apiResponseSchema.type)
        assertEquals(3, apiResponseSchema.properties!!.size)

        // 'data' should be a $ref to UserDto
        val dataField = apiResponseSchema.properties["data"]!!
        assertTrue(dataField.isRef)
        assertEquals("#/components/schemas/UserDto", dataField.ref)
    }

    @Test
    fun `resolves deeply nested generic ApiResponse of PageResponse of UserDto`() {
        val resolver = TypeResolver()
        val schema = resolver.resolve(typeOf<ApiResponse<PageResponse<UserDto>>>())
        assertTrue(schema.isRef)

        val allSchemas = resolver.registry().allSchemas()

        // Should have schemas for all composed types
        assertTrue(allSchemas.containsKey("UserDto"))
        assertTrue(allSchemas.containsKey("PageResponse_UserDto"))
        assertTrue(allSchemas.containsKey("ApiResponse_PageResponse_UserDto"))

        // Check PageResponse schema: items should be List<UserDto> → array of $ref UserDto
        val pageSchema = allSchemas["PageResponse_UserDto"]!!
        val itemsField = pageSchema.properties!!["items"]!!
        assertEquals(SchemaType.ARRAY, itemsField.type)
        assertTrue(itemsField.items!!.isRef)
        assertEquals("#/components/schemas/UserDto", itemsField.items!!.ref)

        // Check ApiResponse schema: data should be $ref to PageResponse_UserDto
        val apiSchema = allSchemas["ApiResponse_PageResponse_UserDto"]!!
        val dataField = apiSchema.properties!!["data"]!!
        assertTrue(dataField.isRef)
        assertEquals("#/components/schemas/PageResponse_UserDto", dataField.ref)
    }

    @Test
    fun `resolves triple nested ApiResponse of PageResponse of List of UserDto`() {
        // This tests the case where T=List<UserDto>, so items becomes List<List<UserDto>>
        val resolver = TypeResolver()
        val schema = resolver.resolve(typeOf<ApiResponse<PageResponse<List<UserDto>>>>())
        assertTrue(schema.isRef)

        val allSchemas = resolver.registry().allSchemas()
        assertTrue(allSchemas.containsKey("UserDto"))
        assertTrue(allSchemas.containsKey("PageResponse_List_UserDto"))
        assertTrue(allSchemas.containsKey("ApiResponse_PageResponse_List_UserDto"))

        // items: List<List<UserDto>> → array of array of $ref UserDto
        val pageSchema = allSchemas["PageResponse_List_UserDto"]!!
        val itemsField = pageSchema.properties!!["items"]!!
        assertEquals(SchemaType.ARRAY, itemsField.type)
        // items is List<List<UserDto>>, so items.items should be an array
        val innerArray = itemsField.items!!
        assertEquals(SchemaType.ARRAY, innerArray.type)
        assertTrue(innerArray.items!!.isRef)
        assertEquals("#/components/schemas/UserDto", innerArray.items!!.ref)
    }

    // -- Sealed class --

    sealed class Shape {
        data class Circle(val radius: Double) : Shape()
        data class Rectangle(val width: Double, val height: Double) : Shape()
    }

    @Test
    fun `resolves sealed class to oneOf`() {
        val resolver = TypeResolver()
        val schema = resolver.resolve(typeOf<Shape>())
        assertNotNull(schema.oneOf)
        assertEquals(2, schema.oneOf!!.size)
        assertNotNull(schema.discriminator)
        assertEquals("type", schema.discriminator!!.propertyName)
    }

    // -- Circular reference --

    data class TreeNode(val value: String, val children: List<TreeNode>)

    @Test
    fun `handles circular reference`() {
        val resolver = TypeResolver()
        val schema = resolver.resolve(typeOf<TreeNode>())
        assertTrue(schema.isRef)

        val allSchemas = resolver.registry().allSchemas()
        val treeSchema = allSchemas["TreeNode"]!!
        assertEquals(SchemaType.OBJECT, treeSchema.type)

        // children should be an array of $ref to TreeNode
        val childrenField = treeSchema.properties!!["children"]!!
        assertEquals(SchemaType.ARRAY, childrenField.type)
        assertTrue(childrenField.items!!.isRef)
        assertEquals("#/components/schemas/TreeNode", childrenField.items!!.ref)
    }

    // -- UUID --

    @Test
    fun `resolves UUID type`() {
        val schema = resolver.resolve(typeOf<java.util.UUID>())
        assertEquals(SchemaType.STRING, schema.type)
        assertEquals("uuid", schema.format)
    }

    // -- Constructor parameter order --

    data class OrderedClass(val third: String, val first: Int, val second: Boolean)

    @Test
    fun `preserves constructor parameter order`() {
        val resolver = TypeResolver()
        resolver.resolve(typeOf<OrderedClass>())
        val schema = resolver.registry().allSchemas()["OrderedClass"]!!
        val propertyNames = schema.properties!!.keys.toList()
        assertEquals(listOf("third", "first", "second"), propertyNames)
    }
}
