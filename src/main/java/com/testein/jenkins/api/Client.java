package com.testein.jenkins.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.testein.jenkins.api.enums.HttpMethod;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.activation.MimetypesFileTypeMap;
import java.io.*;
import java.util.Map;

public class Client {
    public static final String BaseUrl = "http://app.testein.com/";
    public static final String BaseApiUrl = BaseUrl + "api/";

    public  <T> T sendRequest(String url, HttpMethod method, String auth, Object data, Class<T> tClass) throws IOException {
        HttpResponse response = sendRequest(url, method, auth, data);

        try {
            if (response.getStatusLine().getStatusCode() >= 400){
                throw new Exception();
            }

            if (response.getEntity() != null && tClass != null) {
                Gson gson = new GsonBuilder().registerTypeAdapterFactory(new EnumAdapterFactory()).create();
                InputStream content = response.getEntity().getContent();

                return gson.fromJson(new InputStreamReader(content), tClass);
            }

            return null;
        } catch (Exception e){
            throw new HttpResponseReadException(response);
        }
    }

    public HttpResponse sendRequest(String url, HttpMethod method, String auth, Object data) throws IOException {
        HttpClient httpClient = HttpClientBuilder.create().build();
        Gson gson = new Gson();

        url = BaseApiUrl + url;

        HttpUriRequest request = null;
        switch (method) {
            case Get:
                request = new HttpGet(url);
                break;

            case Post:
                request = new HttpPost(url);
                if (data != null) {
                    StringEntity params = new StringEntity(gson.toJson(data));
                    ((HttpPost) request).setEntity(params);
                }

                break;

            case Put:
                request = new HttpPut(url);
                if (data != null) {
                    StringEntity params = new StringEntity(gson.toJson(data));
                    ((HttpPut) request).setEntity(params);
                }

                break;

            case Delete:
                request = new HttpDelete(url);
                break;
        }

        request.addHeader("content-type", "application/json");
        request.addHeader("Accept", "application/json");
        if (auth != null){
            request.addHeader("Authorization", auth);
        }

        return httpClient.execute(request);
    }

    public String upload(String url, Map<String, String> props, Map<String, Map.Entry<String, byte[]>> filesBytes, Map<String, File> files, String auth) throws IOException {
        HttpClient httpClient = HttpClientBuilder.create().build();

        url = BaseApiUrl + url;

        HttpPost request = new HttpPost(url);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        for (Map.Entry<String, String> prop : props.entrySet()){
            builder.addTextBody(prop.getKey(), prop.getValue(), ContentType.TEXT_PLAIN);
        }

        if (filesBytes != null){
            for (Map.Entry<String, Map.Entry<String, byte[]>> file : filesBytes.entrySet()){
                builder.addBinaryBody(
                        file.getKey(),
                        file.getValue().getValue(),
                        ContentType.parse(MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(file.getValue().getKey())),
                        file.getValue().getKey()
                );
            }
        }
        if (files != null){
            for (Map.Entry<String, File> file : files.entrySet()){
                builder.addBinaryBody(
                        file.getKey(),
                        file.getValue(),
                        ContentType.parse(MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(file.getValue().getName())),
                        file.getValue().getName()
                );
            }
        }

        HttpEntity multipart = builder.build();
        request.setEntity(multipart);

        request.addHeader("Authorization", auth);

        HttpResponse response = httpClient.execute(request);

        if (response.getStatusLine().getStatusCode() >= 400){
            throw new HttpResponseReadException(response);
        }

        if (response.getEntity() != null) {
            Gson gson = new Gson();
            InputStream content = response.getEntity().getContent();

            return gson.fromJson(new InputStreamReader(content), String.class);
        }

        throw new IOException("Failed to upload file");
    }

    public String download(String url, String filePath, String auth) throws Exception{
        return download(url, filePath, auth, null);
    }

    public String download(String url, String filePath, String auth, String directory) throws Exception {
        HttpClient httpClient = HttpClientBuilder.create().build();

        url = BaseApiUrl + url;

        HttpGet request = new HttpGet(url);
        if (auth != null) {
            request.addHeader("Authorization", auth);
        }

        HttpResponse response = httpClient.execute(request);
        if (response.getStatusLine().getStatusCode() >= 400){
            throw new HttpResponseReadException(response);
        }

        if (filePath == null) {
            String dispositionValue = response.getFirstHeader("Content-Disposition").getValue();
            int index = dispositionValue.indexOf("filename=");
            if (index > 0) {
                filePath = dispositionValue.substring(index + 9, dispositionValue.indexOf(';', index));

                if (directory != null){
                    File dir = new File(directory);
                    if (!dir.exists()) dir.mkdir();

                    filePath = directory + "/" + filePath;
                }
            } else {
                throw new Exception("Can't find file name in header");
            }
        }

        BufferedInputStream bis = new BufferedInputStream(response.getEntity().getContent());
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(filePath)));

        byte[] buffer = new byte[1280000];
        int inByte;
        while((inByte = bis.read(buffer)) > 0)
            bos.write(buffer,0,inByte);
        bis.close();
        bos.close();

        return filePath;
    }
}
