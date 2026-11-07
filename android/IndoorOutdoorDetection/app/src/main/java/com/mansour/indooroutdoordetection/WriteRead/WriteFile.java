package com.mansour.indooroutdoordetection.WriteRead;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.mansour.indooroutdoordetection.MyApplication;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Safe cross-version external writer.
 * Works on Android 8–14 (API 26–35) with automatic fallback to app sandbox.
 *
 * 1️⃣ If MANAGE_EXTERNAL_STORAGE is granted → writes to /Download/dirName/fileName
 * 2️⃣ Otherwise → writes to app sandbox (/Android/data/.../files/IOD_Record/)
 */
public class WriteFile {

    private static final String TAG = "WriteFile";

    /** Write text with newline at the end */

    /** Write text without forcing newline */
    public static void writeTxtToFiles(String dirName, String fileName, String content) {
        writeInternal(dirName, fileName, content, false);
    }

    /** Core safe writer logic */
    private static void writeInternal(String dirName, String fileName, String content, boolean addNewline) {
        if (TextUtils.isEmpty(content)) return;

        try {
            File targetDir = getWritableDirectory(dirName);
            if (targetDir == null) {
                Log.e(TAG, "❌ No writable directory available.");
                return;
            }

            if (!targetDir.exists() && !targetDir.mkdirs()) {
                Log.e(TAG, "❌ Failed to create directory: " + targetDir.getAbsolutePath());
                // 🔁 Fallback to app sandbox
                Context ctx = MyApplication.getAppContext();
                targetDir = new File(ctx.getExternalFilesDir(null), "IOD_Fallback");
                if (!targetDir.exists()) targetDir.mkdirs();
                Log.w(TAG, "➡️ Falling back to sandbox: " + targetDir.getAbsolutePath());
            }


            if (TextUtils.isEmpty(fileName)) {
                fileName = String.valueOf(System.currentTimeMillis()) + ".txt";
            }

            File targetFile = new File(targetDir, fileName);
            if (!targetFile.exists()) targetFile.createNewFile();

            try (RandomAccessFile raf = new RandomAccessFile(targetFile, "rwd")) {
                raf.seek(targetFile.length());
                raf.write(content.getBytes());
                if (addNewline) raf.write("\r\n".getBytes());
            }

            Log.i(TAG, "✅ Written to: " + targetFile.getAbsolutePath());

        } catch (IOException e) {
            Log.e(TAG, "⚠️ Write error: " + e);
        }
    }

