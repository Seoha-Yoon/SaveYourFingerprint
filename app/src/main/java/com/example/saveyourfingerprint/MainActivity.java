// Good
package com.example.saveyourfingerprint;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.renderscript.ScriptGroup;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
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
    Bitmap bitmap;

    byte[] image_byte;

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

//        BUploadImage.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View v) { uploadImage(); }
//        });

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
            bitmap = null;

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


            try {
                InputStream is = getContentResolver().openInputStream(data.getData());

                Log.d("이미지 로드","업로드 시작");
                image_byte = getBytes(is);
                uploadImage(image_byte);

            } catch (IOException e) {
                e.printStackTrace();
            }


        }else{
            Toast.makeText(this, "사진 업로드 실패", Toast.LENGTH_LONG).show();
        }
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public byte[] getBytes(InputStream is) throws IOException {
        ByteArrayOutputStream byteBuff = new ByteArrayOutputStream();

        int buffSize = 1024;
        byte[] buff = new byte[buffSize];

        int len = 0;
        while ((len = is.read(buff)) != -1) {
            byteBuff.write(buff, 0, len);
        }

        Log.d("bytes_image", byteBuff.toByteArray().toString());

        return byteBuff.toByteArray();
    }



    // Django 서버로 이미지 전송
    private void uploadImage(byte[] imageBytes){

        Log.d("1","1");

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.MINUTES)
//                    .addNetworkInterceptor(interceptor)
                .build();
//                    .connectionSpecs(Arrays.asList(ConnectionSpec.CLEARTEXT,ConnectionSpec.MODERN_TLS))

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(DjangoApi.DJANGO_SITE)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();


        DjangoApi postApi= retrofit.create(DjangoApi.class);
        Log.d("2","retrofit 생성");

        RequestBody requestBody = RequestBody.create(imageBytes,MediaType.parse("image/jpeg"));
        Log.d("3", "requestBody 생성");

        MultipartBody.Part fileToUpload = MultipartBody.Part.createFormData("file","image.jpg",requestBody);

        Call <ResponseBody> call = postApi.uploadFile(fileToUpload);
        call.enqueue(new Callback <ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                ResponseBody body = response.body();

                String result = null;
                try {
                    result = body.string();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d("good", result);

                byte[] byteArray = result.getBytes();

                // byteArray에서 바로 bitMap으로 변환 불가
                // byteArray ->> jpeg ->> bitMap
                // YuvImage는 Raw데이터를 jpeg으로 바꿔줌
                YuvImage yuvimage = new YuvImage(byteArray, ImageFormat.NV21, 100, 100, null );
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                yuvimage.compressToJpeg(new Rect(0, 0, 100, 100), 80, baos);
                byte[] jdata = baos.toByteArray();

                // Convert to Bitmap
                Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length);

                //Bitmap bitmap_image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                //Bitmap bitmap_image = StringToBitMap(result);
                PreviewImage.setImageBitmap(bmp);

                Log.d("Bitmap", String.valueOf(bmp));


                StringBuilder str = new StringBuilder();
                for(byte b: byteArray) {
                    str = str.append(Byte.toString(b));
                    str = str.append(",");
                }

                Log.d("byte content",str.substring(0));


//                    // response.body()를 resultactivity로 넘김
//                    Intent intent = new Intent(MainActivity.this, ResultActivity.class);
//                    intent.putExtra("image", bitmap_image);
//                    startActivity(intent);

            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.d("fail", t.toString());
                //Log.d("fail", "fail");
            }
        });

        Log.d("end","end");

    }

    public static byte[] streamToByte(InputStream is) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        try {
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return buffer.toByteArray();
    }

    public static Bitmap getBitmapByEncodedString(String base64String) {
        String imageDataBytes = base64String.substring(base64String.indexOf(",")+1);
        Log.d("string_ver",imageDataBytes);
        InputStream stream = new ByteArrayInputStream(Base64.decode(imageDataBytes.getBytes(), Base64.DEFAULT));
        return BitmapFactory.decodeStream(stream);
    }


}