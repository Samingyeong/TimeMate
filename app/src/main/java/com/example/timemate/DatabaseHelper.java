package com.example.timemate;

import android.content.Context;

import java.io.File;

public class DatabaseHelper {
    
    private static final String DATABASE_NAME = "timeMate-db";
    
    /**
     * 개발 단계에서 데이터베이스를 완전히 삭제하는 메서드
     * 스키마 변경 시 사용
     */
    public static boolean deleteDatabase(Context context) {
        try {
            // 데이터베이스 파일 경로 가져오기
            File databaseFile = context.getDatabasePath(DATABASE_NAME);
            
            // 관련 파일들 삭제
            boolean deleted = true;
            
            if (databaseFile.exists()) {
                deleted &= databaseFile.delete();
            }
            
            // WAL 파일 삭제
            File walFile = new File(databaseFile.getPath() + "-wal");
            if (walFile.exists()) {
                deleted &= walFile.delete();
            }
            
            // SHM 파일 삭제
            File shmFile = new File(databaseFile.getPath() + "-shm");
            if (shmFile.exists()) {
                deleted &= shmFile.delete();
            }
            
            return deleted;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 데이터베이스가 존재하는지 확인
     */
    public static boolean databaseExists(Context context) {
        File databaseFile = context.getDatabasePath(DATABASE_NAME);
        return databaseFile.exists();
    }
}
