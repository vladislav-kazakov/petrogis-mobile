package ru.nsu.mmedia.petrogismobile;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by vkazakov on 26.07.2017.
 */


public class ServerHelper {
    public interface Callback {
        void onDownloadUuidsFinished(List<String> list);
    }
    public interface CallbackUploadRecord {
        void onUploadFinished();
    }
    public interface CallbackDownloadRecord {
        void onDownloadFinished();
    }
    public String serverUrl = "http://petrogis.mmc.nsu.ru/sync/";
    public Context context;
    public LocalDBHelper localDBHelper;
    OkHttpClient client = new OkHttpClient();
    public void UploadRecord(String name, String uuid, double lat, double lng, String filename, File file, double orientation_x, double orientation_y, double orientation_z, final CallbackUploadRecord callback)
    {
        if (!file.exists() || !file.isFile()) return;
        try {
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", filename,
                            RequestBody.create(MediaType.parse("image/jpg"), file))
                    .addFormDataPart("name", name)
                    .addFormDataPart("uuid", uuid)
                    .addFormDataPart("lat", String.valueOf(lat))
                    .addFormDataPart("lng", String.valueOf(lng))
                    .addFormDataPart("orientation_x", String.valueOf(orientation_x))
                    .addFormDataPart("orientation_y", String.valueOf(orientation_y))
                    .addFormDataPart("orientation_z", String.valueOf(orientation_z))
                    .build();

            Request request = new Request.Builder()
                    .url(serverUrl + "upload/")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new okhttp3.Callback() {

                @Override
                public void onFailure(Call call, IOException e) {
                    // Handle the error
                    Log.d("123", e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        // Handle the error
                        Log.d("error upload", response.body().string());
                    }
                    callback.onUploadFinished();
                }
            });
        } catch (Exception ex) {
            // Handle the error
        }
    }
    public void downloadUuidList(final Callback callback)
    {
        try {
            Request request = new Request.Builder()
                    .url(serverUrl + "uuids/").build();

            client.newCall(request).enqueue(new okhttp3.Callback() {

                @Override
                public void onFailure(Call call, IOException e) {
                    //Log.d("123", e.getMessage());
                    // Handle the error
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                    }
                    String json = response.body().string();
                    Log.d("123", json);// Handle the error
                    try {
                        JSONArray jsonArray = new JSONArray(json);
                        List<String> list = new ArrayList<String>();
                        for (int i=0; i<jsonArray.length(); i++) {
                            list.add( jsonArray.getString(i) );
                        }
                        callback.onDownloadUuidsFinished(list);
                    }
                    catch(Exception e) {
                        Log.d("Error", "Not valid json:" + json);// Handle the error
                    }
                }
            });
        } catch (Exception ex) {
            // Handle the error
        }
    }

    public void downloadRecord(String uuid, final CallbackDownloadRecord callback)
    {
        try {
            Request request = new Request.Builder()
                    .url(serverUrl + "download/" + uuid).build();

            client.newCall(request).enqueue(new okhttp3.Callback() {

                @Override
                public void onFailure(Call call, IOException e) {
                    Log.d("error download", e.getMessage());
                    // Handle the error
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                    }
                    String json = response.body().string();
                    //Log.d("123", json);// Handle the error
                    try {
                        JSONArray jsonArray = new JSONArray(json);
                        List<String> list = new ArrayList<String>();
                        for (int i=0; i<jsonArray.length(); i++) {
                            list.add( jsonArray.getString(i) );
                        }
                        //String 0 - id,  1 - name, 2 - String uuid, 3 - double lat, 4 - double lng, 5 - String filename, 6 - Integer deleted
                        if (jsonArray.getInt(6) == 0) {
                            localDBHelper.createDBRecordWithUuid(jsonArray.getString(1),
                                    jsonArray.getDouble(3), jsonArray.getDouble(4),
                                    jsonArray.getString(5), jsonArray.getString(2),
                                    jsonArray.isNull(7) ? 0 : jsonArray.getDouble(7),
                                    jsonArray.isNull(8) ? 0 : jsonArray.getDouble(8),
                                    jsonArray.isNull(9) ? 0 : jsonArray.getDouble(9)
                            );
                            downloadImage(jsonArray.getInt(0), jsonArray.getString(5), callback);
                        }
                        else callback.onDownloadFinished();
                    }
                    catch(Exception e) {
                        Log.d("Error", "Not valid json:" + json);// Handle the error
                    }
                }
            });
        } catch (Exception ex) {
            // Handle the error
        }
    }
    public void downloadImage(Integer id, final String fileName, final CallbackDownloadRecord callback)
    {
        try {
            Request request = new Request.Builder()
                    .url(serverUrl + "downloadimage/" + id).build();

            client.newCall(request).enqueue(new okhttp3.Callback() {

                @Override
                public void onFailure(Call call, IOException e) {
                    Log.d("345", e.getMessage());
                    // Handle the error
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                    }
                    try {
                        File dir = new File(context.getFilesDir() + "/cache");
                        dir.mkdirs();
                        File file = new File(context.getFilesDir() + "/cache/" + fileName);
                        file.createNewFile();
                        FileOutputStream outputStream = new FileOutputStream(file);
                        outputStream.write(response.body().bytes());
                        outputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    callback.onDownloadFinished();
                }
            });
        } catch (Exception ex) {
            // Handle the error
        }
    }
}
