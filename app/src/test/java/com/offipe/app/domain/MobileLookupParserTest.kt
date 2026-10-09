package com.offipe.app.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MobileLookupParserTest : FunSpec({

    val mobilePrompt = "Enter Mobile No.\nOr 00.Bac"
    val linkedDialog = "Paying PREMAVATIDEVI ,\nEnter Amount in Rs.\n or 00.Back"
    val notLinkedDialog = "the entered UPI ID is invalid. Please enter correct UPI ID"

    test("normalizes and validates mobile numbers") {
        MobileLookupParser.normalizeMobile("+91 98765 43210") shouldBe "9876543210"
        MobileLookupParser.normalizeMobile("09876543210") shouldBe "9876543210"
        MobileLookupParser.isValidMobile("9876543210") shouldBe true
        MobileLookupParser.isValidMobile("+91 98765 43210") shouldBe true
        MobileLookupParser.isValidMobile("5987654321") shouldBe false
        MobileLookupParser.isValidMobile("987654321") shouldBe false
        MobileLookupParser.isValidMobile("98765432101") shouldBe false
    }

    test("matches the captured mobile prompt") {
        MobileLookupParser.MOBILE_PROMPT.containsMatchIn(mobilePrompt) shouldBe true
    }

    test("classifies the captured linked dialog without trailing name whitespace") {
        val result = MobileLookupParser.classify(linkedDialog)
        result shouldBe MobileLookupResult.Linked("PREMAVATIDEVI")
        (result as MobileLookupResult.Linked).name.endsWith(' ') shouldBe false
    }

    test("classifies the captured unlinked dialog before generic failures") {
        MobileLookupParser.NOT_LINKED.containsMatchIn(notLinkedDialog) shouldBe true
        MobileLookupParser.classify(notLinkedDialog) shouldBe MobileLookupResult.NotLinked
        Actions.LookupMobile.failurePatterns.any { it.containsMatchIn(notLinkedDialog) } shouldBe true
    }

    test("unknown carrier text is classified as failed") {
        MobileLookupParser.classify("Please wait while processing") shouldBe MobileLookupResult.Failed
    }

    test("lookup action ignores intermediate frames until a known result appears") {
        Actions.LookupMobile.steps[1].match.containsMatchIn("Please wait") shouldBe false
        Actions.LookupMobile.steps[1].match.containsMatchIn(linkedDialog) shouldBe true
        Actions.LookupMobile.failurePatterns.any { it.containsMatchIn(notLinkedDialog) } shouldBe true
    }

    test("classification tolerates capitalization and extra whitespace") {
        MobileLookupParser.MOBILE_PROMPT.containsMatchIn(" ENTER   mobile No. ") shouldBe true
        MobileLookupParser.classify(
            "pAyInG   PREMAVATIDEVI   ,\n ENTER   AMOUNT in Rs."
        ) shouldBe MobileLookupResult.Linked("PREMAVATIDEVI")
        MobileLookupParser.classify(
            "The ENTERED   UPI ID is INVALID. Please enter correct UPI ID"
        ) shouldBe MobileLookupResult.NotLinked
    }

    test("sanitizes carrier names before they can be displayed") {
        MobileLookupParser.sanitizeName("  {PREMA}\nVATI DEVI}  ") shouldBe "PREMA VATI DEVI"
        MobileLookupParser.sanitizeName("A".repeat(45)).length shouldBe 40
        MobileLookupParser.titleCaseName("PREMAVATIDEVI") shouldBe "Premavatidevi"
    }
})