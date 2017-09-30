package com.testein.jenkins.api;

import org.apache.http.HttpResponse;

import java.io.IOException;

public class HttpResponseReadException extends IOException {
    private HttpResponse httpResponse;

    public HttpResponseReadException(HttpResponse httpResponse) {
        this.httpResponse = httpResponse;
    }

    public HttpResponse getHttpResponse() {
        return httpResponse;
    }
}
