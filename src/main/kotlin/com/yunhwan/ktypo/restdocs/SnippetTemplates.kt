package com.yunhwan.ktypo.restdocs

import com.yunhwan.ktypo.model.ParameterModel

object SnippetTemplates {

    fun requestFieldsTable(fields: List<FlatField>): String =
        fieldsTable("Request Fields", fields)

    fun responseFieldsTable(fields: List<FlatField>): String =
        fieldsTable("Response Fields", fields)

    private fun fieldsTable(title: String, fields: List<FlatField>): String {
        if (fields.isEmpty()) return ""

        val sb = StringBuilder()
        sb.appendLine(".$title")
        sb.appendLine("|===")
        sb.appendLine("|Path|Type|Description|Optional")
        sb.appendLine()
        for (field in fields) {
            sb.appendLine("|`${field.path}`")
            sb.appendLine("|`${field.type}`")
            sb.appendLine("|${field.description}")
            sb.appendLine("|${field.optional}")
            sb.appendLine()
        }
        sb.appendLine("|===")
        return sb.toString()
    }

    fun parametersTable(title: String, parameters: List<ParameterModel>): String {
        if (parameters.isEmpty()) return ""

        val sb = StringBuilder()
        sb.appendLine(".$title")
        sb.appendLine("|===")
        sb.appendLine("|Name|Description|Required")
        sb.appendLine()
        for (param in parameters) {
            sb.appendLine("|`${param.name}`")
            sb.appendLine("|${param.description ?: ""}")
            sb.appendLine("|${param.required}")
            sb.appendLine()
        }
        sb.appendLine("|===")
        return sb.toString()
    }
}
