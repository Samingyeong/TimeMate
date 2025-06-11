package com.example.timemate.core.util;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.File;

/**
 * 파일 접근 안전성을 위한 헬퍼 클래스
 * "File error accessing recents directory" 등의 오류 방지
 */
public class FileAccessHelper {
    
    private static final String TAG = "FileAccessHelper";
    
    /**
     * 안전한 디렉토리 생성
     * @param context 앱 컨텍스트
     * @param dirName 디렉토리 이름
     * @return 생성된 디렉토리 File 객체 (실패 시 null)
     */
    public static File createSafeDirectory(Context context, String dirName) {
        try {
            if (context == null || dirName == null || dirName.trim().isEmpty()) {
                Log.e(TAG, "🔍 [DEBUG] Invalid parameters for directory creation");
                return null;
            }

            // 내부 저장소 사용 (권한 불필요)
            File baseDir = context.getFilesDir();
            if (baseDir == null) {
                Log.e(TAG, "🔍 [DEBUG] Cannot get files directory");
                return null;
            }

            File targetDir = new File(baseDir, dirName);
            
            if (!targetDir.exists()) {
                Log.d(TAG, "🔍 [DEBUG] Creating directory: " + targetDir.getAbsolutePath());
                boolean created = targetDir.mkdirs();
                if (!created) {
                    Log.e(TAG, "🔍 [DEBUG] Failed to create directory: " + targetDir.getAbsolutePath());
                    return null;
                }
            }

            // 디렉토리 접근 가능 여부 확인
            if (!targetDir.canRead() || !targetDir.canWrite()) {
                Log.e(TAG, "🔍 [DEBUG] Directory not accessible: " + targetDir.getAbsolutePath());
                return null;
            }

            Log.d(TAG, "🔍 [DEBUG] Directory ready: " + targetDir.getAbsolutePath());
            return targetDir;

        } catch (SecurityException e) {
            Log.e(TAG, "🔍 [DEBUG] Security exception creating directory", e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "🔍 [DEBUG] Exception creating directory", e);
            return null;
        }
    }

    /**
     * 안전한 파일 생성
     * @param context 앱 컨텍스트
     * @param dirName 디렉토리 이름
     * @param fileName 파일 이름
     * @return 생성된 파일 File 객체 (실패 시 null)
     */
    public static File createSafeFile(Context context, String dirName, String fileName) {
        try {
            File directory = createSafeDirectory(context, dirName);
            if (directory == null) {
                return null;
            }

            if (fileName == null || fileName.trim().isEmpty()) {
                Log.e(TAG, "🔍 [DEBUG] Invalid file name");
                return null;
            }

            File targetFile = new File(directory, fileName);
            Log.d(TAG, "🔍 [DEBUG] Target file: " + targetFile.getAbsolutePath());
            
            return targetFile;

        } catch (Exception e) {
            Log.e(TAG, "🔍 [DEBUG] Exception creating file", e);
            return null;
        }
    }

    /**
     * 파일 존재 여부 안전 확인
     * @param file 확인할 파일
     * @return 존재 여부 (오류 시 false)
     */
    public static boolean safeFileExists(File file) {
        try {
            return file != null && file.exists() && file.isFile();
        } catch (SecurityException e) {
            Log.e(TAG, "🔍 [DEBUG] Security exception checking file existence", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "🔍 [DEBUG] Exception checking file existence", e);
            return false;
        }
    }

    /**
     * 디렉토리 존재 여부 안전 확인
     * @param directory 확인할 디렉토리
     * @return 존재 여부 (오류 시 false)
     */
    public static boolean safeDirectoryExists(File directory) {
        try {
            return directory != null && directory.exists() && directory.isDirectory();
        } catch (SecurityException e) {
            Log.e(TAG, "🔍 [DEBUG] Security exception checking directory existence", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "🔍 [DEBUG] Exception checking directory existence", e);
            return false;
        }
    }

