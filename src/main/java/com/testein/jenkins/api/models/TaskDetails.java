package com.testein.jenkins.api.models;

import java.util.List;
import java.util.UUID;

public class TaskDetails {
    public String id;
    public TaskStatus status;
    public boolean isCompleted;
    public String testName;
    public int percentage;
    public UUID testId;
    public boolean isFailed;
    public List<RunnerDetail> runnerDetails;
}
