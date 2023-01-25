package com.example.videouploader;


import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
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
    private StorageReference storageRef;

    RecyclerView recyclerView;
    List<File> allFiles;
    File path = new File(Objects.requireNonNull(System.getenv("EXTERNAL_STORAGE")));
    CustomAdapter customAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        storageRef = FirebaseStorage.getInstance().getReference();
        
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
    public void onFileClick(File file, String videoName) {
        Intent intent = new Intent(MainActivity.this, VideoPlayerActivity.class);

        Bundle extras = new Bundle();
        extras.putString("VIDEO", file.getAbsolutePath());
        extras.putString("videoName", videoName);
        intent.putExtras(extras);

        startActivity(intent);


    }

    @Override
    public void onButtonClick(File file, String videoName) {

        Uri uri = Uri.fromFile(file);
        storageRef = FirebaseStorage.getInstance().getReference().child("videosEx").child(uri.getLastPathSegment());
        UploadTask uploadTask = storageRef.putFile(uri);

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                Toast.makeText(MainActivity.this, "Fail", Toast.LENGTH_LONG).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_LONG).show();
            }
        });
    }
}