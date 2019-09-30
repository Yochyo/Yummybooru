package de.yochyo.yummybooru.utils.network

object ResponseCodes {
    const val OK = 200
    const val NoContent = 204
    const val BadRequest = 400
    const val Unauthorized = 401
    const val Forbidden = 403
    const val NotFound = 404
    const val Gone = 410
    const val InvalidRecord = 420
    const val Locked = 422
    const val AlreadyExists = 423
    const val InvalidParameters = 424
    const val UserThrottled = 429
    const val InternalServerError = 500
    const val BadGateway = 502
    const val ServiceUnavailable = 503
}