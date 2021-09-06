package com.example.saveyourfingerprint;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface DjangoApi {
    String DJANGO_SITE="http://127.0.0.1:8000/";

    @Multipart
    @POST("api/fakeprint")
    Call<RequestBody> uploadFile(@Part MultipartBody.Part file);
}

