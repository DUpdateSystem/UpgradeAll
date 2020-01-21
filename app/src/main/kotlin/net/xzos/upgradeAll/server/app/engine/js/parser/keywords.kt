package net.xzos.upgradeAll.server.app.engine.js.parser

data class keywords(
        val functionKeywords: String = "function",
        val reservedWords: ArrayList<String> = arrayListOf(
                "abstract", "arguments", "boolean", "break", "byte", "case", "catch",
                "char", "class", "const", "continue", "debugger", "default", "delete",
                "do", "double", "else", "enum", "eval", "export", "extends", "false",
                "final", "finally", "float", "for", "goto", "if", "implements", "import",
                "in", "instanceof", "int", "interface", "let", "long", "native", "new",
                "null", "package", "private", "protected", "public", "return", "short",
                "static", "super", "switch", "synchronized", "this", "throw", "throws",
                "transient", "true", "try", "typeof", "var", "void", "volatile", "while",
                "with", "yield", "const"
        )
)