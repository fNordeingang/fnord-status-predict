package cc.vileda.ml.fnordpredict

import de.jollyday.HolidayCalendar
import de.jollyday.HolidayManager
import de.jollyday.ManagerParameters
import org.apache.commons.math3.util.MathArrays
import java.io.Serializable
import java.nio.file.Files
import java.nio.file.Paths
import java.time.*
import java.time.format.DateTimeFormatter

fun fnordOpensInHoursFromNow(trainedData: List<StatusDateData>): LocalDateTime {
    val now = utc(ZonedDateTime.of(LocalDate.now(), LocalTime.MIN, ZoneId.systemDefault()))
    val result = fnordOpensInHoursFrom(now, trainedData)

    return result?.input!!
}

fun fnordOpensInHoursFrom(dateTime: LocalDateTime, trainedData: List<StatusDateData>): ClassifyResultWithDistance? {
    return IntProgression.fromClosedRange(0, 4 * 24, 1)
            .map { dateTime.plusHours(it.toLong()) }
            .flatMap { classify(it, trainedData) }
            .minBy { it.distance }
}

val data: List<StatusDateData> by lazy {
    val lines = Files.readAllLines(Paths.get("normalized.csv"))
    lines.map {
        val split = it.split(";")
        StatusDateData(
                1,
                split[1].toDouble(),
                split[2].toDouble(),
                split[3].toDouble(),
                split[4].toDouble(),
                split[5].toDouble(),
                split[6].toDouble(),
                LocalDateTime.parse(split[0])
        )
    }
}

fun normalizeTweets(): List<StatusDateData> {
    return dateTweets
            .map { if (it.contains("open")) StatusTweet(1, it) else StatusTweet(0, it) }
            .filter { it.open == 1 }
            .map { StatusTweet(it.open, it.text.replace(Regex("(?i)#fnordeingang is now open! "), "")) }
            .map { StatusTweet(it.open, it.text.replace(Regex("(?i) #manuallyTriggered"), "")) }
            .map { StatusTweet(it.open, it.text.replace(Regex("(?i)is now open! "), "")) }
            .map { StatusDate(it.open, parseDateTime(it.text)) }
            .map(::makeStatusDateData)
}

fun classify(unknown: LocalDateTime, trainedData: List<StatusDateData>, neighbours: Int = 5): List<ClassifyResultWithDistance> {
    val statusDateDataFromDateTime = statusDateDataFromDateTime(unknown, 0)
    return trainedData
            .map {
                ClassifyResultWithDistance(
                        it.open,
                        unknown,
                        //it.data,
                        //it.date,
                        //statusDateDataFromDateTime.data,
                        MathArrays.distance(it.data, statusDateDataFromDateTime.data))
            }
            .sortedBy { it.distance }
            .take(neighbours)
}

private fun makeStatusDateData(it: StatusDate): StatusDateData {
    val date = it.date
    return statusDateDataFromDateTime(date, it.open)
}

private fun statusDateDataFromDateTime(date: LocalDateTime, open: Int): StatusDateData {
    return StatusDateData(
            open,
            date.month.value.toDouble(),
            date.dayOfWeek.value.toDouble(),
            date.hour.toDouble(),
            date.minute.toDouble(),
            isWeekday(date.dayOfWeek.value).toDouble(),
            isVacationDay(date).toDouble(),
            date
    )
}

private fun isVacationDay(value: LocalDateTime): Int {
    return if (holidayManager.isHoliday(value.toLocalDate())) 1 else 0
}

private val holidayManager: HolidayManager by lazy {
    val managerParameter = ManagerParameters.create(HolidayCalendar.GERMANY)
    val holidayManager = HolidayManager.getInstance(managerParameter)
    holidayManager
}

private fun isWeekday(value: Int): Int {
    return if (listOf(6, 7).contains(value)) 1 else 0
}

fun utc(dateTime: ZonedDateTime): LocalDateTime {
    return LocalDateTime.ofInstant(dateTime.toInstant(), ZoneOffset.UTC)
}

private fun parseDateTime(dateTime: String): LocalDateTime {
    try {
        return utc(ZonedDateTime.parse(dateTime))
    } catch (e: DateTimeException) {
        return utc(ZonedDateTime.parse(dateTime, DateTimeFormatter.ofPattern("EE MMM d y H:m:s 'GMT'Z (zz)")))
    }
}

class ClassifyResultWithDistance(
        val open: Int,
        val input: LocalDateTime,
        // val dataInput: DoubleArray,
        // val date: LocalDateTime,
        // val data: DoubleArray,
        val distance: Double
) : Serializable

private data class StatusTweet(val open: Int, val text: String)
private data class StatusDate(val open: Int, val date: LocalDateTime)
class StatusDateData(
        val open: Int,
        month: Double,
        day: Double,
        hour: Double,
        minute: Double,
        isWeekday: Double,
        isVacationDay: Double,
        val date: LocalDateTime
) {
    val data = arrayOf(month, day, hour, minute, isWeekday, isVacationDay)
            .toDoubleArray()
}


