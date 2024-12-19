import java.io.File
import java.util.*

fun main(args: Array<String>) {
    val flashcards = mutableMapOf<String, Pair<String, Int>>() // Map of term -> Pair(definition, mistakes)
    val log = mutableListOf<String>()
    val scanner = Scanner(System.`in`)
    var exportFileName: String? = null

    // Parse command-line arguments
    val arguments = args.toList()
    val importIndex = arguments.indexOf("-import")
    if (importIndex != -1 && importIndex + 1 < arguments.size) {
        val importFileName = arguments[importIndex + 1]
        importCardsFromFile(flashcards, importFileName, log)
    }

    val exportIndex = arguments.indexOf("-export")
    if (exportIndex != -1 && exportIndex + 1 < arguments.size) {
        exportFileName = arguments[exportIndex + 1]
    }

    while (true) {
        logAndPrint(log, "Input the action (add, remove, import, export, ask, exit, log, hardest card, reset stats):")
        val action = scanner.nextLine()
        log.add(action)

        when (action) {
            "add" -> addCard(flashcards, scanner, log)
            "remove" -> removeCard(flashcards, scanner, log)
            "import" -> importCards(flashcards, scanner, log)
            "export" -> exportCards(flashcards, scanner, log)
            "ask" -> askQuestions(flashcards, scanner, log)
            "log" -> saveLog(scanner, log)
            "hardest card" -> hardestCard(flashcards, log)
            "reset stats" -> resetStats(flashcards, log)
            "exit" -> {
                logAndPrint(log, "Bye bye!")
                if (exportFileName != null) {
                    exportCardsToFile(flashcards, exportFileName, log)
                }
                break
            }
            else -> logAndPrint(log, "Invalid action, try again.")
        }
    }
}

fun logAndPrint(log: MutableList<String>, message: String) {
    log.add(message)
    println(message)
}

fun addCard(flashcards: MutableMap<String, Pair<String, Int>>, scanner: Scanner, log: MutableList<String>) {
    logAndPrint(log, "The card:")
    val term = scanner.nextLine().trim()
    log.add(term)

    if (flashcards.containsKey(term)) {
        logAndPrint(log, "The card \"$term\" already exists.")
        return
    }

    logAndPrint(log, "The definition of the card:")
    val definition = scanner.nextLine().trim()
    log.add(definition)

    if (flashcards.values.any { it.first == definition }) {
        logAndPrint(log, "The definition \"$definition\" already exists.")
        return
    }

    flashcards[term] = Pair(definition, 0)
    logAndPrint(log, "The pair (\"$term\":\"$definition\") has been added.")
}

fun removeCard(flashcards: MutableMap<String, Pair<String, Int>>, scanner: Scanner, log: MutableList<String>) {
    logAndPrint(log, "Which card?")
    val term = scanner.nextLine().trim()
    log.add(term)

    if (flashcards.remove(term) != null) {
        logAndPrint(log, "The card has been removed.")
    } else {
        logAndPrint(log, "Can't remove \"$term\": there is no such card.")
    }
}

fun importCards(flashcards: MutableMap<String, Pair<String, Int>>, scanner: Scanner, log: MutableList<String>) {
    logAndPrint(log, "File name:")
    val fileName = scanner.nextLine().trim()
    log.add(fileName)
    importCardsFromFile(flashcards, fileName, log)
}

fun importCardsFromFile(flashcards: MutableMap<String, Pair<String, Int>>, fileName: String, log: MutableList<String>) {
    val file = File(fileName)

    if (file.exists()) {
        val importedCount = file.readLines().count { line ->
            val parts = line.split(":")
            if (parts.size == 3) {
                val term = parts[0].trim('"', ' ')
                val definition = parts[1].trim('"', ' ')
                val mistakes = parts[2].toIntOrNull() ?: 0
                flashcards[term] = Pair(definition, mistakes)
                true
            } else false
        }
        logAndPrint(log, "$importedCount cards have been loaded.")
    } else {
        logAndPrint(log, "File not found.")
    }
}

fun exportCards(flashcards: MutableMap<String, Pair<String, Int>>, scanner: Scanner, log: MutableList<String>) {
    logAndPrint(log, "File name:")
    val fileName = scanner.nextLine().trim()
    log.add(fileName)
    exportCardsToFile(flashcards, fileName, log)
}

fun exportCardsToFile(flashcards: MutableMap<String, Pair<String, Int>>, fileName: String, log: MutableList<String>) {
    val file = File(fileName)

    file.printWriter().use { writer ->
        flashcards.forEach { (term, pair) ->
            writer.println("\"$term\":\"${pair.first}\":${pair.second}")
        }
    }
    logAndPrint(log, "${flashcards.size} cards have been saved.")
}

fun askQuestions(flashcards: MutableMap<String, Pair<String, Int>>, scanner: Scanner, log: MutableList<String>) {
    logAndPrint(log, "How many times to ask?")
    val timesToAsk = scanner.nextLine().toIntOrNull() ?: return
    log.add(timesToAsk.toString())

    val random = Random()
    repeat(timesToAsk) {
        val (term, pair) = flashcards.entries.random()
        logAndPrint(log, "Print the definition of \"$term\":")
        val userAnswer = scanner.nextLine().trim()
        log.add(userAnswer)

        if (userAnswer == pair.first) {
            logAndPrint(log, "Correct!")
        } else {
            val wrongAnswerTerm = flashcards.entries.find { it.value.first == userAnswer }?.key
            flashcards[term] = pair.copy(second = pair.second + 1)
            if (wrongAnswerTerm != null) {
                logAndPrint(log, "Wrong. The right answer is \"${pair.first}\", but your definition is correct for \"$wrongAnswerTerm\".")
            } else {
                logAndPrint(log, "Wrong. The right answer is \"${pair.first}\".")
            }
        }
    }
}

fun saveLog(scanner: Scanner, log: MutableList<String>) {
    logAndPrint(log, "File name:")
    val fileName = scanner.nextLine().trim()
    log.add(fileName)
    val file = File(fileName)

    file.printWriter().use { writer ->
        log.forEach { writer.println(it) }
    }
    logAndPrint(log, "The log has been saved.")
}

fun hardestCard(flashcards: MutableMap<String, Pair<String, Int>>, log: MutableList<String>) {
    val maxMistakes = flashcards.values.maxOfOrNull { it.second } ?: 0

    if (maxMistakes == 0) {
        logAndPrint(log, "There are no cards with errors.")
    } else {
        val hardest = flashcards.filter { it.value.second == maxMistakes }.keys
        val termString = hardest.joinToString(", ") { "\"$it\"" }
        val message = if (hardest.size == 1) {
            "The hardest card is $termString. You have $maxMistakes errors answering it."
        } else {
            "The hardest cards are $termString. You have $maxMistakes errors answering them."
        }
        logAndPrint(log, message)
    }
}

fun resetStats(flashcards: MutableMap<String, Pair<String, Int>>, log: MutableList<String>) {
    flashcards.keys.forEach { key ->
        flashcards[key] = flashcards[key]!!.copy(second = 0)
    }
    logAndPrint(log, "Card statistics have been reset.")
}
