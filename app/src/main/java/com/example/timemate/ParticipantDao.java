package com.example.timemate;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ParticipantDao {
    @Insert
    void insert(Participant participant);

    @Query("SELECT * FROM participant WHERE scheduleId = :scheduleId")
    List<Participant> getParticipantsForSchedule(int scheduleId);
}
