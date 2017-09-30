package com.testein.jenkins.runners;

import com.testein.jenkins.api.Client;
import com.testein.jenkins.api.HttpResponseReadException;
import com.testein.jenkins.api.enums.UploadTargetType;
import hudson.FilePath;
import hudson.model.TaskListener;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Uploader {
    private TaskListener listener;
    private String auth;
    private FilePath path;

    protected Client client = new Client();

    public Uploader(TaskListener listener, String auth, FilePath path) {
        this.listener = listener;
        this.auth = auth;
        this.path = path;
    }

    public void upload(UploadTargetType targetType, String jsFile, String jsonFile, String jarFile, boolean overwrite) throws IOException {
        listener.getLogger().println("Start uploading " + targetType + " steps..");

        Map<String, String> props = new HashMap<>();
        props.put("overwrite", String.valueOf(overwrite));
        props.put("type", String.valueOf(targetType.getValue()));

        Map<String, File> files = new HashMap<>();
        switch (targetType){
            case Js:
                files.put("runFile", new File(path.getRemote() + "/" + jsFile));
                files.put("descriptorFile", new File(path.getRemote() + "/" + jsonFile));
                break;

            case Jar:
                files.put("jarFile", new File(path.getRemote() + "/" + jarFile));
        }

        try {
            client.upload("steps", props, null, files, auth);
        } catch (HttpResponseReadException e){
            switch (e.getHttpResponse().getStatusLine().getStatusCode()){
                case 403:
                    listener.error("Upload failed. Please check your permissions");
                    break;

                default:
                    listener.error("Upload faile. Status: " + e.getHttpResponse().getStatusLine().getStatusCode());
                    break;
            }

            throw e;
        }

        listener.getLogger().println("Steps were successfully uploaded");
    }
}
