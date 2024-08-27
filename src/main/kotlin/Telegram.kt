package org.example

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

const val HOST:String = "https://api.telegram.org"

fun main(args: Array<String>) {

    val botToken = args[0]
    var updateId = 0

    while (true) {
        Thread.sleep(2000)

        val telegramBotService = TelegramBotService()
        val updates: String = telegramBotService.getUpdates(botToken, updateId)
        println(updates)

        val startUpdateId = updates.lastIndexOf("update_id")
        val endUpdateId = updates.lastIndexOf(",\n\"message\"")
        if (startUpdateId == -1 || endUpdateId == -1) continue
        val updateIdString = updates.substring(startUpdateId + 11, endUpdateId)
        updateId = updateIdString.toInt() + 1

        val messageTextRegex: Regex = "\"text\":\"(.+?)\"".toRegex()

        val chatIdRegex: Regex = "\"chat\":[{]\"id\":(.+?),\"first_name\"".toRegex()
        val matchResult: MatchResult? = chatIdRegex.find(updates)
        val groups = matchResult?.groups
        val chatId = groups?.get(1)?.value
        println(chatId)
        telegramBotService.sendMessage(botToken, chatId!!.toInt(), "text to send")
    }
}

class TelegramBotService{

    fun getUpdates(botToken: String, updateId: Int) : String {
        val urlGetUpdates = "$HOST/bot$botToken/getUpdates?offset=$updateId"
        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

        return (response.body())
    }

    fun sendMessage(botToken: String, chatId: Int, text: String) {
        val encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
        val urlSendMessage = "$HOST/bot$botToken/sendMessage?chat_id=$chatId&text=$encodedText"
        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage)).build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
    }
}





