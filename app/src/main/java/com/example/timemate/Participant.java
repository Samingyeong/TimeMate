package com.example.timemate;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(primaryKeys = {"scheduleId", "userId"})
public class Participant {

    public int scheduleId;

    @NonNull
    public String userId;

    public boolean isAccepted;
}

