package com.example.videouploader;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements SelectListener {
    private StorageReference mStorageRef;

    RecyclerView recyclerView;
    List<File> allFiles;
    File path = new File(Objects.requireNonNull(System.getenv("EXTERNAL_STORAGE")));
    CustomAdapter customAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStorageRef = FirebaseStorage.getInstance().getReference();
        
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
         allFiles.addAll(findVideos(path));
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
    public void onFileClick(File file) {
        startActivity(new Intent(MainActivity.this, VideoPlayerActivity.class)
        .putExtra("VIDEO", file.getAbsolutePath()));
    }

}