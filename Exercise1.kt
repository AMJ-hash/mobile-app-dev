import kotlin.collections.*

fun processList(
    numbers: List<Int>,
    predicate: (Int) -> Boolean
): List<Int> {
    val result = mutableListOf<Int>()
    for (number in numbers) {
        if (predicate(number)) {
            result.add(number)
        }
    }
    return result
}

fun main() {
    val nums = listOf(1, 2, 3, 4, 5, 6)
    val even = processList(nums) { it % 2 == 0 }
    println(even) // [2, 4, 6]
}