    /**
     * Automatically chooses best location for writing based on API & permissions.
     */
    private static File getWritableDirectory(String dirName) {
        Context ctx = MyApplication.getAppContext();
        if (TextUtils.isEmpty(dirName)) dirName = "IOD_Record";

        // 1️⃣ Prefer public Download dir if allowed
        if (canWritePublicStorage()) {
            File publicDir = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS), dirName);
            Log.d(TAG, "Using public directory: " + publicDir.getAbsolutePath());
            return publicDir;
        }

        // 2️⃣ Otherwise fallback to sandboxed app files
        File sandbox = new File(ctx.getExternalFilesDir(null), dirName);
        Log.d(TAG, "Using sandbox directory: " + sandbox.getAbsolutePath());
        return sandbox;
    }

    /**
     * Detects if app can write to /storage/emulated/0/Download/
     */
    private static boolean canWritePublicStorage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // Android 10 or below → WRITE_EXTERNAL_STORAGE still works
            return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        } else {
            // Android 11+ → require MANAGE_EXTERNAL_STORAGE
            return Environment.isExternalStorageManager();
        }
    }
    static public void writeTxtToFilesWithEnter(String dirName, String fileName, String content) {
        // 🔒 Global switch — turn OFF writing easily
        final boolean ENABLE_WRITE = true;  // set to false to disable

        if (!ENABLE_WRITE) {
            Log.w("WriteFile", "⚠️ File writing disabled (ENABLE_WRITE=false)");
            return;
        }

        if (content == null) {
            Log.w("WriteFile", "⚠️ Null content, nothing written");
            return;
        }

        String strContent = content + "\r\n";

        try {
            // ✅ Base path — public Download by default
            File parent_path = MyApplication.getAppContext().getExternalFilesDir(null);
            Log.d("WriteFile", "Using public directory: " + parent_path.getAbsolutePath());

            if (TextUtils.isEmpty(fileName)) {
                fileName = String.valueOf(System.currentTimeMillis());
            }
            if (TextUtils.isEmpty(dirName)) {
                dirName = "IOD_Test";
            }

            // ✅ Create directory
            File dirFile = new File(parent_path, dirName);
            if (!dirFile.exists() && !dirFile.mkdirs()) {
                Log.e("WriteFile", "❌ Failed to create public directory: " + dirFile.getAbsolutePath());
                // 🔁 Fallback for Android 11+
                File fallbackDir = MyApplication.getAppContext().getExternalFilesDir("IOD_Fallback");
                if (fallbackDir != null && (fallbackDir.exists() || fallbackDir.mkdirs())) {
                    dirFile = fallbackDir;
                    Log.w("WriteFile", "➡️ Fallback to sandbox: " + dirFile.getAbsolutePath());
                } else {
                    Log.e("WriteFile", "❌ No writable fallback directory found");
                    return;
                }
            }

            // ✅ Prepare file
            File mFile = new File(dirFile, fileName);
            if (!mFile.exists()) {
                mFile.createNewFile();
                Log.i("WriteFile", "📄 Created new file: " + mFile.getAbsolutePath());
            }

            // ✅ Append text with newline
            RandomAccessFile raf = new RandomAccessFile(mFile, "rwd");
            raf.seek(mFile.length());
            raf.write(strContent.getBytes());
            raf.close();

            Log.i("WriteFile", "✅ Written line to: " + mFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e("WriteFile", "❌ Error writing file: " + e.getMessage(), e);
        }
    }

    /**
     * Optional helper to prompt user for “All Files Access” on Android 11+
     */
    @RequiresApi(api = Build.VERSION_CODES.R)
    public static void requestAllFilesAccess(Context context) {
        try {
            context.startActivity(new android.content.Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    android.net.Uri.parse("package:" + context.getPackageName())
            ));
        } catch (Exception e) {
            Log.e(TAG, "Failed to request all files access", e);
        }
    }
}


