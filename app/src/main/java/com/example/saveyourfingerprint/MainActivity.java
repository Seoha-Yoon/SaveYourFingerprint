package com.example.saveyourfingerprint;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    // One Button
    Button BSelectImage;

    // One Preview Image
    ImageView IVPreviewImage;

    // constant to compare the activity result code
    int SELECT_PICTURE = 200;

    int REQUEST_CODE = 200;
    Uri selectedImage;
    String mediaPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // register the UI widgets with their appropriate IDs
        BSelectImage = findViewById(R.id.BSelectImage);
        IVPreviewImage = findViewById(R.id.IVPreviewImage);

        // handle the Choose Image button to trigger the image chooser function
        BSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // imageChooser();
                imageChooserWithMediaStore();
            }
        });



        // upload picture -> MediaStore

    }

    // load image using SAF
    // this function is triggered when the Select Image Button is clicked
    void imageChooser() {

        // create an instance of the intent of the type image
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);

        // pass the constant to compare it
        // with the returned requestCode
        startActivityForResult(Intent.createChooser(i, "Select Picture"), SELECT_PICTURE);
    }

    // load image using MediaStore
    void imageChooserWithMediaStore(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE);
    }

    // this function is triggered when user elects the image from the imageChooser
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // load image using SAF
//        if (resultCode == RESULT_OK) {
//
//            // compare the resultCode with the
//            // SELECT_PICTURE constant
//            if (requestCode == SELECT_PICTURE) {
//                // Get the url of the image from data
//                Uri selectedImageUri = data.getData();
//                if (null != selectedImageUri) {
//                    // update the preview image in the layout
//                    IVPreviewImage.setImageURI(selectedImageUri);
//                }
//            }
//        }

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
            IVPreviewImage.setImageBitmap(bitmap);

            // 사진 경로
            Cursor cursor = getContentResolver().query(Uri.parse(selectedImage.toString()), null, null, null, null);
            assert cursor != null;
            cursor.moveToFirst();
            mediaPath = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
            Log.d("경로 확인 >> ", "$selectedImg  /  $absolutePath");

        }else{
            Toast.makeText(this, "사진 업로드 실패", Toast.LENGTH_LONG).show();
        }
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
}