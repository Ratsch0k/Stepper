package com.coreyd97.stepper.step;

import java.util.HashSet;
import java.util.Set;

import com.coreyd97.stepper.StepStateChangeListener;
import com.coreyd97.stepper.Stepper;
import com.coreyd97.stepper.sequence.StepSequenceState;

import burp.IHttpService;
import burp.IMessageEditor;
import burp.IMessageEditorController;

public class StepState implements IMessageEditorController {
    private final StepStateVariableManager variableManager;
    private String title;
    private IMessageEditor requestEditor;
    private IMessageEditor responseEditor;
    private byte[] requestBody;
    private byte[] responseBody;
    private String hostname;
    private Integer port;
    private Boolean isSSL;
    private IHttpService httpService;
    private final String matchHack = ("MATCHHACK." + Math.random() + ".coreyd97.com");
    private StepExecutionInfo lastExecutionInfo;
    private Set<StepStateChangeListener> listeners;
    private StepSequenceState sequence;

    public StepState(StepSequenceState sequence){
        this.variableManager = new StepStateVariableManager(this);
        this.requestBody = new byte[0];
        this.responseBody = new byte[0];
        this.hostname = "";
        this.port = 443;
        this.isSSL = true;
        this.listeners = new HashSet<StepStateChangeListener>();
        this.sequence = sequence;
    }

    public StepState(StepSequenceState sequence, String title){
        this(sequence);
        if(title != null) {
            this.title = title;
        }
    }


    public void setRequestBody(byte[] requestBody){
        this.requestBody = requestBody;
        if(this.requestEditor != null) {
            this.requestEditor.setMessage(requestBody, true);
        }
    }

    public void setResponseBody(byte[] responseBody){
        if(this.responseEditor != null) {
            this.responseEditor.setMessage(responseBody, false);
        }

    }

    public void setSequenceState(StepSequenceState sequence) {
        this.sequence = sequence;
    }

    public StepStateVariableManager getVariableManager() {
        return variableManager;
    }

    @Override
    public IHttpService getHttpService() {
        return this.httpService;
    }

    @Override
    public byte[] getRequest() {
        if(this.requestEditor == null) {
            return matchHack.getBytes();
        }
        return this.requestEditor.getMessage();
    }

    @Override
    public byte[] getResponse() {
        if(this.responseEditor == null) return responseBody;
        return this.responseEditor.getMessage();
    }

    private void tryUpdateHttpService(){
        try {
            this.httpService = Stepper.callbacks.getHelpers().buildHttpService(
                    this.hostname, this.port, this.isSSL);
        }catch (IllegalArgumentException e){
            //
        }
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
        tryUpdateHttpService();
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
        tryUpdateHttpService();
    }

    public boolean isSSL() {
        return isSSL;
    }

    public void setSSL(boolean SSL) {
        tryUpdateHttpService();
        isSSL = SSL;
    }

    public String getTargetString(){
        if(hostname.isEmpty()) return "Not specified";
        return "http" + (isSSL ? "s" : "") + "://" + hostname + (port != 80 && port != 443 ? ":" + port : "");
    }

    public boolean isValidTarget(){
        if(this.hostname != null && this.port != null && this.isSSL != null){
            try{
                Stepper.callbacks.getHelpers().buildHttpService(hostname, port, isSSL);
                return true;
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean isReadyToExecute(){
        return this.isValidTarget() && this.getRequest() != null && this.getRequest().length != 0;
    }


    public void setHttpService(IHttpService httpService) {
        this.hostname = httpService.getHost();
        this.port = httpService.getPort();
        this.isSSL = httpService.getProtocol().equalsIgnoreCase("https");
        tryUpdateHttpService();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void registerRequestEditor(IMessageEditor requestEditor) {
        this.requestEditor = requestEditor;
        this.requestEditor.setMessage(requestBody, true);
    }

    public void registerResponseEditor(IMessageEditor responseEditor){
        this.responseEditor = responseEditor;
        this.responseEditor.setMessage(responseBody, false);
    }

    public void setLastExecutionResult(StepExecutionInfo lastExecutionInfo) {
        this.lastExecutionInfo = lastExecutionInfo;

        for (StepStateChangeListener listener : this.listeners) {
            listener.onExecutionInfoChanged(lastExecutionInfo);
        }
    }

    public void registerStateChangeListener(StepStateChangeListener listener) {
        this.listeners.add(listener);
    }

    public void removeStateChangeListener(StepStateChangeListener listener) {
        this.listeners.remove(listener);
    }

    public StepExecutionInfo getLastExecutionResult() {
        return this.lastExecutionInfo;
    }

    public StepSequenceState getSequence() {
        return this.sequence;
    }
}
