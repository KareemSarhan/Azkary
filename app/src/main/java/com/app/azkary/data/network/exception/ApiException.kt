package com.app.azkary.data.network.exception

/**
 * Base exception for API-related errors
 */
open class ApiException(
    message: String,
    cause: Throwable? = null,
    val httpCode: Int? = null
) : Exception(message, cause)