package com.example.timemate;

import java.util.concurrent.Executors;

public class ScheduleHelper {
    public static void insertSchedule(AppDatabase db, String title, String dateTime, String departure, String destination, String memo) {
        Schedule schedule = new Schedule();
        schedule.title = title;
        schedule.dateTime = dateTime;
        schedule.departure = departure;
        schedule.destination = destination;
        schedule.memo = memo;

        Executors.newSingleThreadExecutor().execute(() -> {
            db.scheduleDao().insert(schedule);
        });
    }
}
