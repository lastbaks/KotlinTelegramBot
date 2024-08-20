package org.example

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

const val HOST:String = "https://api.telegram.org"

fun main(args: Array<String>) {

    val botToken = args[0]
    val urlGetMe = "$HOST/bot$botToken/getMe"
    val urlGetUpdates = "$HOST/bot$botToken/getUpdates"

    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

    println(response.body())
}