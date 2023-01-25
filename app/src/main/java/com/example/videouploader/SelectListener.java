package com.example.videouploader;

import java.io.File;

public interface SelectListener {
     void onFileClick(File file, String videoName);

     void onButtonClick(File file, String videoName);
}
