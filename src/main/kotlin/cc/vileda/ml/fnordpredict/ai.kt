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
            .filter { it.contains("open") }
            .map { it.replace(Regex("(?i)#fnordeingang is now open! "), "") }
            .map { it.replace(Regex("(?i) #manuallyTriggered"), "") }
            .map { it.replace(Regex("(?i)is now open! "), "") }
            .map(::parseDateTime)
            .map(::statusDateDataFromDateTime)
}

fun classify(unknown: LocalDateTime, trainedData: List<StatusDateData>, neighbours: Int = 5): List<ClassifyResultWithDistance> {
    val statusDateDataFromDateTime = statusDateDataFromDateTime(unknown)
    return trainedData
            .map {
                ClassifyResultWithDistance(
                        unknown,
                        MathArrays.distance(it.data, statusDateDataFromDateTime.data))
            }
            .sortedBy { it.distance }
            .take(neighbours)
}

private fun statusDateDataFromDateTime(date: LocalDateTime): StatusDateData {
    return StatusDateData(
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
        val input: LocalDateTime,
        val distance: Double
) : Serializable

class StatusDateData(
        month: Double,
        day: Double,
        hour: Double,
        minute: Double,
        isWeekday: Double,
        isVacationDay: Double,
        val date: LocalDateTime
) { val data = arrayOf(month, day, hour, minute, isWeekday, isVacationDay).toDoubleArray() }


