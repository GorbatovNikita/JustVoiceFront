package ru.myitschool.justvoice.data.remote;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import ru.myitschool.justvoice.data.remote.dto.Speaker;
import ru.myitschool.justvoice.data.remote.dto.Token;
import ru.myitschool.justvoice.data.remote.dto.TranscriptionResponse;
import ru.myitschool.justvoice.data.remote.dto.TranscriptionStatusResponse;
import ru.myitschool.justvoice.data.remote.dto.UserCreate;
import ru.myitschool.justvoice.data.remote.dto.UserProfile;
import ru.myitschool.justvoice.data.remote.dto.UserUpdate;

public interface ApiService {

    @POST("api/v1/auth/register")
    Call<Token> register(@Body UserCreate userCreate);

    @FormUrlEncoded
    @POST("api/v1/auth/login")
    Call<Token> login(
            @Field("username") String username,
            @Field("password") String password
    );

    @POST("api/v1/auth/logout")
    Call<Map<String, String>> logout();

    @GET("api/v1/users/me")
    Call<UserProfile> getCurrentUser();

    @PUT("api/v1/users/me")
    Call<UserProfile> updateCurrentUser(@Body UserUpdate userUpdate);

    @Multipart
    @POST("api/v1/audio/transcribe")
    Call<TranscriptionStatusResponse> transcribeAudio(
            @Part MultipartBody.Part file
    );

    @GET("api/v1/audio/transcriptions")
    Call<List<TranscriptionStatusResponse>> listTranscriptions();

    @GET("api/v1/audio/transcription/{task_id}")
    Call<TranscriptionResponse> getTranscription(
            @Path("task_id") int taskId
    );

    @Streaming
    @GET("api/v1/audio/file/{task_id}")
    Call<ResponseBody> downloadAudioFile(
            @Path("task_id") int taskId
    );

    @PUT("api/v1/audio/transcription/{task_id}/name")
    Call<Map<String, String>> renameTranscription(
            @Path("task_id") int taskId,
            @Body Map<String, String> nameData
    );

    @DELETE("api/v1/audio/transcription/{task_id}")
    Call<Map<String, String>> deleteTranscription(
            @Path("task_id") int taskId
    );

    @POST("api/v1/audio/transcription/{task_id}/retry")
    Call<TranscriptionStatusResponse> retryTranscription(@Path("task_id") int taskId);

    @POST("api/v1/auth/refresh")
    Call<Token> refreshToken(@Body Map<String, String> body);

    @GET("api/v1/speakers/")
    Call<List<Speaker>> getSpeakers();

    @PUT("api/v1/speakers/{speaker_id}")
    Call<Map<String, String>> renameSpeaker(
            @Path("speaker_id") int speakerId,
            @Query("name") String name
    );

    @DELETE("api/v1/speakers/{speaker_id}")
    Call<Map<String, String>> deleteSpeaker(
            @Path("speaker_id") int speakerId
    );
}