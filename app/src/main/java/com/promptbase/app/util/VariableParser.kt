package com.promptbase.app.util

data class Variable(
    val key: String,            // Normalized (lowercase, trimmed)
    val displayName: String,    // Aesthetic capitalized / exact original parsed name
    val defaultValue: String?   // Optional pre-filled value
)

object VariableParser {

    // Regex specified in the PRD
    private val variableRegex = """\{\{\s*([^:\}]+?)\s*(?::\s*([^:\}]+?)\s*)?\}\}""".toRegex()

    /**
     * Extracts unique variables from the prompt Markdown text.
     * Keeps the first occurrence's display casing and default value if duplicate keys exist.
     */
    fun extractVariables(content: String): List<Variable> {
        val matches = variableRegex.findAll(content)
        val uniqueVars = mutableMapOf<String, Variable>()

        for (match in matches) {
            val originalName = match.groupValues[1]
            val key = originalName.trim().lowercase()
            val defaultValue = match.groupValues.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() }

            if (!uniqueVars.containsKey(key)) {
                // Friendly title-cased display label if it's all lowercase, otherwise use exact parsed name
                val displayName = originalName.trim()
                uniqueVars[key] = Variable(
                    key = key,
                    displayName = displayName,
                    defaultValue = defaultValue
                )
            } else if (defaultValue != null && uniqueVars[key]?.defaultValue == null) {
                // Update with default value if a subsequent occurrence has one and the first didn't
                val current = uniqueVars[key]!!
                uniqueVars[key] = current.copy(defaultValue = defaultValue)
            }
        }

        return uniqueVars.values.toList()
    }

    /**
     * Replaces variable tokens with their corresponding inputs.
     * Matches case-insensitively using normalized keys.
     */
    fun replaceVariables(content: String, values: Map<String, String>): String {
        return variableRegex.replace(content) { match ->
            val originalName = match.groupValues[1]
            val key = originalName.trim().lowercase()
            val defaultValue = match.groupValues.getOrNull(2)?.trim() ?: ""
            
            // Look up input value, default value, or empty string of none is provided
            val replacement = values[key]?.trim() ?: defaultValue
            replacement
        }
    }
}
