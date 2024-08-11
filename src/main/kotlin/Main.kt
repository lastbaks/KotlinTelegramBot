package org.example

import java.io.File

data class Word(
    val original: String,
    val translate: String,
    var correctAnswersCount: Int = 0,
)

fun main() {
    val dictionary = mutableListOf<Word>()

    val wordsFile = File("words.txt")
    val lines: List<String> = wordsFile.readLines()
    for (line in lines) {
        val line = line.split("|")
        val word = Word(original = line[0], translate = line[1], correctAnswersCount = line[2].toInt() ?: 0)
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
                println(1)
            }

            "2" -> {
                println(2)
            }

            "0" -> return
            else -> {
                println("Введено некорректное число")
            }
        }
    }
}