package com.coreyd97.stepper.step;

import burp.IHttpRequestResponse;

public class StepExecutionInfo {
    private long responseTime;
    private IHttpRequestResponse requestResponse;

    public StepExecutionInfo(IHttpRequestResponse requestResponse, long responseTime){
        this.requestResponse = requestResponse;
        this.responseTime = responseTime;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public IHttpRequestResponse getIRequestResponse() {
        return requestResponse;
    }
}
