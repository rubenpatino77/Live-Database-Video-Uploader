package com.example.videouploader;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class databaseHandler {

    private final static File tempDir = directoryService.tempDir;
    private final static String DB_DIRECTORY_NAME = "VideoUploaderVideos";
    private static Context currentContext = null;
    private static StorageReference downloadFrom = null;


    public static void retrieveVideosFromDb(Context context, List<File> allFiles,CustomAdapter customAdapter, StorageReference storageRef){
        Intent dirService = new Intent(context, directoryService.class);
        context.startService(dirService);
        currentContext = context;

        downloadFrom = storageRef.child(DB_DIRECTORY_NAME);

        Task<ListResult> done = downloadFrom.listAll();

        while (!done.isComplete()){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        done.addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {
                        if (!tempDir.exists()) {
                            tempDir.mkdirs();
                        }

                        Toast.makeText(context, "Successfully entered database. Now getting file.", Toast.LENGTH_LONG).show();

                        for (StorageReference item : listResult.getItems()) {
                            String name = item.getName();
                            File tempFile = new File(tempDir.getPath() + "/" + name);

                            StorageReference videoInStorage = downloadFrom.child(name);

                            videoInStorage.getFile(tempFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                    Toast.makeText(currentContext, "Success in getting file", Toast.LENGTH_LONG).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(currentContext, "Failed getting file", Toast.LENGTH_LONG).show();
                                }
                            });


                            allFiles.add(tempFile);
                            customAdapter.notifyItemInserted(allFiles.size() - 1);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // An error occurred!
                        Toast.makeText(context, "Failed accessing database.", Toast.LENGTH_LONG).show();
                    }
                });
    }


    public static void uploadFileToDb(Context context, File file, StorageReference storageRef){
        Uri uri = Uri.fromFile(file);
        StorageReference uploadLocation = storageRef.child(DB_DIRECTORY_NAME).child(uri.getLastPathSegment());
        UploadTask uploadTask = uploadLocation.putFile(uri);

        while (uploadTask.isInProgress()){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                Toast.makeText(context, "Failed to access database", Toast.LENGTH_LONG).show();
                Toast.makeText(context, "Error: " + exception.toString(), Toast.LENGTH_LONG).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                Toast.makeText(context, "Successfully uploaded video", Toast.LENGTH_LONG).show();
            }
        });
    }

    public static HashMap<String, String> getDbFileNames(Context context, StorageReference storageRef){

        HashMap<String, String> map = new HashMap<>();
        downloadFrom = storageRef.child(DB_DIRECTORY_NAME);

        downloadFrom.listAll()
                .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {

                        for (StorageReference item : listResult.getItems()) {
                            map.put(item.getName(), item.getName());
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // An error occurred!
                        Toast.makeText(context, "Failed accessing database.", Toast.LENGTH_LONG).show();
                    }
                });

        return map;
    }
}
