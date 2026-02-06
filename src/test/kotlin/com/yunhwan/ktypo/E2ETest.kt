package com.yunhwan.ktypo

import com.yunhwan.ktypo.config.KtypoConfig
import com.yunhwan.ktypo.dsl.ktypo
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class E2ETest {

    // Domain models for E2E test
    data class CreateUserRequest(
        val name: String,
        val email: String,
        val age: Int,
        val role: UserRole,
    )

    enum class UserRole { ADMIN, USER, GUEST }

    data class UserDto(
        val id: Long,
        val name: String,
        val email: String,
        val age: Int,
        val role: UserRole,
        val createdAt: String,
    )

    data class ApiResponse<T>(
        val code: Int,
        val message: String,
        val data: T,
    )

    data class PageResponse<T>(
        val items: List<T>,
        val totalCount: Long,
        val page: Int,
        val size: Int,
        val hasNext: Boolean,
    )

    @Test
    fun `full pipeline with complex generic type ApiResponse of PageResponse of List of UserDto`() {
        val outputDir = File("build/test-output/e2e-docs")
        val snippetDir = File("build/test-output/e2e-snippets")

        val spec = ktypo {
            info {
                title("User Management API")
                version("1.0.0")
                description("E2E test API")
            }

            config {
                outputDir(outputDir)
                snippetDir(snippetDir)
                format(KtypoConfig.OutputFormat.BOTH)
            }

            document("create-user") {
                post("/api/users") {
                    summary("Create user")
                    tags("Users")

                    requestBody<CreateUserRequest> {
                        field(CreateUserRequest::name) {
                            description("User name")
                            example("Hong Gil-Dong")
                            minLength(2)
                            maxLength(50)
                        }
                        field(CreateUserRequest::email) {
                            description("Email address")
                            example("hong@example.com")
                        }
                        field(CreateUserRequest::age) {
                            description("User age")
                            example(25)
                            minimum(0)
                            maximum(200)
                        }
                    }

                    responseBody<ApiResponse<UserDto>> {
                        field("code") { description("Response code") }
                        field("message") { description("Response message") }
                        field("data.id") { description("User ID") }
                        field("data.name") { description("User name") }
                    }
                }
            }

            document("list-users") {
                get("/api/users") {
                    summary("List users with pagination")
                    tags("Users")

                    queryParameter("page") {
                        description("Page number")
                        required(false)
                        example(0)
                    }

                    queryParameter("size") {
                        description("Page size")
                        required(false)
                        example(20)
                    }

                    responseBody<ApiResponse<PageResponse<List<UserDto>>>> {
                        field("code") { description("Response code") }
                        field("data.totalCount") { description("Total count") }
                    }
                }
            }

            document("get-user") {
                get("/api/users/{id}") {
                    summary("Get user by ID")
                    tags("Users")

                    pathParameter("id") {
                        description("User ID")
                        example(1)
                    }

                    responseBody<ApiResponse<UserDto>> {
                        field("code") { description("Response code") }
                    }
                }
            }

            document("delete-user") {
                delete("/api/users/{id}") {
                    summary("Delete user")
                    tags("Users")

                    pathParameter("id") {
                        description("User ID")
                    }
                }
            }
        }

        // Generate files
        spec.generate()

        // Verify OpenAPI JSON
        val jsonFile = File(outputDir, "openapi.json")
        assertTrue(jsonFile.exists(), "openapi.json should exist")
        val jsonContent = jsonFile.readText()
        assertTrue(jsonContent.contains("\"openapi\""), "Should contain openapi field")
        assertTrue(jsonContent.contains("3.1.0"), "Should be version 3.1.0")
        assertTrue(jsonContent.contains("User Management API"), "Should contain API title")
        assertTrue(jsonContent.contains("/api/users"), "Should contain path")
        assertTrue(jsonContent.contains("Create user"), "Should contain operation summary")

        // Verify OpenAPI YAML
        val yamlFile = File(outputDir, "openapi.yaml")
        assertTrue(yamlFile.exists(), "openapi.yaml should exist")
        val yamlContent = yamlFile.readText()
        assertTrue(yamlContent.contains("openapi"), "YAML should contain openapi")
        assertTrue(yamlContent.contains("3.1.0"), "YAML should be version 3.1.0")

        // Verify RestDocs snippets
        val createUserSnippets = File(snippetDir, "create-user")
        assertTrue(createUserSnippets.exists(), "create-user snippet dir should exist")
        assertTrue(File(createUserSnippets, "request-fields.adoc").exists(), "request-fields.adoc should exist")
        assertTrue(File(createUserSnippets, "response-fields.adoc").exists(), "response-fields.adoc should exist")

        val requestFieldsContent = File(createUserSnippets, "request-fields.adoc").readText()
        assertTrue(requestFieldsContent.contains("name"), "Should contain name field")
        assertTrue(requestFieldsContent.contains("email"), "Should contain email field")
        assertTrue(requestFieldsContent.contains("age"), "Should contain age field")

        val responseFieldsContent = File(createUserSnippets, "response-fields.adoc").readText()
        assertTrue(responseFieldsContent.contains("code"), "Should contain code field")
        assertTrue(responseFieldsContent.contains("message"), "Should contain message field")
        assertTrue(responseFieldsContent.contains("data"), "Should contain data field")

        // Verify list-users snippets
        val listUsersSnippets = File(snippetDir, "list-users")
        assertTrue(listUsersSnippets.exists(), "list-users snippet dir should exist")
        assertTrue(File(listUsersSnippets, "query-parameters.adoc").exists(), "query-parameters.adoc should exist")

        // Verify get-user snippets
        val getUserSnippets = File(snippetDir, "get-user")
        assertTrue(getUserSnippets.exists(), "get-user snippet dir should exist")
        assertTrue(File(getUserSnippets, "path-parameters.adoc").exists(), "path-parameters.adoc should exist")
        assertTrue(File(getUserSnippets, "response-fields.adoc").exists(), "response-fields.adoc should exist")

        // Print generated YAML for visual inspection
        println("=== Generated OpenAPI YAML ===")
        println(yamlContent)

        // Print snippets
        println("\n=== RestDocs: create-user/request-fields.adoc ===")
        println(requestFieldsContent)
        println("\n=== RestDocs: create-user/response-fields.adoc ===")
        println(responseFieldsContent)

        // Cleanup
        outputDir.deleteRecursively()
        snippetDir.deleteRecursively()
    }

    @Test
    fun `toJson and toYaml produce valid output`() {
        val spec = ktypo {
            info {
                title("Simple API")
                version("1.0.0")
            }

            document("hello") {
                get("/hello") {
                    summary("Say hello")
                    responseBody<ApiResponse<String>>()
                }
            }
        }

        val json = spec.toJson()
        assertNotNull(json)
        assertTrue(json.contains("Simple API"))
        assertTrue(json.contains("3.1.0"))
        assertTrue(json.contains("/hello"))

        val yaml = spec.toYaml()
        assertNotNull(yaml)
        assertTrue(yaml.contains("Simple API"))
        assertTrue(yaml.contains("3.1.0"))
    }
}
