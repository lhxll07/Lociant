package io.lociant.android.server

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondText
import org.json.JSONObject

class InvalidRequestException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)
class UnauthorizedRequestException : RuntimeException("Authentication is required")

suspend fun ApplicationCall.respondProblem(
    status: HttpStatusCode,
    title: String,
    detail: String,
    code: String,
) {
    val body = JSONObject()
        .put("type", "https://lociant.io/problems/$code")
        .put("title", title)
        .put("status", status.value)
        .put("detail", detail)
        .put("code", code)
        .put("instance", request.local.uri)
    respondText(
        body.toString(),
        ContentType.parse("application/problem+json; charset=utf-8"),
        status,
    )
}
