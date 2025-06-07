package com.example.timemate;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface NotificationDao {

    @Insert
    void insert(Notification notification);

    @Update
    void update(Notification notification);

    @Query("SELECT * FROM notification ORDER BY timestamp DESC")
    List<Notification> getAllNotifications();

    @Query("SELECT * FROM notification WHERE isRead = 0 ORDER BY timestamp DESC")
    List<Notification> getUnreadNotifications();

    @Query("SELECT * FROM notification WHERE type = :type ORDER BY timestamp DESC")
    List<Notification> getNotificationsByType(String type);

    @Query("SELECT * FROM notification WHERE status = 'PENDING' AND type = 'FRIEND_INVITE' ORDER BY timestamp DESC")
    List<Notification> getPendingInvites();

    @Query("UPDATE notification SET isRead = 1 WHERE id = :notificationId")
    void markAsRead(int notificationId);

    @Query("UPDATE notification SET status = :status WHERE id = :notificationId")
    void updateStatus(int notificationId, String status);

    @Query("SELECT COUNT(*) FROM notification WHERE isRead = 0")
    int getUnreadCount();

    @Query("DELETE FROM notification WHERE id = :notificationId")
    void delete(int notificationId);

    @Query("DELETE FROM notification WHERE timestamp < :timestamp")
    void deleteOldNotifications(long timestamp);
}
