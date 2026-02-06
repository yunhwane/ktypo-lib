package com.yunhwan.ktypo.model

data class OperationModel(
    val method: HttpMethod,
    val path: String,
    val summary: String? = null,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val operationId: String? = null,
    val request: RequestModel? = null,
    val responses: List<ResponseModel> = emptyList(),
    val parameters: List<ParameterModel> = emptyList(),
    val deprecated: Boolean = false,
)

enum class HttpMethod {
    GET, POST, PUT, DELETE, PATCH
}
