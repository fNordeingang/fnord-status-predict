package cc.vileda.ml.fnordpredict

import org.apache.commons.math3.stat.descriptive.rank.Median
import java.time.*
import java.util.stream.Collectors
import java.util.stream.IntStream


fun main(args: Array<String>) {

    val trainingSetSize = 750
    val allData = data.reversed()
    val trainingData = allData.take(trainingSetSize)
    val validationData = allData.subList(trainingSetSize, data.lastIndex).map { it.date }

    val startDate = validationData.first()

    val predictions = IntProgression.fromClosedRange(0, validationData.size - 1, 1)
            .map {
                val now = startDate.plusDays(it.toLong())

                @Suppress("UNCHECKED_CAST")
                val dates = IntStream.range(0, 1 * 24 * 60)
                        .parallel()
                        .mapToObj { now.plusMinutes(it.toLong()) }
                        .map { classify(it, trainingData) }
                        .collect(Collectors.toList())
                        .flatMap { it as List<ClassifyResultWithDistance> }
                        .filter { it.open == 1 && it.distance == 0.0 }
                        .take(20)
                        .map { it.input.atZone(ZoneOffset.UTC)?.withZoneSameInstant(ZoneId.systemDefault())?.toEpochSecond()!!.toDouble() }
                        .toDoubleArray()

                val averageDate = Median().evaluate(dates)
                LocalDateTime.ofEpochSecond(averageDate.toLong(), 0, ZoneOffset.UTC)
            }

    val validationDay =
            predictions
                    .filter {
                        val p = it
                        validationData.filter {
                            p.year == it.year
                                    && p.monthValue == it.monthValue
                                    && p.dayOfMonth == it.dayOfMonth
                        }.isNotEmpty()
                    }

    val validationHour =
            predictions
                    .filter {
                        val p = it
                        validationData.filter {
                            p.year == it.year
                                    && p.monthValue == it.monthValue
                                    && p.dayOfMonth == it.dayOfMonth
                                    && p.hour == it.hour
                        }.isNotEmpty()
                    }

    val validationHourLoosey =
            predictions
                    .filter {
                        val p = it
                        validationData.filter {
                            p.year == it.year
                                    && p.monthValue == it.monthValue
                                    && p.dayOfMonth == it.dayOfMonth
                                    && Math.abs(p.toEpochSecond(ZoneOffset.UTC) - it.toEpochSecond(ZoneOffset.UTC)) <= 60*90
                        }.isNotEmpty()
                    }

    println("Trainingdata length: ${trainingData.size}")
    println("Validationdata length: ${validationData.size}")
    println("predictions length: ${predictions.size}")
    println("% predictions match day: ${validationDay.size.toDouble() / predictions.size.toDouble()}")
    println("% predictions match hour: ${validationHour.size.toDouble() / predictions.size.toDouble()}")
    println("% predictions match hour (+-1.5h): ${validationHourLoosey.size.toDouble() / predictions.size.toDouble()}")
}