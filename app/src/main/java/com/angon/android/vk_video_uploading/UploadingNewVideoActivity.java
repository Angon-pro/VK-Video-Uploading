package com.angon.android.vk_video_uploading;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.vk.api.sdk.VK;
import com.vk.api.sdk.VKApiCallback;
import com.vk.api.sdk.exceptions.VKApiExecutionException;
import com.vk.api.sdk.requests.VKRequest;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadingNewVideoActivity extends AppCompatActivity {

    public static final String CLOSE = "Close";
    private static final String[] PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE};
    String uploadURL, accessToken;
    ProgressBar progressBar;
    Button uploadButton;
    int count, countAfterRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uploading_new_video);

        accessToken = getIntent().getStringExtra("Access token");
        EditText editTextVideoName = findViewById(R.id.edit_text_video_name);
        EditText editTextDescription = findViewById(R.id.edit_text_description);
        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.INVISIBLE);

        VKRequest getRequest = new VKRequest("video.get")
                .addParam("access_key", accessToken).addParam("v", "5.131");
        VK.execute(getRequest, new VKApiCallback() {
            @Override
            public void success(Object o) {
                String response = o.toString();
                count = getCount(response);
                countAfterRequest = count;
            }
            @Override
            public void fail(@NotNull VKApiExecutionException e) {

            }
        });

        uploadButton = findViewById(R.id.upload_button);
        uploadButton.setOnClickListener(v -> {
            String title, description;
            title = editTextVideoName.getText().toString();
            description = editTextDescription.getText().toString();
            if (isEmpty(title)) {
                Toast.makeText(getApplicationContext(), "The title is empty", Toast.LENGTH_SHORT).show();
            } else {
                VKRequest request = new VKRequest("video.save")
                        .addParam("name", title).addParam("description", description)
                        .addParam("access_key", accessToken).addParam("v", "5.131");
                VK.execute(request, new VKApiCallback() {
                    @Override
                    public void success(Object o) {
                        String response = o.toString();
                        uploadURL = getUploadURL(response);
                        ActivityCompat.requestPermissions(UploadingNewVideoActivity.this, PERMISSIONS, 1);
                        Intent pickVideoIntent = new Intent(Intent.ACTION_PICK);
                        pickVideoIntent.setType("video/*");
                        startActivityForResult(pickVideoIntent, 0);
                    }
                    @Override
                    public void fail(@NotNull VKApiExecutionException e) {
                        Toast.makeText(getApplicationContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent videoReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, videoReturnedIntent);
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                if (videoReturnedIntent != null) {
                    uploadButton.setEnabled(false);
                    progressBar.setVisibility(View.VISIBLE);
                    Uri pickedVideoUri = videoReturnedIntent.getData();
                    String videoPath = getPath(pickedVideoUri);
                    File videoFile = new File(videoPath);
                    String binaryVideoFile = null;
                    try {
                        FileInputStream videoFileInputStream = new FileInputStream(videoFile);
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        byte[] byteBufferString = new byte[1024];
                        for (int readNum; (readNum = videoFileInputStream.read(byteBufferString)) != -1; ) {
                            byteArrayOutputStream.write(byteBufferString, 0, readNum);
                        }
                        byte[] byteBinaryData = Base64.encode((byteArrayOutputStream.toByteArray()), Base64.DEFAULT);
                        binaryVideoFile = new String(byteBinaryData);
                    } catch (IOException exception) {
                        Toast.makeText(getApplicationContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
                        exception.printStackTrace();
                    }
                    assert binaryVideoFile != null;
                    RequestBody requestBody = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("video_file", "video_file",
                                    RequestBody.create(MediaType.parse("video/mp4"), binaryVideoFile))
                            .build();
                    Request request = new Request.Builder()
                            .url(uploadURL)
                            .post(requestBody)
                            .build();
                    OkHttpClient client = new OkHttpClient.Builder().build();
                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                            Toast.makeText(getApplicationContext(), "Something went wrong",
                                    Toast.LENGTH_SHORT).show();
                            Intent answerIntent = new Intent();
                            answerIntent.putExtra(CLOSE, "Yes");
                            setResult(RESULT_OK, answerIntent);
                            Intent mainIntent = new Intent(UploadingNewVideoActivity.this, MainActivity.class);
                            startActivity(mainIntent);
                            finish();
                        }
                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) {
                            VKRequest getRequest = new VKRequest("video.get")
                                    .addParam("access_key", accessToken).addParam("v", "5.131");
                            while (countAfterRequest == count) {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException exception) {
                                    exception.printStackTrace();
                                }
                                VK.execute(getRequest, new VKApiCallback() {
                                    @Override
                                    public void success(Object o) {
                                        String response = o.toString();
                                        countAfterRequest = getCount(response);
                                    }
                                    @Override
                                    public void fail(@NotNull VKApiExecutionException e) {

                                    }
                                });
                            }
                            Intent answerIntent = new Intent();
                            answerIntent.putExtra(CLOSE, "Yes");
                            setResult(RESULT_OK, answerIntent);
                            Intent mainIntent = new Intent(UploadingNewVideoActivity.this, MainActivity.class);
                            startActivity(mainIntent);
                            finish();
                        }
                    });
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

    private String getPath(Uri uri) {
        String[] projection = { MediaStore.Video.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        startManagingCursor(cursor);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    private boolean isEmpty(String input) {
        boolean isEmpty;
        input = input.replace(" ", "");
        isEmpty = input.length() == 0;
        return isEmpty;
    }

    private String getUploadURL(String input) {
        String url = "";
        int pos = input.indexOf("upload_url") + 13;
        while (input.charAt(pos) != '"') {
            url += input.charAt(pos);
            pos++;
        }
        return url;
    }
}