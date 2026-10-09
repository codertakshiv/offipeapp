package com.offipe.app.domain

import kotlinx.coroutines.flow.SharedFlow

/**
 * Port interface for the USSD engine, allowing ActionRunner to remain
 * a pure domain class with no Android framework dependencies.
 */
interface UssdEnginePort {
    /** Dial a USSD code (e.g. "*99*1*3#") via ACTION_CALL intent. */
    suspend fun dial(code: String)

    /** Send a reply string to the active carrier dialog. Returns true if successful. */
    suspend fun sendReply(reply: String): Boolean

    /** Sends only the mobile number through the lookup prompt's exact Send button. */
    suspend fun sendLookupReply(mobile: String): Boolean = sendReply(mobile)

    /** Cancel the active USSD session and dismiss the carrier dialog. */
    suspend fun cancel()

    /** Dismiss any leftover carrier dialog from a prior session. Returns true if dismissed. */
    suspend fun dismissDialog(): Boolean

    /** Clear the active input and click the exact Cancel button for mobile lookup. */
    suspend fun dismissLookupDialog(): Boolean = dismissDialog()

    /** Get the current session ID. */
    fun getSessionId(): Int

    /** Check if the AccessibilityService is currently enabled and running. */
    fun isServiceEnabled(): Boolean

    /** Reactive stream of USSD frames captured from the carrier dialog. */
    val frames: SharedFlow<UssdFrame>
}