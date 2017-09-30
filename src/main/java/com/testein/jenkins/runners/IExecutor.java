package com.testein.jenkins.runners;

import java.io.IOException;
import java.util.UUID;

public interface IExecutor {
    UUID start(UUID id) throws IOException;

    void poll(UUID runId, boolean downloadReport) throws Exception;
}
