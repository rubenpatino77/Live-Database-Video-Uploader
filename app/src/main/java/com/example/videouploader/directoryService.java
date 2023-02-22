package com.example.videouploader;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.Objects;

public class directoryService extends Service {

    static File localPath = new File(Objects.requireNonNull(System.getenv("EXTERNAL_STORAGE")));
    static File tempDir = new File(localPath + "/" + Environment.DIRECTORY_DOWNLOADS + "/tempDir");


    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        boolean exist = tempDir.exists();
        if(exist){
            try {
                deleteDirectory(tempDir);
            } catch (Error e){
                System.out.println(e);
            }
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        stopSelf();
    }


    public static boolean deleteDirectory(File file)
    {
        // store all the paths of files and folders present inside directory
        for (File subfile : file.listFiles()) {

            // if it is a subfolder, recursively call function to empty subfolder
            if (subfile.isDirectory()) {
                deleteDirectory(subfile);
            }
            // delete files and empty subfolders
            subfile.delete();
        }
        return file.delete();
    }
}
