package com.dv.moneym.feature.overview.a2ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class OverviewA2UiValidatorTest {
    private val validator = OverviewA2UiValidator()

    @Test
    fun acceptsValidJson() {
        val result = validator.validate(sampleA2UiJson)

        assertIs<OverviewA2UiValidationResult.Valid>(result)
    }

    @Test
    fun rejectsUnknownComponentType() {
        val result = validator.validate("""{"type":"webView"}""")

        assertInvalid(result, OverviewA2UiValidationError.UnknownComponent)
    }

    @Test
    fun rejectsUnknownBinding() {
        val result = validator.validate("""{"type":"text","binding":"transactions.raw"}""")

        assertInvalid(result, OverviewA2UiValidationError.UnknownBinding)
    }

    @Test
    fun rejectsTooDeepTree() {
        val result = validator.validate(
            """
            {"type":"column","children":[
              {"type":"column","children":[
                {"type":"column","children":[
                  {"type":"column","children":[
                    {"type":"column","children":[
                      {"type":"text","text":"too deep"}
                    ]}
                  ]}
                ]}
              ]}
            ]}
            """.trimIndent(),
        )

        assertInvalid(result, OverviewA2UiValidationError.TooDeep)
    }

    @Test
    fun rejectsTooManyChildren() {
        val children = List(9) { """{"type":"text","text":"$it"}""" }.joinToString(",")

        val result = validator.validate("""{"type":"row","children":[$children]}""")

        assertInvalid(result, OverviewA2UiValidationError.TooManyChildren)
    }

    @Test
    fun rejectsTooManyTotalNodes() {
        val children = List(8) { index ->
            val grandchildren = List(5) { """{"type":"text","text":"$it"}""" }.joinToString(",")
            """{"type":"column","title":"$index","children":[$grandchildren]}"""
        }.joinToString(",")

        val result = validator.validate("""{"type":"column","children":[$children]}""")

        assertInvalid(result, OverviewA2UiValidationError.TooManyNodes)
    }

    @Test
    fun rejectsCodeLikeFields() {
        val result = validator.validate("""{"type":"text","script":"fetch()","text":"bad"}""")

        assertInvalid(result, OverviewA2UiValidationError.UnsafeField)
    }

    private fun assertInvalid(result: OverviewA2UiValidationResult, error: OverviewA2UiValidationError) {
        val invalid = assertIs<OverviewA2UiValidationResult.Invalid>(result)
        assertEquals(error, invalid.reason)
    }
}
