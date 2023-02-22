package com.example.videouploader;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.view.LayoutInflater;

public class LoadingDialogue {
    private Activity activity;
    private AlertDialog dialogue;

    LoadingDialogue(Activity myActivity){
        activity = myActivity;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.custom_loading_dialogue, null));
        dialogue = builder.create();

    }

    void startLoadingDialogue(){
        dialogue.show();
    }

    void dismissLoadingDialogue(){

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dialogue.dismiss();
            }
        }, 1500);
    }
}
