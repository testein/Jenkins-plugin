package com.testein.jenkins.runners;

import com.testein.jenkins.api.Client;
import com.testein.jenkins.api.HttpResponseReadException;
import com.testein.jenkins.api.enums.HttpMethod;
import com.testein.jenkins.api.enums.TargetType;
import com.testein.jenkins.api.models.ApplicationRunDetails;
import com.testein.jenkins.api.models.TaskDetails;
import com.testein.jenkins.api.models.TaskStatus;
import com.testein.jenkins.api.models.TestSuiteRunDetails;
import hudson.FilePath;
import hudson.model.TaskListener;

import java.io.IOException;
import java.util.UUID;

public class ApplicationExecutor extends BaseExecutor {
    public ApplicationExecutor(String auth, TaskListener listener, FilePath path) {
        super(auth, listener, path);
    }

    @Override
    public UUID start(UUID id) throws IOException {
        listener.getLogger().println("Starting application with id " + id + "...");
        try {
            UUID runId = client.sendRequest("applications/" + id + "/run", HttpMethod.Post, auth, null, UUID.class);
            listener.getLogger().println("Successfully started coverage run for application. Run id: = " + runId);
            listener.getLogger().println("Link to the run: " + Client.BaseUrl + "#/applications/runs/" + runId);

            return runId;
        } catch (HttpResponseReadException e) {
            switch (e.getHttpResponse().getStatusLine().getStatusCode()) {
                case 401:
                    listener.error("Sorry, please check your credentials");
                    break;

                case 403:
                    listener.error("Sorry, you aren't allowed to start this application");
                    break;

                case 404:
                    listener.error("Sorry, can't find such application");
                    break;

                case 402:
                    listener.error("Sorry, you don't have enough credit on your account");
                    break;

                default:
                    listener.error("Error code: " + e.getHttpResponse().getStatusLine().getStatusCode());
                    break;
            }

            throw e;
        }
    }

    @Override
    public void poll(UUID runId, boolean downloadReport) throws Exception {
        listener.getLogger().println("Start polling application coverage run with id " + runId);

        while (true) {
            ApplicationRunDetails details = client.sendRequest("applications/runs/" + runId + "/details", HttpMethod.Get, auth, null, ApplicationRunDetails.class);

            listener.getLogger().println("----");
            listener.getLogger().println("Task status: " + details.run.status.toString());

            for (int i = 0; i < details.tasks.size(); i++) {
                TaskDetails task = details.tasks.get(i);
                listener.getLogger().println("Task '" + task.testName + "' status is '" + task.status + "', percentage: " + task.percentage + "%. Link: " + Client.BaseUrl + "#tasks/" + task.id + "/details");
            }

            if (details.run.status == TaskStatus.Success) {
                listener.getLogger().println("Application coverage run has completed successfully");
            } else if (details.run.status == TaskStatus.Canceled) {
                listener.getLogger().println("Application coverage run has been canceled");
            } else if (details.run.status == TaskStatus.Failed) {
                listener.error("Application coverage run has failed");
            }

            listener.getLogger().println("----");

            if (details.run.status.getValue() >= TaskStatus.Canceled.getValue() && downloadReport) {
                downloadRunReport(runId, auth, TargetType.Application);
            }

            if (details.run.status.getValue() >= TaskStatus.Canceled.getValue()
                    && details.run.status.getValue() < TaskStatus.Success.getValue()) {
                throw new Exception("Application coverage run hasn't completed successfully");
            } else if (details.run.status == TaskStatus.Success) {
                return;
            }

            listener.getLogger().println("Application coverage run isn't completed yet. Waiting..");
            Thread.sleep(2000);
        }
    }
}
