package com.example.videouploader;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.Objects;

public class directoryService extends Service {

    static File localPath = new File(Objects.requireNonNull(System.getenv("EXTERNAL_STORAGE")));
    static File tempDir = new File(localPath + "/" + Environment.DIRECTORY_DOWNLOADS + "/tempDir");


    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(getApplicationContext(), "Service created and started.", Toast.LENGTH_SHORT).show();
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
        boolean delete = false;
        if(exist){
            try {
                delete = deleteDirectory(tempDir);
            } catch (Error e){
                System.out.println(e);
            }

        }
        Toast.makeText(getApplicationContext(), "exist:" + exist + "\ndelete:" + delete, Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        Toast.makeText(getApplicationContext(), "Service now destroyed.", Toast.LENGTH_SHORT).show();

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
