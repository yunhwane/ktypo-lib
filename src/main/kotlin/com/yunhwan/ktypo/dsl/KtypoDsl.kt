package com.yunhwan.ktypo.dsl

import com.yunhwan.ktypo.KtypoSpec
import com.yunhwan.ktypo.config.KtypoConfig
import com.yunhwan.ktypo.model.DocumentModel
import com.yunhwan.ktypo.model.ResponseModel
import com.yunhwan.ktypo.schema.SchemaRegistry
import com.yunhwan.ktypo.schema.TypeResolver

@DslMarker
annotation class KtypoDslMarker

fun ktypo(block: KtypoBuilder.() -> Unit): KtypoSpec {
    val builder = KtypoBuilder()
    builder.block()
    return builder.build()
}

@KtypoDslMarker
class KtypoBuilder {
    private var title: String = "API Documentation"
    private var version: String = "1.0.0"
    private var description: String? = null
    private val documents = mutableListOf<DocumentModel>()
    private val configBuilder = KtypoConfig.Builder()
    private val typeResolver = TypeResolver()
    private var commonResponses: List<ResponseModel> = emptyList()

    fun info(block: InfoBuilder.() -> Unit) {
        val info = InfoBuilder()
        info.block()
        title = info.title
        version = info.version
        description = info.description
    }

    fun config(block: KtypoConfig.Builder.() -> Unit) {
        configBuilder.block()
    }

    fun commonResponses(block: CommonResponsesBuilder.() -> Unit) {
        val builder = CommonResponsesBuilder(typeResolver)
        builder.block()
        commonResponses = builder.build()
    }

    fun document(identifier: String, block: DocumentBuilder.() -> Unit) {
        val builder = DocumentBuilder(identifier, typeResolver, commonResponses)
        builder.block()
        documents.add(builder.build())
    }

    fun build(): KtypoSpec = KtypoSpec(
        title = title,
        version = version,
        description = description,
        documents = documents.toList(),
        config = configBuilder.build(),
        registry = typeResolver.registry(),
    )
}

@KtypoDslMarker
class InfoBuilder {
    var title: String = "API Documentation"
        private set
    var version: String = "1.0.0"
        private set
    var description: String? = null
        private set

    fun title(value: String) { title = value }
    fun version(value: String) { version = value }
    fun description(value: String) { description = value }
}
