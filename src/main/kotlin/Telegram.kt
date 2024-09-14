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
    val data: String? = null,
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
const val RESET_CLICKED = "resetClicked"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"
var correctAnswerTranslate = "" // тут переменная

fun main(args: Array<String>) {

    val botToken = args[0]
    var lastUpdateId = 0L
    val json = Json { ignoreUnknownKeys = true }
    val trainers = HashMap<Long, LearnWordsTrainer>()

    while (true) {
        Thread.sleep(2000)
        val responseString: String = getUpdates(botToken, lastUpdateId) //тут обращение к классу
        println(responseString)

        val response: Response = json.decodeFromString(responseString)
        if (response.result.isEmpty()) continue
        val sortedUpdates = response.result.sortedBy { it.updateId }
        sortedUpdates.forEach { handleUpdate(it, json, botToken, trainers) }
        lastUpdateId = sortedUpdates.last().updateId + 1
    }
}

fun handleUpdate(update: Update, json: Json, botToken: String, trainers: HashMap<Long, LearnWordsTrainer>) {

    val message = update.message?.text
    val chatId = update.message?.chat?.id ?: update.callbackQuery?.message?.chat?.id ?: return
    val data = update.callbackQuery?.data

    val trainer = trainers.getOrPut(chatId) { LearnWordsTrainer("$chatId.txt") }

    if (message?.lowercase() == "/start") {
        sendMenu(json, botToken, chatId)// тут обращение к классу
    }

    if (data?.lowercase() == LEARN_WORDS_CLICKED) {
        correctAnswerTranslate = checkNextQuestionAndSend(json, trainer, botToken, chatId) // тут обращение к переменной answerTranslate
    }

    if (data?.startsWith(CALLBACK_DATA_ANSWER_PREFIX) == true) {
        val answerId = data.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toInt()
        if (trainer.checkAnswer(answerId)) {
            sendMessage(json, botToken, chatId, "Правильно!")
        } else {
            sendMessage(json, botToken, chatId, "Неправильно. Правильный ответ: $correctAnswerTranslate"
            )
        }
        correctAnswerTranslate = checkNextQuestionAndSend(json, trainer, botToken, chatId)
    }

    if (data?.lowercase() == STATISTICS_CLICKED) {
        val statistics: Statistics = trainer.getStatistics()
        sendMessage(json, botToken, chatId,
            "Выучено ${statistics.learned} из ${statistics.total} слов | ${statistics.percent}%")
    }

    if (data == RESET_CLICKED) {
        trainer.resetProgress()
        sendMessage(json, botToken, chatId, "Прогресс сброшен")
    }
}

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
                    ),
                    listOf(
                        InlineKeyboard(text = "Сбросить прогресс", callbackData = RESET_CLICKED),
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

@Serializable
data class Words(
    val questionWord: String,
    val translate: String,
    var correctAnswersCount: Int = 0,
)