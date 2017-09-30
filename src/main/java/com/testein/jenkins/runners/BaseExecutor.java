package com.testein.jenkins.runners;

import com.testein.jenkins.api.Client;
import com.testein.jenkins.api.HttpResponseReadException;
import com.testein.jenkins.api.enums.TargetType;
import hudson.FilePath;
import hudson.model.TaskListener;

import java.io.File;
import java.util.UUID;

public abstract class BaseExecutor implements IExecutor {
    protected TaskListener listener;
    protected String auth;
    protected FilePath path;

    protected Client client = new Client();

    protected BaseExecutor(String auth, TaskListener listener, FilePath path) {
        this.listener = listener;
        this.auth = auth;
        this.path = path;
    }

    protected void downloadRunReport(UUID runId, String auth, TargetType type) throws Exception {
        String fileName = "Testein-Report-" + runId + ".html";

        listener.getLogger().println("Downloading report..");
        String url = "";
        switch (type) {
            case Application:
                url = "applications/runs/" + runId + "/report";
                break;

            case Suite:
                url = "testsuites/runs/" + runId + "/report";
                break;

            case Test:
                url = "tasks/" + runId + "/report";
                break;

            default:
                throw new Error("Unknown type: " + type);
        }

        try {
            client.download(url, path.getRemote() + "/" + fileName, auth);
        } catch (HttpResponseReadException e){
            switch (e.getHttpResponse().getStatusLine().getStatusCode()){
                case 403:
                    listener.error("Sorry, you aren't allowed to see this report");
                    break;

                case 404:
                    listener.error("Sorry, can't find such target with id " + runId + "; url = " + url);
                    break;

                default:
                    listener.error("Sorry, error: " + e.getHttpResponse().getStatusLine().getStatusCode());
                    break;
            }

            throw e;
        }

        listener.getLogger().println("Successfully downloaded report into file '" + fileName + "'");
    }
}
