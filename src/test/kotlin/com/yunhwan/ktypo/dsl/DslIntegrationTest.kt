package com.yunhwan.ktypo.dsl

import com.yunhwan.ktypo.model.HttpMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DslIntegrationTest {

    data class CreateUserRequest(val name: String, val email: String, val age: Int)
    data class UserDto(val id: Long, val name: String, val email: String)
    data class ApiResponse<T>(val code: Int, val message: String, val data: T)

    @Test
    fun `ktypo DSL builds correct spec`() {
        val spec = ktypo {
            info {
                title("My API")
                version("1.0.0")
                description("Test API")
            }

            document("create-user") {
                post("/api/users") {
                    summary("Create user")
                    tags("Users")

                    requestBody<CreateUserRequest> {
                        field(CreateUserRequest::name) {
                            description("User name")
                            example("John")
                            minLength(2)
                            maxLength(50)
                        }
                        field(CreateUserRequest::email) {
                            description("Email")
                            example("john@example.com")
                        }
                    }

                    responseBody<ApiResponse<UserDto>> {
                        field("code") { description("Response code") }
                        field("data.id") { description("User ID") }
                    }
                }
            }
        }

        assertEquals("My API", spec.title)
        assertEquals("1.0.0", spec.version)
        assertEquals("Test API", spec.description)
        assertEquals(1, spec.documents.size)

        val doc = spec.documents[0]
        assertEquals("create-user", doc.identifier)
        assertEquals(HttpMethod.POST, doc.operation.method)
        assertEquals("/api/users", doc.operation.path)
        assertEquals("Create user", doc.operation.summary)
        assertEquals(listOf("Users"), doc.operation.tags)

        // Request
        val request = doc.operation.request
        assertNotNull(request)
        assertTrue(request.schema.isRef)
        assertEquals(2, request.fieldOverrides.size)
        assertTrue(request.fieldOverrides.containsKey("name"))
        assertTrue(request.fieldOverrides.containsKey("email"))

        // Response
        assertEquals(1, doc.operation.responses.size)
        val response = doc.operation.responses[0]
        assertTrue(response.schema.isRef)
        assertEquals(2, response.fieldOverrides.size)
    }

    @Test
    fun `ktypo DSL with parameters`() {
        val spec = ktypo {
            info {
                title("API")
                version("1.0.0")
            }

            document("get-user") {
                get("/api/users/{id}") {
                    summary("Get user by ID")

                    pathParameter("id") {
                        description("User ID")
                        example(1)
                    }

                    queryParameter("fields") {
                        description("Fields to include")
                        required(false)
                    }

                    headerParameter("X-Request-Id") {
                        description("Request ID")
                        required(false)
                    }

                    responseBody<UserDto>()
                }
            }
        }

        val op = spec.documents[0].operation
        assertEquals(HttpMethod.GET, op.method)
        assertEquals(3, op.parameters.size)
        assertEquals("id", op.parameters[0].name)
        assertEquals("path", op.parameters[0].location.value)
        assertEquals(true, op.parameters[0].required)
        assertEquals("fields", op.parameters[1].name)
        assertEquals("query", op.parameters[1].location.value)
        assertEquals(false, op.parameters[1].required)
    }

    @Test
    fun `ktypo DSL supports all HTTP methods`() {
        val spec = ktypo {
            info {
                title("API")
                version("1.0.0")
            }
            document("d1") { get("/a") { summary("get") } }
            document("d2") { post("/b") { summary("post") } }
            document("d3") { put("/c") { summary("put") } }
            document("d4") { delete("/d") { summary("delete") } }
            document("d5") { patch("/e") { summary("patch") } }
        }

        assertEquals(5, spec.documents.size)
        assertEquals(HttpMethod.GET, spec.documents[0].operation.method)
        assertEquals(HttpMethod.POST, spec.documents[1].operation.method)
        assertEquals(HttpMethod.PUT, spec.documents[2].operation.method)
        assertEquals(HttpMethod.DELETE, spec.documents[3].operation.method)
        assertEquals(HttpMethod.PATCH, spec.documents[4].operation.method)
    }
}
