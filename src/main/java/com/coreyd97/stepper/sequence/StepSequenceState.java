package com.coreyd97.stepper.sequence;

import burp.IHttpRequestResponse;

import com.coreyd97.stepper.Stepper;
import com.coreyd97.stepper.sequence.listener.SequenceExecutionListener;
import com.coreyd97.stepper.sequence.listener.SequenceStateListener;
import com.coreyd97.stepper.step.StepState;
import com.coreyd97.stepper.variable.PreExecutionStepVariable;
import com.coreyd97.stepper.variable.VariableManager;
import com.coreyd97.stepper.variable.StepVariable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class StepSequenceState {
    private String title;
    private VariableManager globalVariablesManager;
    public List<StepState> steps;
    private final List<SequenceStateListener> sequenceListeners;
    private final List<SequenceExecutionListener> sequenceExecutionListeners;

    public StepSequenceState(String title, List<SequenceStateListener> stepListeners, List<SequenceExecutionListener> sequenceExecutionListeners) {
        this.steps = new ArrayList<>();
        this.globalVariablesManager = new GlobalVariableManager();
        this.title = title;
        this.sequenceListeners = stepListeners;
        this.sequenceExecutionListeners = sequenceExecutionListeners;
    }

    public StepSequenceState(String title) {
        this(title, new ArrayList<>(), new ArrayList<>()); 
    }

    public StepSequenceState(){
        this("Step Sequence");
    }

    public void addStep(StepState step){
        this.steps.add(step);
        for (SequenceStateListener stepListener : this.sequenceListeners) {
            try {
                stepListener.onStepAdded(step);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void addStep(){
        this.addStep(new StepState(this));
    }

    public void addStep(IHttpRequestResponse requestResponse) {
        StepState step = new StepState(this);
        step.setRequestBody(requestResponse.getRequest());
        step.setResponseBody(requestResponse.getResponse());
        step.setHttpService(requestResponse.getHttpService());
        addStep(step);
    }

    public void stepModified(StepState step){
        for (SequenceStateListener stepListener : this.sequenceListeners) {
            stepListener.onStepUpdated(step);
        }
    }

    public void removeStep(StepState step) {
        if(!this.steps.remove(step)) throw new IllegalArgumentException("Step not valid for sequence");
        for (SequenceStateListener stepListener : this.sequenceListeners) {
            stepListener.onStepRemoved(step);
        }
    }

    public List<StepState> getSteps() {
        return this.steps;
    }

    public void moveStep(int from, int to){
        if(to > from){ //Moving to the right. Take 1 from to index since we'll remove this one first.
            to--;
        }
        StepState movedStep = this.steps.remove(from);
        this.steps.add(to, movedStep);
    }

    public void addSequenceExecutionListener(SequenceExecutionListener listener){
        this.sequenceExecutionListeners.add(listener);
    }

    public void removeSequenceExecutionListener(SequenceExecutionListener listener){
        this.sequenceExecutionListeners.remove(listener);
    }

    public void addStepListener(SequenceStateListener listener){
        this.sequenceListeners.add(listener);
    }

    public void removeStepListener(SequenceStateListener listener){
        this.sequenceListeners.remove(listener);
    }

    /**
     * Returns all variables, if a variable is overwritten in a later step.
     * Only includes the latest instance
     * @return List of all variables
     */
    public List<StepVariable> getRollingVariablesForWholeSequence() {
        return getRollingVariablesUpToStep(null); //Null for whole sequence
    }

    /**
     * Returns all pre and post variables up to the given step, and the pre variables of the step.
     * If a variable is overwritten in a later step, only includes the latest instance.
     * @return List of all variables
     */
    public List<StepVariable> getRollingVariablesUpToStep(StepState uptoStep){
        LinkedHashMap<String, StepVariable> rolling = new LinkedHashMap<>();
        for (StepVariable variable : this.globalVariablesManager.getVariables()) {
            rolling.put(variable.getIdentifier(), variable);
        }

        for (StepState step : this.steps) {
            if(uptoStep == step){
                for (PreExecutionStepVariable preExecutionVariable : step.getVariableManager().getPreExecutionVariables()) {
                    rolling.put(preExecutionVariable.getIdentifier(), preExecutionVariable);
                }
                break;
            }
            for (StepVariable variable : step.getVariableManager().getVariables()) {
                rolling.put(variable.getIdentifier(), variable);
            }
        }


        return new ArrayList<>(rolling.values());
    }

    public List<SequenceStateListener> getSequenceListeners() {
        return sequenceListeners;
    }

    public List<SequenceExecutionListener> getExecutionListeners() {
        return this.sequenceExecutionListeners;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public VariableManager getGlobalVariableManager() {
        return this.globalVariablesManager;
    }
     
}
