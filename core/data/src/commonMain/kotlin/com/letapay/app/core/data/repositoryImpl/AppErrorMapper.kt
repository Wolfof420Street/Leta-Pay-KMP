package com.letapay.app.core.data.repositoryImpl

import com.letapay.app.core.model.error.AppError
import com.letapay.app.core.network.BackendApiException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.utils.io.errors.IOException

internal fun Throwable.toAppError(): AppError = when (this) {
    is AppError -> this
    is BackendApiException -> when (statusCode) {
        401 -> AppError.Unauthorized(message)
        400 -> AppError.Validation(field = "request", reason = message)
        403 -> AppError.ChainRejected(reason = message, code = statusCode)
        409 -> AppError.Validation(field = "conflict", reason = message)
        422 -> mapUnprocessableEntity()
        else -> AppError.Unexpected(message)
    }
    is IOException, is SocketTimeoutException -> AppError.Network(displayMessage = message ?: "Network error")
    else -> AppError.Unexpected(message ?: "Unknown error")
}

private fun BackendApiException.mapUnprocessableEntity(): AppError {
    val required = Regex("required=([^\\s]+)").find(message)?.groupValues?.getOrNull(1)
    val actual = Regex("actual=([^\\s]+)").find(message)?.groupValues?.getOrNull(1)
    return if (!required.isNullOrBlank() && !actual.isNullOrBlank()) {
        AppError.InsufficientBalance(required = required, actual = actual)
    } else {
        AppError.Validation(field = "request", reason = message)
    }
}
