package org.example

import java.io.File

fun main() {
    val wordsFile = File("words.txt")
    wordsFile.createNewFile()
    val textList = wordsFile.readLines()
    textList.forEach { println(it) }
}