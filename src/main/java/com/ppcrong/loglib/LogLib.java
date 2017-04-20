package com.ppcrong.loglib;

import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import com.socks.library.KLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Log Library
 */
public class LogLib {

    //region External Storage

    //region Common

    /**
     * Ctor
     */
    public LogLib() {

    }

    /**
     * Generate a file name by current time
     * @param prefix Prefix
     * @param postfix Postfix
     * @param ext File extension name
     * @return File name
     */
    public String genFileName(@NonNull String prefix, @NonNull String postfix, @NonNull String ext) {
        SimpleDateFormat logFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        String fileName = "";
        if (prefix.isEmpty()) {
            fileName += logFormat.format(new Date());
        } else {
            fileName += prefix + "_" + logFormat.format(new Date());
        }
        if (postfix.isEmpty()) {
            fileName += "." + ext;
        } else {
            fileName += "_" + postfix + "." + ext;
        }
        KLog.d("fileName: " + fileName);
        return fileName;
    }

    /**
     * Create directory in external storage
     *
     * @param subDir The subfolder in external storage
     * @return The directory
     */
    public File getExDir(String subDir) {
        KLog.d("subDir: " + subDir);
        // Get the subFolder of external storage.
        File file = new File(Environment.getExternalStorageDirectory(), subDir);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                KLog.e("subDir not created");
            }
        }
        return file;
    }

    /**
     * Checks if external storage is available for read and write
     *
     * @return true is writable, false is not
     */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        KLog.d("External Storage isn't writable");
        return false;
    }

    /**
     * Checks if external storage is available to at least read
     *
     * @return true is readable, false is not
     */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        KLog.d("External Storage isn't readable");
        return false;
    }
    //endregion

    //region Logging
    private Object mLock = new Object();
    private File fileLog = null;
    private FileOutputStream mFileOutputStream = null;
    private OutputStreamWriter mOutputStreamWriter = null;

    /**
     * Open/Create log file
     *
     * @param fileDir  The dir of file
     * @param fileName The log file name
     * @return true is open ok, false is open fail
     */
    public boolean openLogFile(File fileDir, String fileName) {
        KLog.d("fileLog: " + fileDir.getPath() + File.separator + fileName);

        boolean bRet = false;
        if (isExternalStorageWritable()) {
            synchronized (mLock) {
                // Create file
                fileLog = new File(fileDir, fileName);
                try {
                    if (fileLog.exists()) {
                        boolean b = fileLog.delete();
                        KLog.d("Delete " + (b ? "ok" : "fail"));
                    }
                    boolean b = fileLog.createNewFile();
                    KLog.d("Create " + (b ? "ok" : "fail"));
                    mFileOutputStream = new FileOutputStream(fileLog);
                    mOutputStreamWriter = new OutputStreamWriter(mFileOutputStream);
                    bRet = true;
                } catch (FileNotFoundException e) {
                    KLog.e(Log.getStackTraceString(e));
                } catch (IOException e) {
                    KLog.e(Log.getStackTraceString(e));
                } catch (Exception e) {
                    KLog.e(Log.getStackTraceString(e));
                }
            }
        }

        return bRet;
    }

    /**
     * Write data to log file
     *
     * @param data The data to write
     */
    public void writeLog(String data) {
        synchronized (mLock) {
            try {
                if (mOutputStreamWriter != null) mOutputStreamWriter.write(data);
            } catch (IOException e) {
                KLog.e(Log.getStackTraceString(e));
            }
        }
    }

    /**
     * Close log file
     */
    public void closeLogFile() {
        if (fileLog == null) {
            KLog.d("fileLog is null ");
            return;
        }
        KLog.d("fileLog: " + fileLog.getPath());
        synchronized (mLock) {
            if (mOutputStreamWriter != null) {
                try {
                    mOutputStreamWriter.flush();
                } catch (IOException e) {
                    KLog.e(Log.getStackTraceString(e));
                } finally {
                    try {
                        mOutputStreamWriter.close();
                    } catch (IOException e) {
                        KLog.e(Log.getStackTraceString(e));
                    } finally {
                        mOutputStreamWriter = null;
                        fileLog = null;
                    }
                }
            }
        }
    }

    /**
     * Close log file and return path
     * @return File path
     */
    public String closeLogFileReturnPath() {
        if (fileLog == null) {
            KLog.d("fileLog is null ");
            return "";
        }
        String path = fileLog.getPath();
        KLog.d("fileLog: " + path);
        synchronized (mLock) {
            if (mOutputStreamWriter != null) {
                try {
                    mOutputStreamWriter.flush();
                } catch (IOException e) {
                    KLog.e(Log.getStackTraceString(e));
                } finally {
                    try {
                        mOutputStreamWriter.close();
                    } catch (IOException e) {
                        KLog.e(Log.getStackTraceString(e));
                    } finally {
                        mOutputStreamWriter = null;
                        fileLog = null;
                    }
                }
            }
        }
        return path;
    }
    //endregion

    //region Save/Read File

    /**
     * Check external storage writable and save data to file
     *
     * @param fileDir  The dir to save
     * @param fileName The file to save
     * @param data     The data to save
     */
    synchronized public void saveFile(File fileDir, String fileName, String data) {
        KLog.d("file: " + fileDir.getPath() + File.separator + fileName);
        if (isExternalStorageWritable()) {
            writeToFile(fileDir, fileName, data);
        }
    }

    /**
     * Write data to file
     *
     * @param fileDir  The dir to write
     * @param fileName The file to save
     * @param data     The data to write
     */
    synchronized private void writeToFile(File fileDir, String fileName, String data) {
        KLog.d("file: " + fileDir.getPath() + File.separator + fileName);

        // Create file
        File fileWrite = new File(fileDir, fileName);
        try {
            if (fileWrite.exists()) {
                boolean b = fileWrite.delete();
                KLog.d("Delete " + (b ? "ok" : "fail"));
            }
            boolean b = fileWrite.createNewFile();
            KLog.d("Create " + (b ? "ok" : "fail"));
        } catch (IOException e) {
            KLog.e(Log.getStackTraceString(e));
        } catch (Exception e) {
            KLog.e(Log.getStackTraceString(e));
        }

        // Write data to file
        FileOutputStream s = null;
        try {
            s = new FileOutputStream(fileWrite);
            if (s != null) {
                s.write(data.getBytes());
                s.flush();
            }
        } catch (FileNotFoundException e) {
            KLog.e(Log.getStackTraceString(e));
        } catch (IOException e) {
            KLog.e(Log.getStackTraceString(e));
        } catch (Exception e) {
            KLog.e(Log.getStackTraceString(e));
        } finally {
            try {
                if (s != null) s.close();
            } catch (Exception e) {
                KLog.e(Log.getStackTraceString(e));
            }
        }
    }

    /**
     * Check external storage readable and read data from file
     *
     * @param fileDir  The dir to read
     * @param fileName The file to read
     * @return The read data
     */
    synchronized public String readFile(File fileDir, String fileName) {
        KLog.d("file: " + fileDir.getPath() + File.separator + fileName);
        if (isExternalStorageReadable()) {
            return readFromFile(fileDir, fileName);
        }
        return "Error read file";
    }

    synchronized private String readFromFile(File fileDir, String fileName) {
        KLog.d("file: " + fileDir.getPath() + File.separator + fileName);

        // The read file
        File fileRead = new File(fileDir, fileName);
        if (!fileRead.exists()) {
            KLog.d("File doesn't exist");
            return "File doesn't exist";
        }

        // Read data from file
        FileInputStream s = null;
        StringBuilder sb = new StringBuilder();
        try {
            s = new FileInputStream(fileRead);
            if (s != null) {
                byte[] data = new byte[s.available()];
                while (s.read(data) != -1) {
                    sb.append(new String(data));
                }
            }
        } catch (FileNotFoundException e) {
            KLog.e(Log.getStackTraceString(e));
        } catch (IOException e) {
            KLog.e(Log.getStackTraceString(e));
        } catch (Exception e) {
            KLog.e(Log.getStackTraceString(e));
        } finally {
            try {
                if (s != null) s.close();
            } catch (Exception e) {
                KLog.e(Log.getStackTraceString(e));
            }
        }

        KLog.d("Read from file: " + sb.toString());
        return sb.toString();
    }
    //endregion

    //endregion
}
