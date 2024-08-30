package org.example

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

const val LEARN_WORDS_CLICKED = "learn_words_clicked"
const val STATISTICS_CLICKED = "statistics_clicked"

fun main(args: Array<String>) {

    val trainer = try {
        LearnWordsTrainer(3, 3, 4)
    } catch (e: Exception) {
        println("невозможно загрузить словарь")
        return
    }

    val botToken = args[0]
    var lastUpdateId = 0
    val telegramBotService = TelegramBotService()

    val updateIdRegex = "\"update_id\":(\\d+)".toRegex()
    val messageTextRegex: Regex = "\"text\":\"(.+?)\"".toRegex()
    val chatIdRegex: Regex = "\"chat\":\\{\"id\":(\\d+)".toRegex()
    val dataRegex: Regex = "\"data\":\"(.+?)\"".toRegex()

    while (true) {
        Thread.sleep(2000)
        val updates: String = telegramBotService.getUpdates(botToken, lastUpdateId)
        println(updates)

        val updateId = updateIdRegex.find(updates)?.groups?.get(1)?.value?.toIntOrNull() ?: continue
        lastUpdateId = updateId + 1

        val message = messageTextRegex.find(updates)?.groups?.get(1)?.value
        val chatId = chatIdRegex.find(updates)?.groups?.get(1)?.value?.toInt()
        val data = dataRegex.find(updates)?.groups?.get(1)?.value

        if (message?.lowercase() == "hello" && chatId != null) {
            telegramBotService.sendMessage(botToken, chatId, "Hello!")
        }

        if (message?.lowercase() == "/start" && chatId != null) {
            telegramBotService.sendMenu(botToken, chatId)
        }

        if (data?.lowercase() == STATISTICS_CLICKED && chatId != null) {
            val statistics = trainer.getStatistics()
            val statisticsMessage = ("Выучено ${statistics.learned} из ${statistics.total} слов | ${statistics.percent}%")
            telegramBotService.sendMessage(botToken, chatId, statisticsMessage)
        }
    }
}

class TelegramBotService {

    private val host: String = "https://api.telegram.org"

    fun getUpdates(botToken: String, updateId: Int): String {
        val urlGetUpdates = "$host/bot$botToken/getUpdates?offset=$updateId"
        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendMessage(botToken: String, chatId: Int, message: String) {
        val encoded = URLEncoder.encode(message, StandardCharsets.UTF_8)
        println(encoded)
        val urlSendMessage = "$host/bot$botToken/sendMessage?chat_id=$chatId&text=$encoded"
        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlSendMessage)).build()
        client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    fun sendMenu(botToken: String, chatId: Int) : String {

        val sendMessage = "$host/bot$botToken/sendMessage"
        val sendMenuBody = """
            {
                "chat_id": $chatId,
                "text": "Основное меню",
                "reply_markup": {
                    "inline_keyboard": [
                        [
                            {
                                "text": "Изучить слова",
                                "callback_data": "$LEARN_WORDS_CLICKED"
                            },
                            {
                                "text": "Статистика",
                                "callback_data": "$STATISTICS_CLICKED"
                            }
                        ]
                    ]
                }
            }
        """.trimIndent()

        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(sendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(sendMenuBody))
            .build()

        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())

        return response.body()
    }
}

data class Word(
    val questionWord: String,
    val translate: String,
    var correctAnswersCount: Int = 0,
)

fun Question.asConsoleString(): String {
    val variants = this.variants
        .mapIndexed { index: Int, word: Word -> " ${index + 1} - ${word.translate}" }
        .joinToString("\n")
    return this.correctAnswer.questionWord + "\n" + variants + "\n 0 - выйти в меню"
}