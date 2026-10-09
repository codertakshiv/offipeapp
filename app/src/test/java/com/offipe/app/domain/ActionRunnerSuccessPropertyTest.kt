package com.offipe.app.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Property-based tests for ActionRunner.matchesUniversalSuccess() correctness.
 *
 * **Validates: Requirements 2.3**
 */
class ActionRunnerSuccessPropertyTest : FunSpec({

    tags(io.kotest.core.Tag("Feature: offipe-native-app, Property 5: Universal Success Detection"))

    // Fake UssdEnginePort for creating ActionRunner instance
    val fakeEngine = object : UssdEnginePort {
        override suspend fun dial(code: String) {}
        override suspend fun sendReply(reply: String): Boolean = true
        override suspend fun cancel() {}
        override suspend fun dismissDialog(): Boolean = true
        override fun getSessionId(): Int = 0
        override fun isServiceEnabled(): Boolean = true
        override val frames: SharedFlow<UssdFrame> = MutableSharedFlow()
    }

    val runner = ActionRunner(fakeEngine)

    // Generator for random context text (prefix/suffix around the pattern)
    val contextArb = Arb.string(0..30)

    // Universal success pattern snippets
    val successPatternArb = arbitrary {
        val patternIndex = Arb.int(0..3).bind()
        when (patternIndex) {
            0 -> "is successful"
            1 -> {
                val verb = Arb.element("sent", "paid", "completed").bind()
                "successfully $verb"
            }
            2 -> "transaction successful"
            3 -> "payment successful"
            else -> "is successful"
        }
    }

    test("matchesUniversalSuccess returns true for strings containing a success pattern") {
        checkAll(100, contextArb, successPatternArb, contextArb) { prefix, pattern, suffix ->
            val text = "$prefix $pattern $suffix"
            runner.matchesUniversalSuccess(text) shouldBe true
        }
    }

    test("matchesUniversalSuccess returns false for strings without any success pattern") {
        // Generate strings that definitely don't contain any success patterns
        val safeStringsArb = Arb.element(
            "please enter your pin",
            "enter amount",
            "select your bank",
            "wrong pin entered",
            "insufficient balance",
            "hello world 12345",
            "some random carrier text",
            "menu option 1 2 3",
            "enter vpa address",
            "confirm your details",
            "processing your request",
            "please wait",
            "try again later",
            "service unavailable",
            "invalid input provided",
            "account number 12345",
            "ref 12345",
            "abcdef ghijkl mnopqr",
            "carrier response text here",
            "dial *99# for more"
        )

        checkAll(100, safeStringsArb) { text ->
            runner.matchesUniversalSuccess(text) shouldBe false
        }
    }

    test("reference numbers alone do not indicate success") {
        runner.matchesUniversalSuccess("reference no: 123456") shouldBe false
        runner.matchesUniversalSuccess("ref: 987654321") shouldBe false
    }

    test("fillTemplate replaces placeholders in one pass") {
        runner.fillTemplate(
            "Note {note}; PIN {pin}",
            mapOf("note" to "{pin}", "pin" to "123456")
        ) shouldBe "Note {pin}; PIN 123456"
    }

    test("fillTemplate substitutes the mobile lookup variable") {
        runner.fillTemplate("{mobile}", mapOf("mobile" to "9876543210")) shouldBe "9876543210"
    }

    test("LookupMobile can only send the mobile variable") {
        val replies = Actions.LookupMobile.steps.mapNotNull { it.reply }
        replies shouldBe listOf("{mobile}")
        replies.any { "{amount}" in it || "{note}" in it || "{pin}" in it } shouldBe false
    }
})
