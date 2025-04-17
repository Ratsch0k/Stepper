package com.coreyd97.stepper.sequence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.coreyd97.stepper.exception.SequenceExecutionException;
import com.coreyd97.stepper.sequence.listener.SequenceExecutionListener;
import com.coreyd97.stepper.step.StepExecutionResult;
import com.coreyd97.stepper.step.StepExecutionable;
import com.coreyd97.stepper.step.StepState;
import com.coreyd97.stepper.variable.PreExecutionStepVariable;
import com.coreyd97.stepper.variable.StepVariable;
import com.coreyd97.stepper.variable.VariableManager;

public class StepSequenceExecutionable {
    private StepSequenceState state;
    private boolean updateState;

    public StepSequenceExecutionable(StepSequenceState state, boolean updateState) {
        this.state = state;
        this.updateState = updateState;
    }

    /**
     * Execute the step sequence with a predefined list of global variables.
     * 
     * Variables are set in the synchronization block which should ensure that if the sequence
     * has already been executed with another set of variable in between the extraction of the
     * variables from the header or comment, it is still executed with this set of variables.
     * @param arguments Map of global variables
     */
    public void execute(Map<String, String> arguments) throws SequenceExecutionException {
        Map<String, StepVariable> executionVariables = new HashMap<>();
        // Assign variables to global variables
        for (StepVariable variable : this.state.getGlobalVariableManager().getVariables()) {
            StepVariable executionVariable = variable.copy();
            executionVariables.put(executionVariable.getIdentifier(), executionVariable);

            if (arguments.containsKey(executionVariable.getIdentifier())) {
                String newValue = arguments.get(executionVariable.getIdentifier());

                executionVariable.setValue(newValue);
            }
        }
        ArrayList<StepExecutionable> executionableSteps = new ArrayList<>();

        for (StepState step : this.state.steps) {
            executionableSteps.add(new StepExecutionable(step, this.updateState));
        }

        for (SequenceExecutionListener listener : this.state.getExecutionListeners()) {
            listener.beforeSequenceStateStart(this.state.steps);
        }

        for (StepExecutionable executionable : executionableSteps) {
            // Insert pre-execution variables into variables passed to step
            for (PreExecutionStepVariable preVariable : executionable.variableManager.getPreExecutionVariables()) {
                executionVariables.put(preVariable.getIdentifier(), preVariable);
            }

            // Execute the step            
            StepExecutionResult result = executionable.executeStep(new ArrayList<>(executionVariables.values()));

            // Retrieve all variables from step and put them into the variables.
            // Will be passed to the next step
            for (StepVariable variable : result.getVariables()) {
                executionVariables.put(variable.getIdentifier(), variable);
            }

            for (SequenceExecutionListener listener : this.state.getExecutionListeners()) {
                listener.sequenceStepExecuted(result.getInfo());
            }
        }

        for (SequenceExecutionListener listener : this.state.getExecutionListeners()) {
            listener.afterSequenceEnd(true);
        }
    }
}
