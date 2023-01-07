package com.example.videouploader;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class CustomAdapter extends RecyclerView.Adapter<VideoViewHolder> {

    private Context context;
    private List<File> fileList;

    public CustomAdapter(Context context, List<File> fileList) {
        this.context = context;
        this.fileList = fileList;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VideoViewHolder(LayoutInflater.from(context).inflate(R.layout.custom_list, parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        holder.txtName.setText(fileList.get(position).getName());
        holder.txtName.setSelected(true); //FOR SCROLLING??
        Bitmap thumb = ThumbnailUtils.createVideoThumbnail(fileList.get(position).getAbsolutePath(),
                MediaStore.Images.Thumbnails.MINI_KIND);

        holder.imgThumbnail.setImageBitmap(thumb);
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }
}
