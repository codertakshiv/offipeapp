package com.offipe.app.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withTimeout

class ActionRunnerLookupTest : FunSpec({

    test("lookup submits only the mobile value then dismisses and cancels") {
        val frames = MutableSharedFlow<UssdFrame>(extraBufferCapacity = 4)
        val lookupReplies = mutableListOf<String>()
        var genericReplies = 0
        var lookupDismissals = 0
        var cancellations = 0
        val cancelled = CompletableDeferred<Unit>()
        val engine = object : UssdEnginePort {
            override suspend fun dial(code: String) { code shouldBe "*99*1*1#" }
            override suspend fun sendReply(reply: String): Boolean {
                genericReplies++
                return true
            }
            override suspend fun sendLookupReply(mobile: String): Boolean {
                lookupReplies += mobile
                return true
            }
            override suspend fun cancel() {
                cancellations++
                cancelled.complete(Unit)
            }
            override suspend fun dismissDialog(): Boolean = true
            override suspend fun dismissLookupDialog(): Boolean {
                lookupDismissals++
                return true
            }
            override fun getSessionId(): Int = 42
            override fun isServiceEnabled(): Boolean = true
            override val frames: SharedFlow<UssdFrame> = frames
        }
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val run = ActionRunner(engine).runAction(
            action = Actions.LookupMobile,
            vars = mapOf("mobile" to "9876543210"),
            scope = scope
        )

        frames.emit(
            UssdFrame(
                text = "Enter Mobile No.\nOr 00.Bac",
                isMenu = true,
                isTerminal = false,
                sessionId = 42,
                frameId = 1
            )
        )
        frames.emit(
            UssdFrame(
                text = "Please wait",
                isMenu = true,
                isTerminal = false,
                sessionId = 42,
                frameId = 2
            )
        )
        frames.emit(
            UssdFrame(
                text = "Paying PREMAVATIDEVI ,\nEnter Amount in Rs.\n or 00.Back",
                isMenu = true,
                isTerminal = false,
                sessionId = 42,
                frameId = 3
            )
        )

        val result = withTimeout(1_000) { run.result.await() }
        withTimeout(1_000) { cancelled.await() }
        scope.cancel()

        result.success shouldBe true
        lookupReplies shouldBe listOf("9876543210")
        genericReplies shouldBe 0
        lookupDismissals shouldBe 2
        cancellations shouldBe 1
    }

    test("failed mobile submission cancels without another reply") {
        val frames = MutableSharedFlow<UssdFrame>(extraBufferCapacity = 2)
        var genericReplies = 0
        var lookupDismissals = 0
        var cancellations = 0
        val cancelled = CompletableDeferred<Unit>()
        val engine = object : UssdEnginePort {
            override suspend fun dial(code: String) {}
            override suspend fun sendReply(reply: String): Boolean {
                genericReplies++
                return true
            }
            override suspend fun sendLookupReply(mobile: String): Boolean = false
            override suspend fun cancel() {
                cancellations++
                cancelled.complete(Unit)
            }
            override suspend fun dismissDialog(): Boolean = true
            override suspend fun dismissLookupDialog(): Boolean {
                lookupDismissals++
                return true
            }
            override fun getSessionId(): Int = 7
            override fun isServiceEnabled(): Boolean = true
            override val frames: SharedFlow<UssdFrame> = frames
        }
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val run = ActionRunner(engine).runAction(
            action = Actions.LookupMobile,
            vars = mapOf("mobile" to "9876543210"),
            scope = scope
        )

        frames.emit(
            UssdFrame(
                text = "Enter Mobile No.\nOr 00.Bac",
                isMenu = true,
                isTerminal = false,
                sessionId = 7,
                frameId = 1
            )
        )
        val result = withTimeout(1_000) { run.result.await() }
        withTimeout(1_000) { cancelled.await() }
        scope.cancel()

        result.success shouldBe false
        genericReplies shouldBe 0
        lookupDismissals shouldBe 2
        cancellations shouldBe 1
    }
})
