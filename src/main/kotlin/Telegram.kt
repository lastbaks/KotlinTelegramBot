package org.example

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Serializable
data class Update(
    @SerialName("update_id")
    val updateId: Long,
    @SerialName("message")
    val message: Message? = null,
    @SerialName("callback_query")
    val callbackQuery: CallbackQuery? = null
)

@Serializable
data class Response(
    @SerialName("result")
    val result: List<Update>,
)

@Serializable
data class Message(
    @SerialName("text")
    val text: String,
    @SerialName("chat")
    val chat: Chat,

    )

@Serializable
data class CallbackQuery(
    @SerialName("data")
    val data: String,
    @SerialName("message")
    val message: Message? = null,
)

@Serializable
data class Chat(
    @SerialName("id")
    val id: Long,
)

@Serializable
data class SendMessageRequest(
    @SerialName("chat_id")
    val chatId: Long,
    @SerialName("text")
    val text: String,
    @SerialName("reply_markup")
    val replyMarkup: ReplyMarkup? = null,
)

@Serializable
data class ReplyMarkup(
    @SerialName("inline_keyboard")
    val inlineKeyboard: List<List<InlineKeyboard>>,
)

@Serializable
data class InlineKeyboard(
    @SerialName("callback_data")
    val callbackData: String,
    @SerialName("text")
    val text: String,
)

const val LEARN_WORDS_CLICKED = "learn_words_clicked"
const val STATISTICS_CLICKED = "statistics_clicked"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

fun main(args: Array<String>) {

    val trainer = try {
        LearnWordsTrainer(3, 3, 4)
    } catch (e: Exception) {
        println("Невозможно загрузить словарь")
        return
    }

    val botToken = args[0]
    var lastUpdateId = 0L
    val telegramBotService = TelegramBotService()
    var answerTranslate = ""

    val json = Json {
        ignoreUnknownKeys = true
    }

    while (true) {
        Thread.sleep(2000)
        val responseString: String = telegramBotService.getUpdates(botToken, lastUpdateId)
        println(responseString)
        val response: Response = json.decodeFromString(responseString)
        val updates = response.result
        val firstUpdate = updates.firstOrNull() ?: continue
        val updateId = firstUpdate.updateId
        lastUpdateId = updateId + 1


        val message = firstUpdate.message?.text
        val chatId = firstUpdate.message?.chat?.id ?: firstUpdate.callbackQuery?.message?.chat?.id
        val data = firstUpdate.callbackQuery?.data

        if (message?.lowercase() == "hello" && chatId != null) {
            telegramBotService.sendMessage(json, botToken, chatId, "Hello!")
        }

        if (message?.lowercase() == "/start" && chatId != null) {
            telegramBotService.sendMenu(json, botToken, chatId)
        }

        if (data?.lowercase() == STATISTICS_CLICKED && chatId != null) {
            val statistics = trainer.getStatistics()
            val statisticsMessage =
                ("Выучено ${statistics.learned} из ${statistics.total} слов | ${statistics.percent}%")
            telegramBotService.sendMessage(json, botToken, chatId, statisticsMessage)
        }

        if (data?.lowercase() == LEARN_WORDS_CLICKED && chatId != null) {
            answerTranslate = telegramBotService.checkNextQuestionAndSend(json, trainer, botToken, chatId)
        }

        if (data != null && chatId != null) {
            if (data.startsWith(CALLBACK_DATA_ANSWER_PREFIX)) {
                val answerId = data.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toInt()

                if (trainer.checkAnswer(answerId)) {
                    telegramBotService.sendMessage(json, botToken, chatId, "Правильно!")
                } else {
                    telegramBotService.sendMessage(
                        json,
                        botToken,
                        chatId,
                        "Неправильно. Правильный ответ: $answerTranslate"
                    )
                    println("Не правильно!  $answerTranslate")
                }
                answerTranslate = telegramBotService.checkNextQuestionAndSend(json, trainer, botToken, chatId)
            }
        }
    }
}

class TelegramBotService {

    private val host: String = "https://api.telegram.org"

    fun checkNextQuestionAndSend(json: Json, trainer: LearnWordsTrainer, token: String, chatId: Long): String {
        val nextQuestion = trainer.getNextQuestion()
        if (nextQuestion == null) {
            sendMessage(json, token, chatId, "Вы выучили все слова в базе")
        } else {
            sendQuestion(json, token, chatId, nextQuestion)
        }
        return nextQuestion?.correctAnswer?.translate ?: ""
    }

    fun getUpdates(botToken: String, updateId: Long): String {
        val urlGetUpdates = "$host/bot$botToken/getUpdates?offset=$updateId"
        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdates)).build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendMessage(json: Json, botToken: String, chatId: Long, message: String): String {
        val sendMessage = "$host/bot$botToken/sendMessage"
        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = message,
        )
        val requestBodyString = json.encodeToString(requestBody)
        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(sendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun sendMenu(json: Json, botToken: String, chatId: Long): String {
        val sendMessage = "$host/bot$botToken/sendMessage"
        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = "Основное меню",
            replyMarkup = ReplyMarkup(
                listOf(
                    listOf(
                        InlineKeyboard(text = "Изучать слова", callbackData = LEARN_WORDS_CLICKED),
                        InlineKeyboard(text = "Статистика", callbackData = STATISTICS_CLICKED),
                    )
                )
            )
        )
        val requestBodyString = json.encodeToString(requestBody)
        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(sendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    private fun sendQuestion(json: Json, botToken: String, chatId: Long, question: Question): String {

        val sendMessage = "$host/bot$botToken/sendMessage"
        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = question.correctAnswer.questionWord,
            replyMarkup = ReplyMarkup(
                listOf(question.variants.mapIndexed { index, words ->
                    InlineKeyboard(
                        text = words.translate, callbackData = "$CALLBACK_DATA_ANSWER_PREFIX$index"
                    )
                })
            )
        )
        val requestBodyString = json.encodeToString(requestBody)
        val client: HttpClient = HttpClient.newBuilder().build()
        val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(sendMessage))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()
        val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }
}

@Serializable
data class Words(
    val questionWord: String,
    val translate: String,
    var correctAnswersCount: Int = 0,
)