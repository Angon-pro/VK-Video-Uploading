package com.angon.android.vk_video_uploading;

import android.widget.Toast;

import com.vk.api.sdk.VK;
import com.vk.api.sdk.VKTokenExpiredHandler;

public class Application extends android.app.Application {

    private final VKTokenExpiredHandler tokenTracker = () ->
            Toast.makeText(getApplicationContext(), "Invalid token", Toast.LENGTH_SHORT).show();

    @Override
    public void onCreate() {
        super.onCreate();
        VK.addTokenExpiredHandler(tokenTracker);
    }
}