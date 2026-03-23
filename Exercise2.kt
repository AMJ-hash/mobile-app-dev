import kotlin.collections.*

fun main() {
    val words = listOf("apple", "cat", "banana", "dog", "elephant")

    words
        .associateWith { it.length }        // creates map: {apple=5, cat=3, banana=6...}
        .filter { it.value > 4 }            // keep only entries where length > 4
        .forEach { (word, length) ->        // print each remaining entry
            println("$word has length $length")
        }
}
