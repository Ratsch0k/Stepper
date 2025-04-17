package com.coreyd97.stepper.util.variablereplacementstab;

import burp.IMessageEditorController;
import burp.IMessageEditorTab;
import burp.IMessageEditorTabFactory;
import com.coreyd97.stepper.sequencemanager.SequenceManager;
import com.coreyd97.stepper.step.StepState;
import com.coreyd97.stepper.sequence.StepSequenceState;

import java.util.Arrays;
import java.util.List;

public class VariableReplacementsTabFactory implements IMessageEditorTabFactory {

    private final SequenceManager sequenceManager;

    public VariableReplacementsTabFactory(SequenceManager sequenceManager){
        this.sequenceManager = sequenceManager;
    }

    @Override
    public IMessageEditorTab createNewInstance(IMessageEditorController controllerProxyInstance, boolean editable) {
        VariableReplacementsTab tab = new VariableReplacementsTab(sequenceManager, controllerProxyInstance, editable);
        IMessageEditorController actualController = findActualController(controllerProxyInstance);
        if(actualController instanceof StepState) {
            tab.setStep((StepState) actualController);
        }
        return tab;
    }

    private IMessageEditorController findActualController(IMessageEditorController controller){
        List<StepSequenceState> stepSequences = sequenceManager.getStepSequenceStates();
        byte[] requestMatchHack;

        try{
             requestMatchHack = controller.getRequest();
        }catch (Exception e){
            //The controller threw and exception when trying to get the request.
            //This is caused by the class which implements the controller, not stepper!
            return null;
        }

        for (StepSequenceState stepSequence : stepSequences) {
            for (StepState step : stepSequence.getSteps()) {
                if(Arrays.equals(requestMatchHack, step.getRequest())){
                    return step;
                }
            }
        }
        return null;
    }
}
