package com.angon.android.vk_video_uploading;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.vk.api.sdk.VK;
import com.vk.api.sdk.VKApiCallback;
import com.vk.api.sdk.auth.VKAccessToken;
import com.vk.api.sdk.auth.VKAuthCallback;
import com.vk.api.sdk.auth.VKScope;
import com.vk.api.sdk.exceptions.VKApiExecutionException;
import com.vk.api.sdk.requests.VKRequest;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private final ArrayList<VKScope> scope = new ArrayList<>();
    private static final String TITLE = "Title";
    private static final String DESCRIPTION = "Description";
    private static final String ACCESS_TOKEN = "Access token";
    private static final int UPLOAD_INTENT_CODE = 0;

    Intent uploadIntent = new Intent();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scope.add(VKScope.VIDEO);
        VK.login(MainActivity.this, scope);

        Button uploadNewVideoButton = findViewById(R.id.upload_new_video_button);
        uploadNewVideoButton.setOnClickListener(v -> {
            uploadIntent.setClass(MainActivity.this, UploadingNewVideoActivity.class);
            startActivityForResult(uploadIntent, UPLOAD_INTENT_CODE);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        VKAuthCallback callback = new VKAuthCallback() {
            @Override
            public void onLoginFailed(int i) {
                Toast.makeText(getApplicationContext(), "Login failed", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onLogin(@NotNull VKAccessToken vkAccessToken) {
                uploadIntent.putExtra(ACCESS_TOKEN, vkAccessToken.getAccessToken());
                Toast.makeText(getApplicationContext(), "Login successful", Toast.LENGTH_SHORT).show();
                VKRequest request = new VKRequest("video.get")
                        .addParam("access_key", vkAccessToken.getAccessToken())
                        .addParam("v", "5.131");
                VK.execute(request, new VKApiCallback() {
                    @Override
                    public void success(Object o) {
                        ListView videosListView = findViewById(R.id.videos_list_view);
                        ArrayList<HashMap<String, Object>> videos = new ArrayList<>();
                        HashMap<String, Object> hashMap;

                        String response = o.toString();
                        int count = getCount(response);
                        String title, description;
                        for (int i = 0; i < count; i++) {
                            title = getTitle(response);
                            description = getDescription(response);
                            hashMap = new HashMap<>();
                            hashMap.put(TITLE, title);
                            hashMap.put(DESCRIPTION, description);
                            videos.add(hashMap);
                            response = response.substring(response.indexOf("title") + 7);
                        }

                        SimpleAdapter adapter = new SimpleAdapter(MainActivity.this, videos, R.layout.list_item,
                                new String[]{TITLE, DESCRIPTION},
                                new int[]{R.id.title_text_view, R.id.description_text_view});
                        videosListView.setAdapter(adapter);
                    }
                    @Override
                    public void fail(@NotNull VKApiExecutionException e) {
                        Toast.makeText(getApplicationContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
        if (data == null || !VK.onActivityResult(requestCode, resultCode, data, callback)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
        if (requestCode == UPLOAD_INTENT_CODE) {
            if (resultCode == RESULT_OK) {
                assert data != null;
                if (data.getStringExtra(UploadingNewVideoActivity.CLOSE).equals("Yes")) {
                    finish();
                }
            }
        }
    }

    private int getCount(String input) {
        String buff = "";
        int pos = 21;
        while (input.charAt(pos) != ',') {
            buff += input.charAt(pos);
            pos++;
        }
        return Integer.parseInt(buff);
    }

    private String getTitle(String input) {
        String title = "";
        int pos = input.indexOf("title") + 8;
        while (input.charAt(pos) != ',') {
            title += input.charAt(pos);
            pos++;
        }
        title = title.substring(0, title.length() - 1);
        return title;
    }

    private String getDescription(String input) {
        String description = "";
        int pos = input.indexOf("description") + 14;
        while (input.charAt(pos) != ',') {
            description += input.charAt(pos);
            pos++;
        }
        description = description.substring(0, description.length() - 1);
        return description;
    }
}