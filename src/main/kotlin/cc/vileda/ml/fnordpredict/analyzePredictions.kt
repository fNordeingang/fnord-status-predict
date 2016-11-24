package cc.vileda.ml.fnordpredict

import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime

fun main(args: Array<String>) {
    Files.readAllLines(Paths.get("predictions.txt"))
            .map { LocalDateTime.parse(it) }
            .distinctBy { "${it.dayOfMonth}${it.dayOfWeek}" }
            .forEach {
                print(it.dayOfWeek.name)
                println("\t\t$it")
            }
}

