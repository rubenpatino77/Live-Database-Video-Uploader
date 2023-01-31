package com.example.videouploader;


import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
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
    File localPath = new File(Objects.requireNonNull(System.getenv("EXTERNAL_STORAGE")));
    CustomAdapter customAdapter;
    Button getPhotosButton;
    File tempDir = new File(localPath.getPath() + "/" + Environment.DIRECTORY_DOWNLOADS + "/tempDir");
    boolean videosDownloaded = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        storageRef = FirebaseStorage.getInstance().getReference();
        getPhotosButton = findViewById(R.id.getPhotosButton);

        getPhotosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getPhotos();
                videosDownloaded = true;
                getPhotosButton.setClickable(false);
            }
        });

        if(tempDir.exists()){
            deleteDirectory(tempDir);
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

        Uri uri = Uri.fromFile(file);
        StorageReference uploadLocation = storageRef.child("VideoUploaderVideos").child(uri.getLastPathSegment());
        UploadTask uploadTask = uploadLocation.putFile(uri);

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                Toast.makeText(MainActivity.this, "Failed to access database", Toast.LENGTH_LONG).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                Toast.makeText(MainActivity.this, "Successfully entered database", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void getPhotos() {
        StorageReference downloadFrom = storageRef.child("VideoUploaderVideos");

        downloadFrom.listAll()
                .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {

                        Toast.makeText(MainActivity.this, "Successfully entered database. Now getting file.", Toast.LENGTH_LONG).show();

                        for (StorageReference item : listResult.getItems()) {
                            String name = item.getName();
                            File tempFile = new File(tempDir.getPath() + "/" + name);
                            StorageReference videoInStorage = downloadFrom.child(name);

                            videoInStorage.getFile(tempFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                    Toast.makeText(MainActivity.this, "Success in getting file", Toast.LENGTH_LONG).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(MainActivity.this, "Failed getting file", Toast.LENGTH_LONG).show();
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
                        // Uh-oh, an error occurred!
                        Toast.makeText(MainActivity.this, "Failed accessing database.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    public static void deleteDirectory(File file)
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
    }

}