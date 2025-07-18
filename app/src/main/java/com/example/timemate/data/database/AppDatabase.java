package com.example.timemate.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.timemate.data.model.Schedule;
import com.example.timemate.data.model.User;
import com.example.timemate.data.model.Friend;
import com.example.timemate.data.model.SharedSchedule;
import com.example.timemate.data.database.dao.ScheduleDao;
import com.example.timemate.data.database.dao.UserDao;
import com.example.timemate.data.database.dao.FriendDao;
import com.example.timemate.data.database.dao.SharedScheduleDao;
import com.example.timemate.ScheduleReminder;
import com.example.timemate.ScheduleReminderDao;

/**
 * TimeMate 앱의 메인 데이터베이스
 * Room 데이터베이스 설정 및 관리
 */
@Database(
    entities = {Schedule.class, User.class, Friend.class, SharedSchedule.class, ScheduleReminder.class},
    version = 6,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    // DAO 인터페이스들
    public abstract ScheduleDao scheduleDao();
    public abstract UserDao userDao();
    public abstract FriendDao friendDao();
    public abstract SharedScheduleDao sharedScheduleDao();
    public abstract ScheduleReminderDao scheduleReminderDao();

    // 싱글톤 인스턴스
    private static volatile AppDatabase INSTANCE;

    /**
     * 데이터베이스 인스턴스 가져오기 (싱글톤 패턴)
     */
    public static AppDatabase getInstance(final Context context) {
        return getDatabase(context);
    }

    /**
     * 데이터베이스 인스턴스 가져오기 (싱글톤 패턴)
     */
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "timemate_database"
                    )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration() // 개발 중에만 사용
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 데이터베이스 마이그레이션 v1 -> v2
     * User 테이블 추가
     */
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // User 테이블 생성
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `users` (" +
                "`userId` TEXT NOT NULL, " +
                "`nickname` TEXT, " +
                "`email` TEXT, " +
                "`profileImage` TEXT, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`lastLoginAt` INTEGER NOT NULL, " +
                "`isActive` INTEGER NOT NULL, " +
                "PRIMARY KEY(`userId`))"
            );
        }
    };

    /**
     * 데이터베이스 마이그레이션 v2 -> v3
     * Friend 테이블 추가
     */
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Friend 테이블 생성
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `friends` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`userId` TEXT, " +
                "`friendUserId` TEXT, " +
                "`friendNickname` TEXT, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`isAccepted` INTEGER NOT NULL, " +
                "`isBlocked` INTEGER NOT NULL)"
            );
        }
    };

    /**
     * 데이터베이스 마이그레이션 v3 -> v4
     * Schedule 테이블 추가
     */
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Schedule 테이블 생성
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `schedules` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`userId` TEXT NOT NULL, " +
                "`title` TEXT NOT NULL, " +
                "`date` TEXT NOT NULL, " +
                "`time` TEXT NOT NULL, " +
                "`departure` TEXT, " +
                "`destination` TEXT, " +
                "`memo` TEXT, " +
                "`isCompleted` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL)"
            );
        }
    };

    /**
     * 데이터베이스 마이그레이션 v4 -> v5
     * ScheduleReminder 테이블 추가
     */
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // ScheduleReminder 테이블 생성
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `schedule_reminder` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`schedule_id` INTEGER NOT NULL, " +
                "`user_id` TEXT, " +
                "`title` TEXT, " +
                "`appointment_time` TEXT, " +
                "`departure` TEXT, " +
                "`destination` TEXT, " +
                "`optimal_transport` TEXT, " +
                "`duration_minutes` INTEGER NOT NULL, " +
                "`recommended_departure_time` TEXT, " +
                "`distance` TEXT, " +
                "`route_summary` TEXT, " +
                "`toll_fare` TEXT, " +
                "`fuel_price` TEXT, " +
                "`created_at` INTEGER NOT NULL, " +
                "`notification_sent` INTEGER NOT NULL, " +
                "`is_active` INTEGER NOT NULL)"
            );
        }
    };

    /**
     * 데이터베이스 마이그레이션 v5 -> v6
     * Schedule 테이블에 routeInfo, selectedTransportModes 필드 추가
     */
    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Schedule 테이블에 새 필드 추가
            database.execSQL("ALTER TABLE schedules ADD COLUMN routeInfo TEXT");
            database.execSQL("ALTER TABLE schedules ADD COLUMN selectedTransportModes TEXT");
        }
    };

    /**
     * 데이터베이스 초기화 (개발/테스트용)
     */
    public static void destroyInstance() {
        INSTANCE = null;
    }

    /**
     * 데이터베이스 백업 (향후 구현)
     */
    public void backup() {
        // TODO: 데이터베이스 백업 로직 구현
    }

    /**
     * 데이터베이스 복원 (향후 구현)
     */
    public void restore() {
        // TODO: 데이터베이스 복원 로직 구현
    }
}
