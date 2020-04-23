package com.example.j7_003.data.database

import com.example.j7_003.data.settings.SettingsManager
import com.example.j7_003.system_interaction.handler.AlarmHandler
import com.example.j7_003.system_interaction.handler.StorageHandler
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.threeten.bp.DayOfWeek
import org.threeten.bp.DayOfWeek.*
import org.threeten.bp.Duration
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.temporal.ChronoUnit
import kotlin.collections.HashMap

/**
 * A simple class to handle different Reminders for a sleep schedule
 */
class SleepReminder {
    companion object {
        var daysAreCustom: Boolean = false
        private const val fileName: String = "SLEEP_REMINDER_DEBUG"

        var reminder = HashMap<DayOfWeek, Reminder>(7)

        /**
         * Initializes the SleepReminder.
         * @see initMap Initializes the Map for usage.
         * @see createFile Makes the save file accessible.
         * @see load Loads data from the save file into class.
         */
        fun init() {
            initMap()
            createFile()
            load()
        }

        /**
         * Marks the as regular and saves the SleepReminders state
         * to the save file.
         * @see setCustom is the according counterpart.
         */
        fun setRegular() {
            daysAreCustom = false
            save()
        }

        /**
         * Marks the SleepReminder as custom and saved the SleepReminders state to the
         * save file.
         * @see setRegular is the according counterpart.
         */
        fun setCustom() {
            daysAreCustom = true
            save()
        }

        /**
         * Edits a specific Reminders WakeUp time and saves to file.
         * @param day A DayOfWeek object to specify the Reminder to edit.
         * @param hour The hour to set the WakeUp time to.
         * @param minute The minute to set the WakeUp time to.
         */
        fun editWakeUpAtDay(day: DayOfWeek, hour: Int, minute: Int) {
            reminder[day]?.editWakeUpTime(hour, minute)
            updateSingleReminder(day)
            save()
        }

        /**
         * Edits the WakeUp time of all Reminders and saves to file.
         * @param hour The hour to set all Reminders to.
         * @param minute The minute to set all Reminders to.
         */
        fun editAllWakeUp(hour: Int, minute: Int) {
            reminder.forEach { n ->
                n.value.editWakeUpTime(hour, minute)
            }
            save()
        }

        /**
         * Edits a specific Reminders Duration and saves to file.
         * @param day A DayOfWeek object to specify the Reminder to edit.
         * @param hour The hour the Duration will last.
         * @param minute The amount of minutes the Duration will be set to.
         */
        fun editDurationAtDay(day: DayOfWeek, hour: Int, minute: Int) {
            reminder[day]?.editDuration(hour, minute)
            updateSingleReminder(day)
            save()
        }

        /**
         * Edits the Duration of all Reminders and saves to file.
         * @param hour The amount of hours the Duration will be set to.
         * @param minute The amount of minutes the Duration will be set to.
         */
        fun editAllDuration(hour: Int, minute: Int) {
            reminder.forEach { n ->
                n.value.editDuration(hour, minute)
            }
            save()
        }

        /**
         * Returns a string containing the RemainingWakeDuration of the current day.
         * @return A string of the remaining time until the Reminders reminderTime.
         */
        fun getRemainingWakeDurationString(): String {
            return reminder[LocalDate.now().dayOfWeek]?.getRemainingWakeDuration()!!
        }

        /**
         * Enables all Reminders to notify the user and saves the SleepReminder to the save file.
         * @see disableAll is the counterpart of this function.
         */
        fun enableAll() {
            reminder.forEach { n ->
                n.value.isSet = true
            }
            save()
        }

        /**
         * Disables all Reminders from notifying the user and saves the SleepReminder to file.
         * @see enableAll is the counterpart of this function.
         */
        fun disableAll() {
            reminder.forEach { n ->
                 n.value.isSet = false
            }
            save()
        }

        private fun initMap() {
            values().forEach { n ->
                reminder[n] = Reminder()
            }
        }

        private fun createFile() {
            StorageHandler.createJsonFile(
                fileName,
                "SReminder_Debug.json",
                text = Gson().toJson(reminder)
            )
            if (SettingsManager.settings["daysAreCustom"] == null) {
                SettingsManager.addSetting("daysAreCustom", daysAreCustom)
            }
        }

        private fun save() {
            StorageHandler.saveAsJsonToFile(StorageHandler.files[fileName], reminder)
            SettingsManager.addSetting("daysAreCustom", daysAreCustom)
            updateReminder()
        }

        private fun load() {
            val jsonString = StorageHandler.files[fileName]?.readText()

            reminder = GsonBuilder().create()
                .fromJson(jsonString, object : TypeToken<HashMap<DayOfWeek, Reminder>>() {}.type)

            daysAreCustom = SettingsManager.settings["daysAreCustom"] as Boolean

            updateReminder()
        }

        /**
         * Calls the updateAlarm function on all Reminders with their according requestCodes.
         * @see Reminder.updateAlarm for alarm updating.
         */
         fun updateReminder() {
            var i = 200
            reminder.forEach { n ->
                n.value.updateAlarm(n.key, i)
                i++
            }
        }

        private fun updateSingleReminder(dayOfWeek: DayOfWeek) {
           reminder[dayOfWeek]?.updateAlarm(
               dayOfWeek,
               when(dayOfWeek) {
                   SATURDAY -> 200
                   TUESDAY -> 201
                   SUNDAY -> 202
                   THURSDAY -> 203
                   FRIDAY -> 204
                   WEDNESDAY -> 205
                   MONDAY -> 206
               }
           )
        }

        /**
         * A simple local class which instances are used to remind the user
         * of his sleeping habits.
         */
        class Reminder {
            var isSet: Boolean = false
            private var reminderTime = LocalTime.of(23, 59)
            private var wakeUpTime: LocalTime = LocalTime.of(12, 0)
            private var duration: Duration = Duration.ofHours(8).plusMinutes(0)

            /**
             * @return The hour of the Reminders wakeUpTime as int.
             */
            fun getWakeHour(): Int = wakeUpTime.hour

            /**
             * @return The minute of the Reminders wakeUpTime as an int.
             */
            fun getWakeMinute(): Int = wakeUpTime.minute

            /**
             * @return The hour of the Reminders duration as an int.
             */
            fun getDurationHour(): Int = duration.toHours().toInt()

            /**
             * @return The minute of the Reminders duration as an int.
             */
            fun getDurationMinute(): Int = (duration.toMinutes() % 60).toInt()

            /**
             * Changes the Reminders wakeUpTime with the given parameters and
             * then calculates the reminderTime.
             * @param hour The hour to set the wakeUpTime to.
             * @param minute The minute to set the wakeUpTime to.
             */
            fun editWakeUpTime(hour: Int, minute: Int) {
                wakeUpTime = LocalTime.of(hour, minute)
                calcReminderTime()
            }

            /**
             * Changes the Reminders Duration with the given parameters and
             * then calculates the reminderTime.
             * @param hour The hours the duration will last.
             * @param minute The minutes the duration will last.
             */
            fun editDuration(hour: Int, minute: Int) {
                duration = Duration.ofHours(hour.toLong()).plusMinutes(minute.toLong())
                calcReminderTime()
            }

            /**
             * Marks the Reminder as notifiable.
             * @see disable for the counterpart.
             */
            fun enable() { isSet = true; save() }

            /**
             * Marks the Reminder as not notifiable.
             * @see enable for the counterpart.
             */
            fun disable() { isSet = false; save() }

            /**
             * @return The reminderTime formatted as a string.
             */
            fun getRemindTimeString(): String = reminderTime.toString()

            /**
             * @return The WakeUpTime formatted as a string.
             */
            fun getWakeUpTimeString(): String = wakeUpTime.toString()

            /**
             * @return The Duration formatted as "HHh MMm".
             */
            fun getDurationTimeString(): String =
                "${duration.toHours().toString().padStart(2, ' ')}h " +
                    "${(duration.toMinutes()%60).toString().padStart(2, ' ')}m"

            /**
             * @return The remainingWakeDuration formatted as "HHh MMm".
             */
            fun getRemainingWakeDuration(): String {
                return "${(LocalTime.now().until(reminderTime, ChronoUnit.HOURS))}h " +
                        "${(LocalTime.now().until(reminderTime, ChronoUnit.MINUTES)%60)}m"
            }

            private fun calcReminderTime() {
               reminderTime = wakeUpTime.minus(duration)
            }

            /**
             * Updates the AlarmManager for the calling Reminder.
             * @param weekdays The day of the week to specify the Reminder.
             * @param requestCode An integer to identify the alarm.
             */
            fun updateAlarm(weekdays: DayOfWeek, requestCode: Int) {
                AlarmHandler.setNewSleepReminderAlarm(
                    dayOfWeek = weekdays,
                    requestCode = requestCode,
                    reminderTime = reminderTime
                )
            }
        }
    }
}