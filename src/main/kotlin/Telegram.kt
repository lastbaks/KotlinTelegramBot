package org.example

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

const val HOST: String = "https://api.telegram.org"

fun main(args: Array<String>) {

    val botToken = args[0]
    var updateId = 0
    val telegramBotService = TelegramBotService()

    while (true) {
        Thread.sleep(2000)

        val updates: String = telegramBotService.getUpdates(botToken, updateId)
        println(updates)

        val startUpdateId = updates.lastIndexOf("update_id")
        val endUpdateId = updates.lastIndexOf(",\n\"message\"")
        if (startUpdateId == -1 || endUpdateId == -1) continue
        val updateIdString = updates.substring(startUpdateId + 11, endUpdateId)
        updateId = updateIdString.toIntOrNull()?.plus(1) ?: continue

        val messageTextRegex: Regex = "\"text\":\"(.+?)\"".toRegex()
        var matchResult: MatchResult? = messageTextRegex.find(updates)
        var groups = matchResult?.groups
        val message = groups?.get(1)?.value ?: continue

        val chatIdRegex: Regex = "\"chat\":[{]\"id\":(.+?),\"first_name\"".toRegex()
        matchResult = chatIdRegex.find(updates)
        groups = matchResult?.groups
        val chatId = groups?.get(1)?.value ?: continue

        if (message == "Hello") {
            telegramBotService.sendMessage(botToken, chatId.toInt(), "Hello!")
        } else {
            telegramBotService.sendMessage(botToken, chatId.toInt(), "Пока я умею отвечать только на Hello")
        }
    }
}

class TelegramBotService {

    fun getUpdates(botToken: String, updateId: Int): String {
        val urlGetUpdates = "$HOST/bot$botToken/getUpdates?offset=$updateId"
        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

        return response.body()
    }

    fun sendMessage(botToken: String, chatId: Int, text: String) {
        val encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
        val urlSendMessage = "$HOST/bot$botToken/sendMessage?chat_id=$chatId&text=$encodedText"
        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage)).build()
        client.send(request, HttpResponse.BodyHandlers.ofString())
    }
}