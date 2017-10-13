package com.testein.jenkins.runners;

import com.testein.jenkins.api.Client;
import com.testein.jenkins.api.HttpResponseReadException;
import com.testein.jenkins.api.enums.HttpMethod;
import com.testein.jenkins.api.enums.TargetType;
import com.testein.jenkins.api.models.*;
import hudson.FilePath;
import hudson.model.TaskListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SuiteExecutor extends BaseExecutor {
    public SuiteExecutor(String auth, TaskListener listener, FilePath path) {
        super(auth, listener, path);
    }

    @Override
    public UUID start(UUID id) throws IOException {
        listener.getLogger().println("Starting test suite with id " + id + "...");
        try {
            UUID runId = client.sendRequest("testsuites/" + id + "/run", HttpMethod.Post, auth, null, UUID.class);
            listener.getLogger().println("Successfully started coverage run for test suite. Run id: = " + runId);
            listener.getLogger().println("Link to the run: " + Client.BaseUrl + "#/testsuites/runs/" + runId);

            return runId;
        } catch (HttpResponseReadException e) {
            switch (e.getHttpResponse().getStatusLine().getStatusCode()) {
                case 401:
                    listener.error("Sorry, please check your credentials");
                    break;

                case 403:
                    listener.error("Sorry, you aren't allowed to start this test suite");
                    break;

                case 404:
                    listener.error("Sorry, can't find such test suite");
                    break;

                case 402:
                    listener.error("Sorry, you don't have enough credit in your account");
                    break;

                case 406:
                    listener.error("Sorry, you have exceeded monthly runs in your account");
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
        listener.getLogger().println("Start polling test suite coverage run with id " + runId);

        while (true) {
            TestSuiteRunDetails details = client.sendRequest("testsuites/runs/" + runId + "/details", HttpMethod.Get, auth, null, TestSuiteRunDetails.class);

            listener.getLogger().println("----");
            listener.getLogger().println("Task status: " + details.run.status.toString());

            for (int i = 0; i < details.tasks.size(); i++) {
                TaskDetails task = details.tasks.get(i);
                listener.getLogger().println("Task '" + task.testName + "' status is '" + task.status + "', percentage: " + task.percentage + "%. Link: " + Client.BaseUrl + "#tasks/" + task.id + "/details");
            }

            if (details.run.status == TaskStatus.Success) {
                listener.getLogger().println("Test suite coverage run has completed successfully");
            } else if (details.run.status == TaskStatus.Canceled) {
                listener.getLogger().println("Test suite coverage run has been canceled");
            } else if (details.run.status == TaskStatus.Failed) {
                listener.error("Test suite coverage run has failed");
            }

            listener.getLogger().println("----");

            if (details.run.status.getValue() >= TaskStatus.Canceled.getValue() && downloadReport) {
                downloadRunReport(runId, auth, TargetType.Suite);
            }

            if (details.run.status.getValue() >= TaskStatus.Canceled.getValue()
                    && details.run.status.getValue() < TaskStatus.Success.getValue()) {
                throw new Exception("Test suite coverage run hasn't completed successfully");
            } else if (details.run.status == TaskStatus.Success) {
                return;
            }

            listener.getLogger().println("Test suite coverage isn't completed yet. Waiting..");
            Thread.sleep(2000);
        }
    }
}
