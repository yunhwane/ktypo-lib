package com.yunhwan.ktypo.restdocs

import com.yunhwan.ktypo.schema.SchemaObject
import com.yunhwan.ktypo.schema.SchemaRegistry
import com.yunhwan.ktypo.schema.SchemaType
import com.yunhwan.ktypo.schema.TypeResolver
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RestDocsGeneratorTest {

    data class UserDto(val id: Long, val name: String, val email: String?)

    @Test
    fun `FieldFlattener flattens simple data class`() {
        val resolver = TypeResolver()
        val schema = resolver.resolve(typeOf<UserDto>())

        val flattener = FieldFlattener(resolver.registry())
        val resolvedSchema = resolver.registry().allSchemas()["UserDto"]!!
        val fields = flattener.flatten(resolvedSchema)

        assertEquals(3, fields.size)
        assertEquals("id", fields[0].path)
        assertEquals("Integer", fields[0].type)
        assertEquals(false, fields[0].optional)

        assertEquals("name", fields[1].path)
        assertEquals("String", fields[1].type)
        assertEquals(false, fields[1].optional)

        assertEquals("email", fields[2].path)
        assertEquals("String", fields[2].type)
        assertEquals(true, fields[2].optional)
    }

    data class ApiResponse<T>(val code: Int, val message: String, val data: T)

    @Test
    fun `FieldFlattener flattens nested generic type`() {
        val resolver = TypeResolver()
        resolver.resolve(typeOf<ApiResponse<UserDto>>())

        val flattener = FieldFlattener(resolver.registry())
        val schema = resolver.registry().allSchemas()["ApiResponse_UserDto"]!!
        val fields = flattener.flatten(schema)

        // Should have: code, message, data, data.id, data.name, data.email
        val paths = fields.map { it.path }
        assertTrue("code" in paths)
        assertTrue("message" in paths)
        assertTrue("data" in paths)
        assertTrue("data.id" in paths)
        assertTrue("data.name" in paths)
        assertTrue("data.email" in paths)
    }

    data class PageResponse<T>(val items: List<T>, val totalCount: Long, val page: Int)

    @Test
    fun `FieldFlattener flattens array items with bracket notation`() {
        val resolver = TypeResolver()
        resolver.resolve(typeOf<PageResponse<UserDto>>())

        val flattener = FieldFlattener(resolver.registry())
        val schema = resolver.registry().allSchemas()["PageResponse_UserDto"]!!
        val fields = flattener.flatten(schema)

        val paths = fields.map { it.path }
        assertTrue("items" in paths)
        assertTrue("items[].id" in paths)
        assertTrue("items[].name" in paths)
        assertTrue("items[].email" in paths)
        assertTrue("totalCount" in paths)
        assertTrue("page" in paths)
    }

    @Test
    fun `SnippetTemplates generates valid Asciidoc request-fields table`() {
        val fields = listOf(
            FlatField("name", "String", "User name", false),
            FlatField("email", "String", "Email address", true),
        )
        val result = SnippetTemplates.requestFieldsTable(fields)

        assertTrue(result.contains(".Request Fields"))
        assertTrue(result.contains("|==="))
        assertTrue(result.contains("|`name`"))
        assertTrue(result.contains("|`String`"))
        assertTrue(result.contains("|User name"))
        assertTrue(result.contains("|`email`"))
        assertTrue(result.contains("|Email address"))
    }

    @Test
    fun `SnippetTemplates generates valid Asciidoc response-fields table`() {
        val fields = listOf(
            FlatField("id", "Integer", "User ID", false),
        )
        val result = SnippetTemplates.responseFieldsTable(fields)

        assertTrue(result.contains(".Response Fields"))
        assertTrue(result.contains("|`id`"))
        assertTrue(result.contains("|`Integer`"))
    }

    @Test
    fun `SnippetTemplates returns empty for empty fields`() {
        val result = SnippetTemplates.requestFieldsTable(emptyList())
        assertEquals("", result)
    }
}