//package com.mansour.indooroutdoordetection.WriteRead;
//
//import static android.os.Environment.DIRECTORY_DOWNLOADS;
//
//import android.os.Environment;
//import android.text.TextUtils;
//import android.util.Log;
//
//import com.mansour.indooroutdoordetection.MyApplication;
//
//import java.io.File;
//import java.io.RandomAccessFile;
//
//
///**
// * Created by 35859 on 2018/1/11.
// */
//
//public class WriteFile {
//    static public void writeTxtToFilesWithEnter(String dirName, String fileName, String content) {
//        //TODO add switch to limit program to write file
//        boolean K = true;
//        if (K) {
//            String strContent = content + "\r\n";
////        Log.i("SAVE NAME","dirName="+dirName+",fileName="+fileName);
//            //storage/emulated/0/Download
//            File parent_path = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);
//            //if (TextUtils.isEmpty(dirName)){
//            //    dirName = "20180121";
//            //}
//            if (TextUtils.isEmpty(fileName)) {
//                fileName = String.valueOf(System.currentTimeMillis());
//            }
//            //storage/emulated/0/Download/dirName
//            String dirPath = parent_path.getAbsolutePath() + "/" + dirName;
//            try {
//                File dirFile = new File(dirPath);
//                if (!dirFile.exists()) {
//                    dirFile.mkdir();
//                }
//                //storage/emulated/0/Download/dirName/filename
//
//                String strFilePath = dirPath + "/" + fileName;
//
//                File mFile = new File(strFilePath);
//                if (!mFile.exists()) {
//                    mFile.createNewFile();
//                } else {
//
//
//                }
//
//                RandomAccessFile raf = new RandomAccessFile(mFile, "rwd");
//                raf.seek(mFile.length());
//                raf.write(strContent.getBytes());
//                raf.close();
//            } catch (Exception e) {
//                Log.e("TestFile", "Error on write File:" + e);
//            }
//        }
//    }
//    static public void writeTxtToFiles(String dirName, String fileName, String content) {
//        boolean Record=true;
//        if (content==null){
//            Record = false;
//        }
//        if(Record) {
//            String strContent = content ;
////        Log.i("SAVE NAME","dirName="+dirName+",fileName="+fileName);
//            //storage/emulated/0/Download
//            File parent_path = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);
////            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "sample-take-image")
//            if (TextUtils.isEmpty(dirName)){
//                dirName = "io";
//            }
//            if (TextUtils.isEmpty(fileName)) {
//                fileName = String.valueOf(System.currentTimeMillis());
//            }
//            //storage/emulated/0/Download/dirName
//            String dirPath = parent_path.getAbsolutePath() + "/" + dirName;
//            try {
//                File dirFile = new File(dirPath);
//                if (!dirFile.exists()) {
//                    dirFile.mkdir();
//                }
//                //storage/emulated/0/Download/dirName/filename
//                String strFilePath = dirPath + "/" + fileName;
//
//                File mFile = new File(strFilePath);
//                if (!mFile.exists()) {
//                    mFile.createNewFile();
//                    RandomAccessFile raf = new RandomAccessFile(mFile, "rwd");
//                    raf.seek(mFile.length());
//                    raf.write(strContent.getBytes());
//                    raf.close();
//                } else {
//                    RandomAccessFile raf = new RandomAccessFile(mFile, "rwd");
//                    raf.seek(mFile.length());
//                    raf.write(strContent.getBytes());
//                    raf.close();
//                }
////            Log.i("SAVE FILE","parentFile="+parent_path+",mFile="+mFile.getAbsolutePath());
//
//
//            } catch (Exception e) {
//                Log.e("TestFile", "here:" + e);
//            }
//        }
//    }
//    static public void writeTxtToFilesTwoDir(String dirName,String dirNameInside, String fileName, String content) {
//        boolean Record=true;
//        if (content==null){
//            Record = false;
//        }
//        if(Record) {
//            String strContent = content ;
////        Log.i("SAVE NAME","dirName="+dirName+",fileName="+fileName);
//            //storage/emulated/0/Download
//            File parent_path = MyApplication.getAppContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
////            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "sample-take-image")
//            if (TextUtils.isEmpty(dirName)){
//                dirName = "20218";
//            }
//            if (TextUtils.isEmpty(dirNameInside)){
//                dirNameInside = String.valueOf(System.currentTimeMillis());
//            }
//            if (TextUtils.isEmpty(fileName)) {
//                fileName = String.valueOf(System.currentTimeMillis());
//            }
//            //storage/emulated/0/Download/dirName
//            String dirPath = parent_path.getAbsolutePath() + "/" + dirName;
//            try {
//                File dirFile = new File(dirPath);
//                if (!dirFile.exists()) {
//                    dirFile.mkdir();
//                }
//                String dirPathInside = parent_path.getAbsolutePath() + "/" + dirName+ "/" +dirNameInside;
//                File dirFileInside = new File(dirPathInside);
//                if (!dirFileInside.exists()) {
//                    dirFileInside.mkdir();
//                }
//                //storage/emulated/0/Download/dirName/filename
//                String strFilePath = dirFileInside + "/" + fileName;
//
//                File mFile = new File(strFilePath);
//                if (!mFile.exists()) {
//                    mFile.createNewFile();
//                    RandomAccessFile raf = new RandomAccessFile(mFile, "rwd");
//                    raf.seek(mFile.length());
//                    raf.write(strContent.getBytes());
//                    raf.close();
//                } else {
//                    RandomAccessFile raf = new RandomAccessFile(mFile, "rwd");
//                    raf.seek(mFile.length());
//                    raf.write(strContent.getBytes());
//                    raf.close();
//                }
////            Log.i("SAVE FILE","parentFile="+parent_path+",mFile="+mFile.getAbsolutePath());
//
//
//            } catch (Exception e) {
//                Log.e("TestFile", "here:" + e);
//            }
//        }
//    }
//
//}
