package cc.vileda.ml.fnordpredict

import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
    val p = Paths.get("normalized.csv").toFile()
    if (p.exists()) p.delete()
    Files.createFile(Paths.get("normalized.csv")).toFile().writeText(
            normalizeTweets()
                    .map { "${it.date};${it.data.joinToString(";")}" }
                    .joinToString("\n")
    )
}

