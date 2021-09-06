package com.example.saveyourfingerprint;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface DjangoApi {
//    String DJANGO_SITE="http://127.0.0.1:8000";
    String DJANGO_SITE="http://10.0.0.2:8000";
//    String DJANGO_SITE="http://10.210.60.88:8000";

    @Multipart
    @POST("api/fakeprint")
    Call <ResponseBody> uploadFile(@Part MultipartBody.Part file);
}
