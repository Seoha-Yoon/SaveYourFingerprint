package com.example.saveyourfingerprint;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ResultActivity extends AppCompatActivity {

    ImageView resultImage;
    Button BtnDownload;

    byte[] byteArray;

    String getTime;
    Bitmap bitmap;

    // 이미지 파일 저장
    public void saveFile(@NonNull final byte[] bArray, @NonNull final String mimeType, @NonNull final String displayName) throws IOException {
        final ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/*");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // IS_PENDING을 1로 설정해놓으면, 현재 파일을 업데이트 전까지 외부에서 접근하지 못하도록 할 수 있다.
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        final ContentResolver resolver = getContentResolver();
        Uri uri = null;

        try {
            final Uri contentUri;

            // 다운로드 경
            contentUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
            uri = resolver.insert(contentUri, values);

            if(uri == null) {
                throw new IOException("Failed to create new MediaStore record.");
            }
            try (final OutputStream stream = resolver.openOutputStream(uri)) {
                if(stream == null) {
                    throw new IOException("Failed to open output stream.");
                }
                // 파일에 저장
                stream.write(bArray);
                stream.flush();
                stream.close();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear();
                    // 파일 저장이 완료되었으니, IS_PENDING을 다시 0으로 설정한다.
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    // 파일을 업데이트하면, 파일이 보인다.
                    resolver.update(uri, values, null, null);
                    Toast.makeText(this, "사진이 저장되었습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        }
        catch (IOException e) {
            Toast.makeText(ResultActivity.this, "사진을 저장할 수 없습니다.", Toast.LENGTH_SHORT).show();
            if (uri != null) {
                resolver.delete(uri, null, null);
            }
            throw e;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        resultImage = findViewById(R.id.ResultImage);
        BtnDownload = findViewById(R.id.BDownloadImage);

        // MainActivity에서 getExtra로 받아온 Image의 byteArray
        byteArray = getIntent().getByteArrayExtra("image");
        Log.d("byteArray",byteArray.toString());
        Log.d("bitmap",String.valueOf(bitmap));
        Log.d("byte length",String.valueOf(byteArray.length));

        bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);

        resultImage.setImageBitmap(bitmap);

        BtnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // 현재 날
                long now = System.currentTimeMillis();
                Date mDate = new Date(now);
                SimpleDateFormat simpleDate = new SimpleDateFormat("/yyyyMMdd_hhmmss");
                getTime = simpleDate.format(mDate);

                // byteArray를 'image_YYYYMMDD_time.jpg'로 저장
                try {
                    saveFile(byteArray,"image/*","image"+getTime+".jpg");
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

    }
}
