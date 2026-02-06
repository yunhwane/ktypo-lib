package com.yunhwan.ktypo.dsl

import com.yunhwan.ktypo.model.HttpMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommonResponsesTest {

    data class ErrorResponse(val code: Int, val message: String)
    data class UserDto(val id: Long, val name: String)
    data class ApiResponse<T>(val code: Int, val data: T)

    @Test
    fun `commonResponses are automatically included in all operations`() {
        val spec = ktypo {
            info { title("API"); version("1.0.0") }

            commonResponses {
                response<ErrorResponse>(400) { description("Bad Request") }
                response(401, "Unauthorized")
                response<ErrorResponse>(500) { description("Internal Server Error") }
            }

            document("create-user") {
                post("/api/users") {
                    responseBody<ApiResponse<UserDto>> { description("Success") }
                }
            }
        }

        val responses = spec.documents[0].operation.responses
        assertEquals(4, responses.size)

        val statusCodes = responses.map { it.statusCode }.toSet()
        assertTrue(statusCodes.containsAll(setOf(200, 400, 401, 500)))

        // 200 is from the operation itself
        val ok = responses.first { it.statusCode == 200 }
        assertNotNull(ok.schema)

        // 400 has body (ErrorResponse)
        val badRequest = responses.first { it.statusCode == 400 }
        assertEquals("Bad Request", badRequest.description)
        assertNotNull(badRequest.schema)

        // 401 is description-only
        val unauthorized = responses.first { it.statusCode == 401 }
        assertEquals("Unauthorized", unauthorized.description)
        assertNull(unauthorized.schema)

        // 500 has body (ErrorResponse)
        val serverError = responses.first { it.statusCode == 500 }
        assertEquals("Internal Server Error", serverError.description)
        assertNotNull(serverError.schema)
    }

    @Test
    fun `excludeCommonResponses removes specific common responses`() {
        val spec = ktypo {
            info { title("API"); version("1.0.0") }

            commonResponses {
                response(401, "Unauthorized")
                response(403, "Forbidden")
                response<ErrorResponse>(500) { description("Internal Server Error") }
            }

            document("public-endpoint") {
                get("/api/public") {
                    excludeCommonResponses(401, 403)
                    responseBody<ApiResponse<String>> { description("OK") }
                }
            }
        }

        val responses = spec.documents[0].operation.responses
        assertEquals(2, responses.size)

        val statusCodes = responses.map { it.statusCode }.toSet()
        assertEquals(setOf(200, 500), statusCodes)
    }

    @Test
    fun `operation response overrides common response with same status code`() {
        val spec = ktypo {
            info { title("API"); version("1.0.0") }

            commonResponses {
                response<ErrorResponse>(400) { description("Common Bad Request") }
                response<ErrorResponse>(500) { description("Internal Server Error") }
            }

            document("create-user") {
                post("/api/users") {
                    responseBody<ApiResponse<UserDto>> {
                        statusCode(400)
                        description("Validation Error")
                    }
                }
            }
        }

        val responses = spec.documents[0].operation.responses
        // 400 from operation + 500 from common = 2
        assertEquals(2, responses.size)

        val badRequest = responses.first { it.statusCode == 400 }
        assertEquals("Validation Error", badRequest.description)

        val serverError = responses.first { it.statusCode == 500 }
        assertEquals("Internal Server Error", serverError.description)
    }

    @Test
    fun `works without commonResponses defined`() {
        val spec = ktypo {
            info { title("API"); version("1.0.0") }

            document("simple") {
                get("/api/simple") {
                    responseBody<ApiResponse<String>> { description("OK") }
                }
            }
        }

        val responses = spec.documents[0].operation.responses
        assertEquals(1, responses.size)
        assertEquals(200, responses[0].statusCode)
    }

    @Test
    fun `commonResponses applied to multiple documents`() {
        val spec = ktypo {
            info { title("API"); version("1.0.0") }

            commonResponses {
                response(401, "Unauthorized")
                response<ErrorResponse>(500) { description("Internal Server Error") }
            }

            document("doc1") {
                get("/api/a") {
                    responseBody<ApiResponse<String>> {}
                }
            }

            document("doc2") {
                post("/api/b") {
                    responseBody<ApiResponse<UserDto>> {}
                }
            }
        }

        for (doc in spec.documents) {
            val statusCodes = doc.operation.responses.map { it.statusCode }.toSet()
            assertTrue(statusCodes.contains(401), "Document '${doc.identifier}' should have 401")
            assertTrue(statusCodes.contains(500), "Document '${doc.identifier}' should have 500")
        }
    }

    @Test
    fun `openapi output includes common responses`() {
        val spec = ktypo {
            info { title("API"); version("1.0.0") }

            commonResponses {
                response<ErrorResponse>(400) { description("Bad Request") }
                response(401, "Unauthorized")
            }

            document("test") {
                get("/api/test") {
                    responseBody<ApiResponse<String>> { description("OK") }
                }
            }
        }

        val json = spec.toJson()
        assertTrue(json.contains("\"400\""), "JSON should contain 400 response")
        assertTrue(json.contains("\"401\""), "JSON should contain 401 response")
        assertTrue(json.contains("Bad Request"), "JSON should contain Bad Request description")
        assertTrue(json.contains("Unauthorized"), "JSON should contain Unauthorized description")
    }
}
