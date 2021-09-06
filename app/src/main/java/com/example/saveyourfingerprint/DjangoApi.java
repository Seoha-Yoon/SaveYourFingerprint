package com.example.saveyourfingerprint;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public class DjangoApi {
    public static String DJANGO_SITE="http://127.0.0.1:8000/api/fakeprint/";

    @Multipart
    @POST("upload/")
    Call<RequestBody> uploadFile(@Part MultipartBody.Part file) {
        return null;
    }
}

