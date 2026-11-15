package com.galaxyrio.gracelauncher.data

import org.junit.Assert.assertThrows
import org.junit.Test

class BackupJsonLimitsTest {
    @Test fun bracesAndEscapedQuotesInsideStringsDoNotCountAsStructure() {
        BackupJsonLimits.checkNesting("""{"label":"{[\\\"value\\\"]}","entries":{}}""")
    }
    @Test fun deepNestingIsRejectedBeforeRecursiveParsing() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupJsonLimits.checkNesting("[".repeat(17) + "]".repeat(17))
        }
    }
    @Test fun truncatedAndMismatchedStructuresAreRejected() {
        for (input in listOf("{", "{]", "\"unterminated")) {
            assertThrows(IllegalArgumentException::class.java) { BackupJsonLimits.checkNesting(input) }
        }
    }
}
