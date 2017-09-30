package com.testein.jenkins.api.models;

import java.util.List;

public class TestSuiteRunDetails {
        public Application application;
        public TestSuite testSuite;
        public TaskDetails run;
        public List<TaskDetails> tasks;
}
