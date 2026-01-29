package com.app.azkary.data.network.exception

/**
 * Exception thrown when API rate limit is exceeded
 */
class RateLimitException(
    message: String = "API rate limit exceeded",
    val retryAfterSeconds: Int? = null,
    cause: Throwable? = null
) : ApiException(message, cause, httpCode = 429)