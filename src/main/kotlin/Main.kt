package org.example

import java.io.File

fun main() {
    val dictionary = mutableListOf<Word>()
    val wordsFile = File("words.txt")
    val lines: List<String> = wordsFile.readLines()
    for (line in lines) {
        val item = line.split("|")
        val word = Word(original = item[0], translate = item[1], correctAnswersCount = item[2].toInt())
        dictionary.add(word)
    }

    while (true) {
        println("Меню:")
        println("1 - Учить слова")
        println("2 - Статистика")
        println("0 - Выход")
        val input = readln()

        when (input) {
            "1" -> {
                while (true) {
                    print("Слово: ")
                    val nonLearnedWords = dictionary.filter { it.correctAnswersCount <= 3 }
                    if (nonLearnedWords.isEmpty()) {
                        println("Все слова выучены")
                        return
                    }
                    val answers = nonLearnedWords.shuffled().take(4)
                    val question = answers.shuffled().take(1)
                    var answer: Int

                    question.forEach {
                        println(it.original)
                    }
                    println("Введите номер с правильным вариантом перевода:")
                    answers.forEachIndexed { index, value -> println("${index + 1} - ${value.translate}") }
                    println("0 - Выход в главное меню")

                    answer = (readln().toInt())
                    if (answer == 0) break
                    if (answers[answer - 1].translate == question[0].translate) {

                        dictionary.forEach {
                            if (it.translate == answers[answer].translate) {
                                it.correctAnswersCount++
                            }
                        }
                        saveDictionary(dictionary, wordsFile)
                    }
                }
            }

            "2" -> {
                // Печать статистики по выученым словам
                val totalWordsCount = dictionary.size
                val correctAnswers = dictionary.filter { it.correctAnswersCount > 3 }.size
                val percentCorrectAnswers = (correctAnswers * 100 / totalWordsCount)
                println("Выучено $correctAnswers из $totalWordsCount слов | $percentCorrectAnswers%")
                //
            }

            "0" -> return
            else -> {
                println("Введено некорректное число")
            }
        }
    }
}

fun saveDictionary(dictionary: MutableList<Word>,  wordsFile: File) {
    wordsFile.writeText("")
    dictionary.forEach {
        wordsFile.appendText("${ it.original }|${it.translate}|${it.correctAnswersCount}\n")
    }
}