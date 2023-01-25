package com.example.videouploader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.CancellationSignal;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class CustomAdapter extends RecyclerView.Adapter<VideoViewHolder> {

    private final Context context;
    private List<File> fileList;
    private SelectListener listener;

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

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.txtName.setText(fileList.get(position).getName());
        holder.txtName.setSelected(true);

        MediaMetadataRetriever dataRetriever = new MediaMetadataRetriever();
        dataRetriever.setDataSource(fileList.get(position).getPath());
        String width =
                dataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        String height =
                dataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        Size mSize = new Size(Integer.parseInt(width), Integer.parseInt(height));
        CancellationSignal ca = new CancellationSignal();
        Bitmap thumb = null;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                thumb = ThumbnailUtils.createVideoThumbnail(new File(fileList.get(position).getAbsolutePath()), mSize, ca);


            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        holder.imgThumbnail.setImageBitmap(thumb);

        holder.cardView.setOnClickListener(view -> {
            listener.onFileClick(fileList.get(position), holder.txtName.getText().toString());
        });

        holder.uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onButtonClick(fileList.get(position), holder.txtName.getText().toString());
            }
        });
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }
}
