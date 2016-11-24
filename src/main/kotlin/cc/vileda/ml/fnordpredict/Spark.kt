package cc.vileda.ml.fnordpredict

import org.apache.commons.math3.stat.descriptive.rank.Median
import org.apache.spark.SparkConf
import org.apache.spark.api.java.JavaSparkContext
import java.time.*


fun main(args: Array<String>) {
    val nowTime = LocalTime.now()
    val startDate = utc(ZonedDateTime.of(LocalDate.now(), LocalTime.MIN.withHour(nowTime.hour), ZoneId.systemDefault()))

    val sparkConf = SparkConf().setAppName("Spark")
    val jsc = JavaSparkContext(sparkConf)

    val dates = jsc.parallelize(IntProgression.fromClosedRange(0, 360, 1).toList())
            .map { startDate.plusDays(it.toLong()) }
            .map {
                val now = it
                IntProgression.fromClosedRange(0, 7*24*60, 1)
                    .map { now.plusMinutes(it.toLong()) }
                    .flatMap { classify(it, data) }
                    .sortedBy({ it.distance })
                    .map { it.input.atZone(ZoneOffset.UTC)?.withZoneSameInstant(ZoneId.systemDefault())?.toEpochSecond()!!.toDouble() }
                    .take(20)
                    .toDoubleArray()
            }.collect()

    dates.forEach {
        val averageDate = Median().evaluate(it)
        val ofEpochSecond = LocalDateTime.ofEpochSecond(averageDate.toLong(), 0, ZoneOffset.UTC)
        println(ofEpochSecond)
    }
}
