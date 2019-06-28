package de.yochyo.yummybooru.utils

object ResponseCodes {
    val OK = 200
    val NoContent = 204
    val BadRequest = 400
    val Unauthorized = 401
    val Forbidden = 403
    val NotFound = 404
    val Gone = 410
    val InvalidRecord = 420
    val Locked = 422
    val AlreadyExists = 423
    val InvalidParameters = 424
    val UserThrottled = 429
    val InternalServerError = 500
    val BadGateway = 502
    val ServiceUnavailable = 503
}