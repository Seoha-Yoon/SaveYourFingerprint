package com.example.saveyourfingerprint;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    // Button
    Button BSelectImage, BUploadImage;

    // Preview Image
    ImageView PreviewImage;

    int REQUEST_CODE = 200;
    Uri selectedImage;
    String mediaPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // register the UI widgets with their appropriate IDs
        BSelectImage = findViewById(R.id.BSelectImage);
        PreviewImage = findViewById(R.id.PreviewImage);
        BUploadImage = findViewById(R.id.BUploadImage);

        // handle the Choose Image button to trigger the image chooser function
        BSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageChooserWithMediaStore();
            }
        });

        BUploadImage.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) { uploadImage(); }
        });

    }

    // load image using MediaStore
    void imageChooserWithMediaStore(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE);
    }

    // this function is triggered when user elects the image from the imageChooser
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // load image using MediaStore
        if(requestCode== REQUEST_CODE && resultCode==RESULT_OK && data!=null) {

            selectedImage = data.getData();
            Uri photoUri = data.getData();
            Bitmap bitmap = null;

            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),photoUri);
                // 사진 옆으로 누워서 출력 되는 것 해결
                bitmap = rotateImage(bitmap, 90);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // load image
            PreviewImage.setImageBitmap(bitmap);

            // 사진 경로
            Cursor cursor = getContentResolver().query(Uri.parse(selectedImage.toString()), null, null, null, null);
            assert cursor != null;
            cursor.moveToFirst();
            mediaPath = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
            //Log.d("경로 확인 >> ", "$selectedImg  /  $absolutePath");
            Log.d("경로 확인 >>", mediaPath);
            // /storage/emulated/0/DCIM/Camera/20210905_184903.jpg

        }else{
            Toast.makeText(this, "사진 업로드 실패", Toast.LENGTH_LONG).show();
        }
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    // Django 서버로 이미지 전송
    private void uploadImage(){

        if(mediaPath == null){
            Toast.makeText(this,"사진을 골라 주십시오.", Toast.LENGTH_SHORT);
        }else{
            File imageFile = new File(mediaPath);
            Log.d("이미지 경로",mediaPath);

            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(DjangoApi.DJANGO_SITE)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();


            DjangoApi postApi= retrofit.create(DjangoApi.class);

            RequestBody requestBody = RequestBody.create(imageFile,MediaType.parse("image/jpg"));
            MultipartBody.Part fileToUpload = MultipartBody.Part.createFormData("file", imageFile.getName(),requestBody);

            Log.d("이미지 확인",imageFile.getName());

//            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//            String token = sp.getString("Token", "");

            Call<RequestBody> call = postApi.uploadFile(fileToUpload);
            Log.d("call","call");

            call.enqueue(new Callback<RequestBody>() {
                @Override
                public void onResponse(Call<RequestBody> call, Response<RequestBody> response) {
                    if(!response.isSuccessful()){
                        Log.e("연결이 비정상적 : ", "error code : " + response.code());
                        return;
                    }
                    Log.d("good", "good");

                }
                @Override
                public void onFailure(Call<RequestBody> call, Throwable t) {
                    Log.d("fail", "fail");
                }
            });

            Log.d("end","end");
        }

    }

    private void uploadImage2(){

        try{
            URL url = new URL("http://127.0.0.1:8000/api/fakeprint");
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type","multipart/form-data");
            con.setDoOutput(true);

            // [2-2]. parameter 전달 및 데이터 읽어오기.
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(con.getOutputStream()));
            //pw.write(sbParams.toString());
            pw.flush(); // 출력 스트림을 flush. 버퍼링 된 모든 출력 바이트를 강제 실행.
            pw.close(); // 출력 스트림을 닫고 모든 시스템 자원을 해제.

            // [2-3]. 연결 요청 확인.
            // 실패 시 null을 리턴하고 메서드를 종료.
            if (con.getResponseCode() != HttpURLConnection.HTTP_OK)
                Log.d("연결요청","실패");

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}