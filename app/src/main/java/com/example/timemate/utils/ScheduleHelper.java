package com.example.timemate.utils;

import com.example.timemate.data.database.AppDatabase;
import com.example.timemate.data.model.Schedule;

import java.util.concurrent.Executors;

public class ScheduleHelper {
    public static void insertSchedule(AppDatabase db, String title, String dateTime, String departure, String destination, String memo) {
        Schedule schedule = new Schedule();
        schedule.title = title;
        // dateTime을 date와 time으로 분리
        String[] parts = dateTime.split(" ");
        if (parts.length >= 2) {
            schedule.date = parts[0];
            schedule.time = parts[1];
        }
        schedule.departure = departure;
        schedule.destination = destination;
        schedule.memo = memo;

        Executors.newSingleThreadExecutor().execute(() -> {
            db.scheduleDao().insert(schedule);
        });
    }
}
