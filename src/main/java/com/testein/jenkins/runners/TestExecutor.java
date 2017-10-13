package com.testein.jenkins.runners;

import com.testein.jenkins.api.Client;
import com.testein.jenkins.api.HttpResponseReadException;
import com.testein.jenkins.api.enums.HttpMethod;
import com.testein.jenkins.api.enums.TargetType;
import com.testein.jenkins.api.models.RunnerDetail;
import com.testein.jenkins.api.models.RunnerType;
import com.testein.jenkins.api.models.TaskDetails;
import com.testein.jenkins.api.models.TaskStatus;
import hudson.FilePath;
import hudson.model.TaskListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TestExecutor extends BaseExecutor {
    public TestExecutor(String auth, TaskListener listener, FilePath path) {
        super(auth, listener, path);
    }

    @Override
    public UUID start(UUID id) throws IOException {
        listener.getLogger().println("Starting test with id " + id + "...");
        try {
        UUID taskId = client.sendRequest("tests/" + id + "/run", HttpMethod.Post, auth, null, UUID.class);
        listener.getLogger().println("Successfully started task for test. Task id: = " + taskId);
        listener.getLogger().println("Link to the task: " + Client.BaseUrl + "#/tasks/" + taskId + "/details");

        return taskId;

        } catch (HttpResponseReadException e){
            switch (e.getHttpResponse().getStatusLine().getStatusCode()){
                case 401:
                    listener.error("Sorry, please check your credentials");
                    break;

                case 403:
                    listener.error("Sorry, you aren't allowed to start this test");
                    break;

                case 404:
                    listener.error("Sorry, can't find such test");
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
        listener.getLogger().println("Start polling task with id " + runId);
        List<RunnerDetail> runners = new ArrayList<>();

        while(true){
            TaskDetails details = client.sendRequest("tasks/" + runId + "/details", HttpMethod.Get, auth, null, TaskDetails.class);

            listener.getLogger().println("----");
            listener.getLogger().println("Task status: " + details.status.toString());

            for (int i = 0; i < details.runnerDetails.size(); i++) {
                RunnerDetail runnerDetail = details.runnerDetails.get(i);
                String str = runnerDetail.runner.type == RunnerType.ById
                        ? (runnerDetail.agent != null
                            ? "Agent " + runnerDetail.agent.hostName + "<" + runnerDetail.agent.ipAddress + ">"
                            : "Agent with id " + runnerDetail.runner.value + " is missing")
                        : "Agent with label '" + (runnerDetail.runner.value != null
                            ? runnerDetail.runner.value
                            : "Any") + "'";

                str += ": ";

                String newLog = "";
                if (runnerDetail.log == null) {
                    str += "Waiting for an agent..";
                } else {
                    String oldLog = runners.size() > i && runners.get(i) != null ? runners.get(i).log : "";
                    oldLog = oldLog == null ? "" : oldLog;
                    String log = runnerDetail.log == null ? "" : runnerDetail.log;

                    newLog = log.substring(oldLog.length()).trim();
                    newLog = newLog.replaceAll("(?i)\\[section=(\\w+)\\]([\\s\\S]+?)\\[\\/section\\]", "$1: $2 /$1");

                    str += newLog;
                }

                if (!newLog.isEmpty()) {
                    listener.getLogger().println(str);
                }
            }

            runners = details.runnerDetails;

            if (details.status == TaskStatus.Success) {
                listener.getLogger().println("Task has completed successfully");
            } else if (details.status == TaskStatus.Canceled) {
                listener.getLogger().println("Task has been canceled");
            } else if (details.status == TaskStatus.Failed) {
                listener.error("Task has failed");
            }

            listener.getLogger().println("----");

            if (details.status.getValue() >= TaskStatus.Canceled.getValue()) {
                listener.getLogger().println("Link to the task: " + Client.BaseUrl + "#/tasks/" + runId + "/details");
                listener.getLogger().println("Link to the task details download: " + Client.BaseApiUrl + "tasks/" + runId + "/details/download");

                if (downloadReport) {
                    downloadRunReport(runId, auth, TargetType.Test);
                }
            }

            if (details.status.getValue() >= TaskStatus.Canceled.getValue()
                    && details.status.getValue() < TaskStatus.Success.getValue()) {
                throw new Exception("Task hasn't completed successfully");
            } else if (details.status == TaskStatus.Success) {
                return;
            }

            listener.getLogger().println("Task isn't completed yet. Waiting..");
            Thread.sleep(2000);
        }
    }
}
