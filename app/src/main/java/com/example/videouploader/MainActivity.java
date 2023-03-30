package com.example.videouploader;


import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SelectListener {

    private StorageReference storageRef;
    RecyclerView recyclerView;
    List<File> allFiles;
    CustomAdapter customAdapter;
    Button getPhotosButton;
    private final File localPath = directoryService.localPath;
    private final File tempDir = directoryService.tempDir;
    private HashMap<String, String> dbFileNames = new HashMap<>();
    private LoadingDialogue loadingDialogue;
    File fileToDelete;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        storageRef = FirebaseStorage.getInstance().getReference();
        getPhotosButton = findViewById(R.id.getPhotosButton);
        dbFileNames = databaseHandler.getDbFileNames(this, storageRef);
        loadingDialogue = new LoadingDialogue(MainActivity.this);
        databaseHandler.setLoadingDialogue(loadingDialogue);

        getPhotosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!databaseHandler.isDbRetrieved()){
                    loadingDialogue.startLoadingDialogue();
                    getPhotosButton.setBackgroundColor(Color.RED);
                    getPhotosButton.setText("Remove database files.");
                    getDbVideos();
                    loadingDialogue.dismissLoadingDialogue();
                } else {
                    loadingDialogue.startLoadingDialogue();
                    getPhotosButton.setBackgroundColor(Color.parseColor("#004999"));
                    getPhotosButton.setText("Get uploaded photos");
                    databaseHandler.resetDbRetrieved();
                    if(tempDir.exists()){
                        directoryService.deleteDirectory(tempDir);
                    }
                    displayFiles();
                    loadingDialogue.dismissLoadingDialogue();
                }

            }
        });

        if(tempDir.getAbsoluteFile().exists()){
            directoryService.deleteDirectory(tempDir);
        }
        
        askPermission();
    }


    private void askPermission() {
        Dexter.withContext(this).withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        displayFiles();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        Toast.makeText(MainActivity.this, "Storage permission is required.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                }).check();
    }


    private void displayFiles() {
        if(tempDir.getAbsoluteFile().exists()){
            directoryService.deleteDirectory(tempDir);
        }
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        allFiles = new ArrayList<>();
        allFiles.addAll(findVideos(localPath));
        customAdapter = new CustomAdapter(this, allFiles, this);
        //customAdapter.setHasStableIds(true);
        recyclerView.setAdapter(customAdapter);

        if(databaseHandler.isDbRetrieved()){
            getDbVideos();
        }
    }


    private List<File> findVideos(File allFilesPath){
        ArrayList<File> myVideos = new ArrayList<>();
        File[] allFiles = allFilesPath.listFiles();

        if(allFiles != null){
            for (File singleFile : allFiles){
                if(singleFile.isDirectory() && !singleFile.isHidden()){
                    myVideos.addAll(findVideos(singleFile));
                } else if(singleFile.getName().toLowerCase().endsWith(".mp4")){
                    myVideos.add(singleFile);
                }
            }
        }
        return myVideos;
    }


    @Override
    public void onFileClick(File file, String videoName) {
        Intent intent = new Intent(MainActivity.this, VideoPlayerActivity.class);

        Bundle extras = new Bundle();
        extras.putString("VIDEO", file.getAbsolutePath());
        extras.putString("videoName", videoName);
        intent.putExtras(extras);

        startActivity(intent);
    }


    @Override
    public void onUploadClick(File file) {
        loadingDialogue.startLoadingDialogue();

        if(!dbFileNames.containsKey(file.getName())){
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    databaseHandler.uploadFileToDb(getBaseContext(), file, storageRef, allFiles, customAdapter);
                    dbFileNames = databaseHandler.getDbFileNames(MainActivity.this, storageRef);
                }
            }, 1000);
        } else {
            Toast.makeText(getBaseContext(), "File Already exists in the database.", Toast.LENGTH_LONG).show();
            loadingDialogue.dismissLoadingDialogue();
        }
    }



    @Override
    public void onDownloadClick(File file) {
        loadingDialogue.startLoadingDialogue();

        //Move file from temporary directory to 'SDCard/Downloads/VU_Videos'
        Path fileMover = null;
        File downloadedFiles = new File(localPath + "/" + Environment.DIRECTORY_DOWNLOADS + "/VU_Videos");
        if(!downloadedFiles.exists()){
            downloadedFiles.mkdirs();
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                fileMover = Files.move(file.toPath(), new File(downloadedFiles.toPath() + "/" + file.getName()).toPath());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(fileMover != null){
            directoryService.deleteDirectory(tempDir);
            displayFiles();

            Toast.makeText(MainActivity.this, "File is now in " + downloadedFiles.getAbsolutePath() +" directory", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(MainActivity.this, "Failed to download file", Toast.LENGTH_LONG).show();
        }

        loadingDialogue.dismissLoadingDialogue();
    }

    @Override
    public void onDeleteClick(File file) {
        loadingDialogue.startLoadingDialogue();
        fileToDelete = file;

        //file uri != content uri
        ///storage/emulated/0/Pictures/image_name.jpg // file uri
        //content://media/external/image/media/114 // content uri

        long mediaID=getFilePathToMediaID(file,  getApplicationContext());
        Uri uri = ContentUris.withAppendedId( MediaStore.Video.Media.getContentUri("external"),mediaID);

        try {
            deleteAPI30(uri);
        }catch (Exception e){
            Toast.makeText(getApplicationContext(),"Permission needed", Toast.LENGTH_SHORT).show();
            try {
                deleteAPI30(uri);
                file.delete();
                displayFiles();
                Toast.makeText(getApplicationContext(), "Image Deleted successfully", Toast.LENGTH_SHORT).show();
            } catch (IntentSender.SendIntentException e1) {
                e1.printStackTrace();
            }
        }
    }

    public long getFilePathToMediaID(File file, Context context)
    {
        long id = 0;
        ContentResolver cr = context.getContentResolver();

        Uri uri = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            uri = MediaStore.Video.Media.getContentUri("external");
        }

        /*
        String selection = MediaStore.Video.Media._ID; //sql-where-clause-with-placeholder-variables;
        String[] selectionArgs = {new File(songPath).getName()}; //values-of-placeholder-variables
        String[] projection = {MediaStore.Video.Media._ID}; //media-database-columns-to-retrieve
        String sortOrder = MediaStore.Downloads.TITLE + " ASC"; //sql-order-by-clause;
        */

        Cursor cursor = cr.query(uri, null, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int idIndex = cursor.getColumnIndex(MediaStore.Video.Media._ID);
                int idIndex2 = cursor.getColumnIndex(MediaStore.Video.Media.DATA);
                String localStoragePath = cursor.getString(idIndex2);
                String filePath = fileToDelete.getPath().substring(localPath.toString().length());

                id = Long.parseLong(cursor.getString(idIndex));

                if(localStoragePath.endsWith(filePath)){
                    break;
                }
            }
        }

        return id;
    }

    private void deleteAPI30(Uri imageUri) throws IntentSender.SendIntentException {
        ContentResolver contentResolver = getContentResolver();
        // API 30

        List<Uri> uriList = new ArrayList<>();
        Collections.addAll(uriList, imageUri);
        PendingIntent pendingIntent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            pendingIntent = MediaStore.createDeleteRequest(contentResolver, uriList);
        }
        Activity result = ((Activity)this);

        result.startIntentSenderForResult(pendingIntent.getIntentSender(),
                1,null,0,
                0,0,null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == 1) {
                if(resultCode  == RESULT_OK){

                    if(fileToDelete.getAbsolutePath().startsWith(tempDir.getPath())){
                        StorageReference fileToDel = storageRef.child(databaseHandler.getDbDirectoryName()).child(fileToDelete.getName());
                        fileToDel.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                fileToDel.delete();
                                displayFiles();
                                loadingDialogue.dismissLoadingDialogue();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                displayFiles();
                                loadingDialogue.dismissLoadingDialogue();
                            }
                        });
                    } else {
                        displayFiles();
                        loadingDialogue.dismissLoadingDialogue();
                    }
                    Toast.makeText(this, "success", Toast.LENGTH_SHORT).show();
                } else if (resultCode == RESULT_CANCELED){
                    Toast.makeText(this, "Denied", Toast.LENGTH_SHORT).show();
                    loadingDialogue.dismissLoadingDialogue();
                }
            }
        } catch (Exception ex) {
            Toast.makeText(this, ex.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public void getDbVideos() {
        //Downloads videos to 'SDCard/Downloads/tempDir', a temporary directory
        databaseHandler.retrieveVideosFromDb(this, allFiles, customAdapter, storageRef);
    }
}