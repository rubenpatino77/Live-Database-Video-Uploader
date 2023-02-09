package com.example.videouploader;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.List;

public final class databaseHandler {

    private final static File tempDir = directoryService.tempDir;
    private final static String DB_DIRECTORY_NAME = "VideoUploaderVideos";

    public static void retrieveVideosFromDb(Context context, List<File> allFiles,CustomAdapter customAdapter, StorageReference storageRef){
        Intent dirService = new Intent(context, directoryService.class);
        context.startService(dirService);

        StorageReference downloadFrom = storageRef.child(DB_DIRECTORY_NAME);

        downloadFrom.listAll()
                .addOnSuccessListener(new OnSuccessListener<ListResult>() {
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
                                    Toast.makeText(context, "Success in getting file", Toast.LENGTH_LONG).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(context, "Failed getting file", Toast.LENGTH_LONG).show();
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
        StorageReference uploadLocation = storageRef.child("VideoUploaderVideos").child(uri.getLastPathSegment());
        UploadTask uploadTask = uploadLocation.putFile(uri);

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                Toast.makeText(context, "Failed to access database", Toast.LENGTH_LONG).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                Toast.makeText(context, "Successfully entered database", Toast.LENGTH_LONG).show();
            }
        });
    }
}