    /**
     * 안전한 파일 삭제
     * @param file 삭제할 파일
     * @return 삭제 성공 여부
     */
    public static boolean safeDeleteFile(File file) {
        try {
            if (file == null) {
                Log.w(TAG, "🔍 [DEBUG] File is null, cannot delete");
                return false;
            }

            if (!file.exists()) {
                Log.d(TAG, "🔍 [DEBUG] File does not exist, consider as deleted");
                return true;
            }

            boolean deleted = file.delete();
            if (deleted) {
                Log.d(TAG, "🔍 [DEBUG] File deleted successfully: " + file.getAbsolutePath());
            } else {
                Log.w(TAG, "🔍 [DEBUG] Failed to delete file: " + file.getAbsolutePath());
            }
            return deleted;

        } catch (SecurityException e) {
            Log.e(TAG, "🔍 [DEBUG] Security exception deleting file", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "🔍 [DEBUG] Exception deleting file", e);
            return false;
        }
    }

    /**
     * 앱별 캐시 디렉토리 가져오기 (안전)
     * @param context 앱 컨텍스트
     * @return 캐시 디렉토리 (실패 시 null)
     */
    public static File getSafeCacheDir(Context context) {
        try {
            if (context == null) {
                Log.e(TAG, "🔍 [DEBUG] Context is null");
                return null;
            }

            File cacheDir = context.getCacheDir();
            if (cacheDir == null) {
                Log.e(TAG, "🔍 [DEBUG] Cannot get cache directory");
                return null;
            }

            if (!cacheDir.exists()) {
                boolean created = cacheDir.mkdirs();
                if (!created) {
                    Log.e(TAG, "🔍 [DEBUG] Failed to create cache directory");
                    return null;
                }
            }

            return cacheDir;

        } catch (Exception e) {
            Log.e(TAG, "🔍 [DEBUG] Exception getting cache directory", e);
            return null;
        }
    }

    /**
     * 앱별 파일 디렉토리 가져오기 (안전)
     * @param context 앱 컨텍스트
     * @return 파일 디렉토리 (실패 시 null)
     */
    public static File getSafeFilesDir(Context context) {
        try {
            if (context == null) {
                Log.e(TAG, "🔍 [DEBUG] Context is null");
                return null;
            }

            File filesDir = context.getFilesDir();
            if (filesDir == null) {
                Log.e(TAG, "🔍 [DEBUG] Cannot get files directory");
                return null;
            }

            if (!filesDir.exists()) {
                boolean created = filesDir.mkdirs();
                if (!created) {
                    Log.e(TAG, "🔍 [DEBUG] Failed to create files directory");
                    return null;
                }
            }

            return filesDir;

        } catch (Exception e) {
            Log.e(TAG, "🔍 [DEBUG] Exception getting files directory", e);
            return null;
        }
    }

    /**
     * Android 버전별 저장소 접근 방식 확인
     * @return 저장소 접근 정보 문자열
     */
    public static String getStorageAccessInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Android API Level: ").append(Build.VERSION.SDK_INT).append("\n");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            info.append("Scoped Storage: Enabled (API 29+)\n");
            info.append("Recommendation: Use app-specific directories\n");
        } else {
            info.append("Scoped Storage: Disabled\n");
            info.append("Recommendation: External storage available with permissions\n");
        }
        
        return info.toString();
    }

    /**
     * 디버깅용: 파일 시스템 상태 로깅
     * @param context 앱 컨텍스트
     */
    public static void logFileSystemStatus(Context context) {
        Log.d(TAG, "🔍 [DEBUG] === File System Status ===");
        Log.d(TAG, getStorageAccessInfo());
        
        try {
            File filesDir = context.getFilesDir();
            Log.d(TAG, "🔍 [DEBUG] Files Dir: " + (filesDir != null ? filesDir.getAbsolutePath() : "null"));
            
            File cacheDir = context.getCacheDir();
            Log.d(TAG, "🔍 [DEBUG] Cache Dir: " + (cacheDir != null ? cacheDir.getAbsolutePath() : "null"));
            
        } catch (Exception e) {
            Log.e(TAG, "🔍 [DEBUG] Error logging file system status", e);
        }
        
        Log.d(TAG, "🔍 [DEBUG] === End File System Status ===");
    }
}
