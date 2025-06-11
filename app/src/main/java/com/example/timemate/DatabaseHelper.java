package com.example.timemate;

import android.content.Context;
import android.util.Log;
import java.io.File;

import java.io.File;

public class DatabaseHelper {
    
    private static final String DATABASE_NAME = "timeMate-db";

    /**
     * 데이터베이스 디렉토리 존재 확인 및 생성
     */
    public static boolean ensureDatabaseDirectoryExists(Context context) {
        try {
            // 앱의 데이터베이스 디렉토리 경로
            File databaseDir = new File(context.getApplicationInfo().dataDir, "databases");

            if (!databaseDir.exists()) {
                Log.d("DatabaseHelper", "📁 데이터베이스 디렉토리 생성: " + databaseDir.getAbsolutePath());
                boolean created = databaseDir.mkdirs();
                Log.d("DatabaseHelper", "📁 디렉토리 생성 결과: " + created);
                return created;
            } else {
                Log.d("DatabaseHelper", "✅ 데이터베이스 디렉토리 이미 존재: " + databaseDir.getAbsolutePath());
                return true;
            }

        } catch (Exception e) {
            Log.e("DatabaseHelper", "❌ 데이터베이스 디렉토리 생성 오류", e);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 개발 단계에서 데이터베이스를 완전히 삭제하는 메서드
     * 스키마 변경 시 사용
     */
    public static boolean deleteDatabase(Context context) {
        try {
            android.util.Log.d("DatabaseHelper", "🔍 [DEBUG] 데이터베이스 삭제 시작");

            // Context 유효성 검사
            if (context == null) {
                android.util.Log.e("DatabaseHelper", "🔍 [DEBUG] Context가 null입니다");
                return false;
            }

            // 데이터베이스 파일 경로 가져오기
            File databaseFile = null;
            try {
                databaseFile = context.getDatabasePath(DATABASE_NAME);
                android.util.Log.d("DatabaseHelper", "🔍 [DEBUG] 데이터베이스 경로: " +
                                 (databaseFile != null ? databaseFile.getAbsolutePath() : "null"));
            } catch (Exception e) {
                android.util.Log.e("DatabaseHelper", "🔍 [DEBUG] 데이터베이스 경로 가져오기 실패", e);
                return false;
            }

            if (databaseFile == null) {
                android.util.Log.e("DatabaseHelper", "🔍 [DEBUG] 데이터베이스 파일 경로가 null입니다");
                return false;
            }

            // 디렉토리 존재 여부 확인 및 생성
            File parentDir = databaseFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                android.util.Log.d("DatabaseHelper", "🔍 [DEBUG] 데이터베이스 디렉토리 생성: " + parentDir.getPath());
                try {
                    if (!parentDir.mkdirs()) {
                        android.util.Log.w("DatabaseHelper", "🔍 [DEBUG] 데이터베이스 디렉토리 생성 실패");
                        // 디렉토리 생성 실패해도 계속 진행 (이미 존재할 수 있음)
                    }
                } catch (SecurityException e) {
                    android.util.Log.e("DatabaseHelper", "🔍 [DEBUG] 디렉토리 생성 권한 오류", e);
                    return false;
                }
            }

            // 관련 파일들 삭제
            boolean deleted = true;

            try {
                if (databaseFile.exists()) {
                    android.util.Log.d("DatabaseHelper", "🔍 [DEBUG] 메인 데이터베이스 파일 삭제 시도");
                    deleted &= safeDeleteFile(databaseFile, "database");
                }

                // WAL 파일 삭제
                File walFile = new File(databaseFile.getPath() + "-wal");
                if (walFile.exists()) {
                    android.util.Log.d("DatabaseHelper", "🔍 [DEBUG] WAL 파일 삭제 시도");
                    deleted &= safeDeleteFile(walFile, "WAL");
                }

                // SHM 파일 삭제
                File shmFile = new File(databaseFile.getPath() + "-shm");
                if (shmFile.exists()) {
                    android.util.Log.d("DatabaseHelper", "🔍 [DEBUG] SHM 파일 삭제 시도");
                    deleted &= safeDeleteFile(shmFile, "SHM");
                }
            } catch (Exception e) {
                android.util.Log.e("DatabaseHelper", "🔍 [DEBUG] 파일 삭제 중 예외 발생", e);
                deleted = false;
            }

            android.util.Log.d("DatabaseHelper", "🔍 [DEBUG] 데이터베이스 삭제 완료: " + deleted);
            return deleted;
        } catch (SecurityException e) {
            android.util.Log.e("DatabaseHelper", "Security exception accessing database files", e);
            return false;
        } catch (Exception e) {
            android.util.Log.e("DatabaseHelper", "Error deleting database", e);
            return false;
        }
    }

    /**
     * 파일 안전 삭제 (예외 처리 포함)
     */
    private static boolean safeDeleteFile(File file, String fileType) {
        try {
            if (file == null) {
                android.util.Log.w("DatabaseHelper", "🔍 [DEBUG] " + fileType + " 파일이 null입니다");
                return false;
            }

            android.util.Log.d("DatabaseHelper", "🔍 [DEBUG] " + fileType + " 파일 삭제 시도: " + file.getAbsolutePath());

            if (!file.exists()) {
                android.util.Log.d("DatabaseHelper", "🔍 [DEBUG] " + fileType + " 파일이 존재하지 않음");
                return true; // 이미 없으므로 성공으로 간주
            }

            if (!file.isFile()) {
                android.util.Log.w("DatabaseHelper", "🔍 [DEBUG] " + fileType + " 경로가 파일이 아님");
                return false;
            }

            boolean deleted = file.delete();
            if (deleted) {
                android.util.Log.d("DatabaseHelper", "🔍 [DEBUG] " + fileType + " 파일 삭제 성공");
            } else {
                android.util.Log.w("DatabaseHelper", "🔍 [DEBUG] " + fileType + " 파일 삭제 실패: " + file.getPath());
            }
            return deleted;

        } catch (SecurityException e) {
            android.util.Log.e("DatabaseHelper", "🔍 [DEBUG] " + fileType + " 파일 삭제 권한 오류", e);
            return false;
        } catch (Exception e) {
            android.util.Log.e("DatabaseHelper", "🔍 [DEBUG] " + fileType + " 파일 삭제 중 오류", e);
            return false;
        }
    }
    
    /**
     * 데이터베이스가 존재하는지 확인 (안전한 파일 접근)
     */
    public static boolean databaseExists(Context context) {
        try {
            File databaseFile = context.getDatabasePath(DATABASE_NAME);
            return databaseFile != null && databaseFile.exists() && databaseFile.isFile();
        } catch (SecurityException e) {
            android.util.Log.e("DatabaseHelper", "Security exception checking database existence", e);
            return false;
        } catch (Exception e) {
            android.util.Log.e("DatabaseHelper", "Error checking database existence", e);
            return false;
        }
    }


}
