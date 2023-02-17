package com.example.videouploader;


import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        storageRef = FirebaseStorage.getInstance().getReference();
        getPhotosButton = findViewById(R.id.getPhotosButton);
        dbFileNames = databaseHandler.getDbFileNames(this, storageRef);

        getPhotosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDbVideos();
                getPhotosButton.setClickable(false);
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
         recyclerView = findViewById(R.id.recycler_view);
         recyclerView.setHasFixedSize(true);
         recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
         allFiles = new ArrayList<>();
         allFiles.addAll(findVideos(localPath));
         customAdapter = new CustomAdapter(this, allFiles, this);
         customAdapter.setHasStableIds(true);
         recyclerView.setAdapter(customAdapter);
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
        if(!dbFileNames.containsKey(file.getName())){
            databaseHandler.uploadFileToDb(getBaseContext(), file, storageRef);
            dbFileNames = databaseHandler.getDbFileNames(this, storageRef);
        } else {
            Toast.makeText(getBaseContext(), "File Already exists in the database.", Toast.LENGTH_LONG).show();
        }
    }



    @Override
    public void onDownloadClick(File file) {
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
            Toast.makeText(MainActivity.this, "File is now in " + downloadedFiles.getAbsolutePath() +" directory", Toast.LENGTH_LONG).show();
            //TODO: Refresh adapter with new values organized..... OR.....
        } else {
            Toast.makeText(MainActivity.this, "Failed to download file", Toast.LENGTH_LONG).show();
        }
    }


    public void getDbVideos() {
        //Downloads videos to 'SDCard/Downloads/tempDir', a temporary directory
        databaseHandler.retrieveVideosFromDb(this, allFiles, customAdapter, storageRef);
    }
}