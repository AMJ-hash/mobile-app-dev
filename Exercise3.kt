import kotlin.collections.*

data class Person(val name: String, val age: Int)

fun main() {
    val people = listOf(
        Person("Alice", 25),
        Person("Bob", 30),
        Person("Charlie", 35),
        Person("Anna", 22),
        Person("Ben", 28)
    )

    val average = people
        .filter { it.name.startsWith("A") || it.name.startsWith("B") }  // Step 1
        .map { it.age }                                                   // Step 2
        .average()                                                        // Step 3

    println("Average age: ${String.format("%.1f", average)}")                    // Step 4
}
