package com.galaxyrio.gracelauncher.data

/** Bound nesting before invoking Android's recursive JSON parser on untrusted data. */
internal object BackupJsonLimits {
    fun checkNesting(value: String, maxDepth: Int = 16) {
        require(maxDepth in 1..64)
        val stack = mutableListOf<Char>()
        var quoted = false
        var escaped = false
        for (character in value) {
            if (quoted) {
                if (escaped) escaped = false
                else if (character == '\\') escaped = true
                else if (character == '"') quoted = false
                continue
            }
            when (character) {
                '"' -> quoted = true
                '{', '[' -> {
                    require(stack.size < maxDepth) { "Backup JSON nesting exceeds limit" }
                    stack += character
                }
                '}', ']' -> {
                    require(stack.isNotEmpty() && stack.removeAt(stack.lastIndex) ==
                        if (character == '}') '{' else '[') { "Malformed backup JSON" }
                }
            }
        }
        require(!quoted && stack.isEmpty()) { "Incomplete backup JSON" }
    }
}
