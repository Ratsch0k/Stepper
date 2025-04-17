package com.coreyd97.stepper.step;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.coreyd97.stepper.Globals;
import com.coreyd97.stepper.MessageProcessor;
import com.coreyd97.stepper.Stepper;
import com.coreyd97.stepper.exception.SequenceExecutionException;
import com.coreyd97.stepper.sequence.StepSequenceState;
import com.coreyd97.stepper.step.listener.StepExecutionListener;
import com.coreyd97.stepper.variable.StepVariable;

import burp.IHttpRequestResponse;

public class StepExecutionable {
    private StepState state;
    public final StepStateVariableManager variableManager;
    private boolean updateState;
    private StepExecutionInfo lastExecutionInfo;

    public StepExecutionable(StepState state, boolean updateState) {
        this.state = state;
        this.variableManager = new StepStateVariableManager(state);
        
        this.updateState = updateState;
    }

    public StepExecutionResult executeStep(List<StepVariable> replacements) throws SequenceExecutionException {
        for (StepVariable variable : this.state.getVariableManager().getVariables()) {
            this.variableManager.addVariable(variable.copy());
        }

        List<StepVariable> stepVariables = new LinkedList<>(this.variableManager.getVariables());
        stepVariables.addAll(replacements);

        byte[] requestWithoutReplacements = this.state.getRequest();
        byte[] builtRequest;

        this.variableManager.updateVariablesBeforeExecution();

        if(MessageProcessor.hasStepVariable(requestWithoutReplacements)) {
//            if(MessageProcessor.isUnprocessable(requestWithoutReplacements)){
//                //If there's unicode issues, we're likely acting on binary data. Warn the user.
//                //TODO STEP SEQUENCE HANDLE BINARY ERRORS.
//                int result = JOptionPane.showConfirmDialog(Stepper.getInstance().getUI().getUiComponent(),
//                        "The request contains non UTF characters.\nStepper is able to make the replacements, " +
//                                "but some of the binary data may be lost. Continue?",
//                        "Stepper Replacement Error", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
//                if(result == JOptionPane.NO_OPTION) throw new SequenceCancelledException("Binary data, user cancelled.");
//            }
            // Collect all variables from this sequence
            builtRequest = MessageProcessor.makeReplacementsForSingleSequence(requestWithoutReplacements, stepVariables);
            HashMap<StepSequenceState, List<StepVariable>> allVariables = Stepper.instance.getSequenceManager().getRollingVariablesFromAllSequences();
            
            // Insert arguments into map of variables to support sequence variables from sequence and cross-sequences
            // In the case the variable is also present in the list of arguments it is replaced with it
            List<StepVariable> sequenceVariables = allVariables.get(this.state.getSequence());
            for (int sequenceVariableIndex = 0; sequenceVariableIndex < sequenceVariables.size(); sequenceVariableIndex++) {
                StepVariable sequenceVariable = sequenceVariables.get(sequenceVariableIndex);

                for (StepVariable replacementVariable : replacements) {
                    if (sequenceVariable.getIdentifier() == replacementVariable.getIdentifier()) {
                        sequenceVariables.set(sequenceVariableIndex, replacementVariable);
                    }
                }
            }

            builtRequest = MessageProcessor.makeReplacementsForAllSequences(builtRequest, allVariables);
        }else{
            builtRequest = Arrays.copyOf(requestWithoutReplacements, requestWithoutReplacements.length);
        }

        if(Stepper.getPreferences().getSetting(Globals.PREF_UPDATE_REQUEST_LENGTH)){
            builtRequest = MessageProcessor.updateContentLength(builtRequest);

            //TODO Find a way to reliably replace content-length of templated request.
            byte[] fixedContentLengthTemplate = MessageProcessor.updateContentLength(requestWithoutReplacements);
            //setRequestBody();
        }

        if (this.updateState) {
            this.state.setResponseBody(new byte[0]);
        }
        //Update the httpService
        //Part of hack to match VariableReplacementTab with actual IMessageEditorController
        this.state.setHttpService(Stepper.callbacks.getHelpers().buildHttpService(
                this.state.getHostname(), this.state.getPort(), this.state.isSSL()));

        //Add X-Stepper-Ignore header so its not picked up by messageProcessor
        builtRequest = MessageProcessor.addHeaderToRequest(builtRequest, MessageProcessor.STEPPER_IGNORE_HEADER);

        long start = new Date().getTime();
        //Update with response
        IHttpRequestResponse requestResponse = null;
        try {
            requestResponse = Stepper.callbacks.makeHttpRequest(this.state.getHttpService(), builtRequest);
        }catch (RuntimeException e){
            if(e.getMessage().isEmpty() || e.getMessage().equalsIgnoreCase(this.state.getHostname()))
                throw new RuntimeException(String.format("Failed to execute step \"%s\"", this.state.getTitle()));
        }
        long end = new Date().getTime();
        if(requestResponse.getResponse() == null)
            throw new SequenceExecutionException("The request to the server timed out.");


        this.lastExecutionInfo = new StepExecutionInfo(requestResponse, end-start);

        //Pull variables from response
        this.variableManager.updateVariablesAfterExecution(lastExecutionInfo);

        // Update step state
        if (this.updateState) {
            this.state.setResponseBody(requestResponse.getResponse());
            this.state.setLastExecutionResult(this.lastExecutionInfo);
            this.state.getVariableManager().updateVariablesAfterExecution(this.lastExecutionInfo);
        }

        // Retrive updated variables and return execution result
        return new StepExecutionResult(lastExecutionInfo, stepVariables);
    }
}
