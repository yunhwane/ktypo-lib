package com.yunhwan.ktypo.openapi

import com.yunhwan.ktypo.dsl.ktypo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OpenApiGeneratorTest {

    data class CreateUserRequest(val name: String, val email: String)
    data class UserDto(val id: Long, val name: String, val email: String)
    data class ApiResponse<T>(val code: Int, val message: String, val data: T)

    @Test
    fun `generates valid OpenAPI spec`() {
        val spec = ktypo {
            info {
                title("Test API")
                version("2.0.0")
            }

            document("create-user") {
                post("/api/users") {
                    summary("Create user")
                    tags("Users")

                    requestBody<CreateUserRequest> {
                        field("name") { description("Name") }
                    }

                    responseBody<ApiResponse<UserDto>> {
                        field("code") { description("Response code") }
                    }
                }
            }
        }

        val openApi = spec.generateOpenApi()
        assertEquals("3.1.0", openApi.openapi)
        assertEquals("Test API", openApi.info.title)
        assertEquals("2.0.0", openApi.info.version)

        // Paths
        assertNotNull(openApi.paths)
        val pathItem = openApi.paths["/api/users"]
        assertNotNull(pathItem)
        assertNotNull(pathItem.post)
        assertEquals("Create user", pathItem.post.summary)
        assertEquals(listOf("Users"), pathItem.post.tags)

        // Request body
        assertNotNull(pathItem.post.requestBody)
        assertNotNull(pathItem.post.requestBody.content["application/json"])

        // Responses
        assertNotNull(pathItem.post.responses["200"])

        // Component schemas
        assertNotNull(openApi.components)
        assertTrue(openApi.components.schemas.isNotEmpty())
    }

    @Test
    fun `generates JSON output`() {
        val spec = ktypo {
            info {
                title("Test API")
                version("1.0.0")
            }

            document("get-users") {
                get("/api/users") {
                    summary("List users")
                    responseBody<List<UserDto>>()
                }
            }
        }

        val json = spec.toJson()
        assertNotNull(json)
        assertTrue(json.contains("\"openapi\""))
        assertTrue(json.contains("3.1.0"))
        assertTrue(json.contains("Test API"))
    }

    @Test
    fun `generates YAML output`() {
        val spec = ktypo {
            info {
                title("Test API")
                version("1.0.0")
            }

            document("get-users") {
                get("/api/users") {
                    summary("List users")
                    responseBody<List<UserDto>>()
                }
            }
        }

        val yaml = spec.toYaml()
        assertNotNull(yaml)
        assertTrue(yaml.contains("openapi"))
        assertTrue(yaml.contains("3.1.0"))
        assertTrue(yaml.contains("Test API"))
    }

    @Test
    fun `generates spec with parameters`() {
        val spec = ktypo {
            info {
                title("Test API")
                version("1.0.0")
            }

            document("get-user") {
                get("/api/users/{id}") {
                    summary("Get user")

                    pathParameter("id") {
                        description("User ID")
                    }

                    responseBody<UserDto>()
                }
            }
        }

        val openApi = spec.generateOpenApi()
        val pathItem = openApi.paths["/api/users/{id}"]
        assertNotNull(pathItem)
        val params = pathItem.get.parameters
        assertNotNull(params)
        assertEquals(1, params.size)
        assertEquals("id", params[0].name)
        assertEquals("path", params[0].`in`)
        assertEquals("User ID", params[0].description)
    }
}
