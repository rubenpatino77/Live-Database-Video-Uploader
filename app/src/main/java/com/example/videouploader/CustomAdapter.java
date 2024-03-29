package com.example.videouploader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Environment;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class CustomAdapter extends RecyclerView.Adapter<VideoViewHolder> {

    private final Context context;
    private List<File> fileList;
    private SelectListener listener;
    private String tempDir = Objects.requireNonNull(System.getenv("EXTERNAL_STORAGE")) + "/"
            + Environment.DIRECTORY_DOWNLOADS + "/tempDir";

    public CustomAdapter(Context context, List<File> fileList, SelectListener listener) {
        this.context = context;
        this.fileList = fileList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VideoViewHolder(LayoutInflater.from(context).inflate(R.layout.custom_list, parent,false));
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, @SuppressLint("RecyclerView") int position) {

        Size mSize;
        CancellationSignal ca = new CancellationSignal();
        Bitmap thumb = null;

        holder.txtName.setText(fileList.get(position).getName());
        holder.txtName.setSelected(true);

        File file = fileList.get(position);
        String filePath = fileList.get(position).getPath();

        MediaMetadataRetriever dataRetriever = new MediaMetadataRetriever();

        boolean fileAvailable = false;

        while (!file.exists() || file.getTotalSpace() == 0){
            try {
                Thread.sleep(300);
            } catch (InterruptedException x) {
                x.printStackTrace();
            }
        }

        while (!fileAvailable){
            try {
                dataRetriever.setDataSource(filePath);
                fileAvailable = true;
            } catch (Exception e){
                e.printStackTrace();
                try {
                    Thread.sleep(300);
                } catch (InterruptedException x) {
                    x.printStackTrace();
                }
            }
        }

        String width =
                dataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        String height =
                dataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        mSize = new Size(Integer.parseInt(width), Integer.parseInt(height));

        if(filePath.startsWith(tempDir)) {
            holder.uploadButton.setBackgroundColor(Color.parseColor("#004999"));
            holder.uploadButton.setText("Download to local files");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                holder.cardView.setOutlineSpotShadowColor(Color.parseColor("#004999"));
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                thumb = ThumbnailUtils.createVideoThumbnail(new File(fileList.get(position).getAbsolutePath()), mSize, ca);
            }
            try {
                Thread.sleep(300);
                thumb = ThumbnailUtils.createVideoThumbnail(new File(fileList.get(position).getAbsolutePath()), mSize, ca);
            } catch (Exception interruptedException) {
                interruptedException.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        holder.imgThumbnail.setImageBitmap(thumb);

        holder.cardView.setOnClickListener(view -> {
            listener.onFileClick(fileList.get(position), holder.txtName.getText().toString());
        });

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onDeleteClick(fileList.get(position));
            }
        });

        if(!filePath.startsWith(tempDir)) {
            holder.uploadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onUploadClick(fileList.get(position));
                }
            });
        } else {
            holder.uploadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onDownloadClick(fileList.get(position));
                }
            });
        }
    }
    @Override
    public int getItemCount() {
        return fileList.size();
    }
}
