package com.example.timemate;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {
        User.class,
        Friend.class,
        Schedule.class,
        Participant.class,
        Notification.class,
        ScheduleReminder.class
}, version = 7)  // DB version 증가
public abstract class AppDatabase extends RoomDatabase {
    public abstract ScheduleDao scheduleDao();
    public abstract UserDao userDao();
    public abstract FriendDao friendDao();
    public abstract ParticipantDao participantDao();
    public abstract NotificationDao notificationDao();
    public abstract ScheduleReminderDao scheduleReminderDao();
}