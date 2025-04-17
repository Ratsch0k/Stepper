package com.coreyd97.stepper;

import java.util.HashMap;
import java.util.Map;

import com.coreyd97.stepper.exception.SequenceExecutionException;
import com.coreyd97.stepper.sequence.StepSequenceExecutionable;
import com.coreyd97.stepper.sequence.StepSequenceState;
import com.coreyd97.stepper.variable.StepVariable;

public class StepSequenceExecutor {
    public static Map<String, StepVariable> execute(StepSequenceState sequence) {
        return StepSequenceExecutor.execute(sequence, new HashMap<>(), false);
    }

    public static Map<String, StepVariable> execute(StepSequenceState sequence, boolean updateState) {
        return StepSequenceExecutor.execute(sequence, new HashMap<>(), updateState);
    }
    
    public static Map<String, StepVariable> execute(StepSequenceState sequence, Map<String, String> variables, boolean updateState) {
        StepSequenceExecutionable executionable = new StepSequenceExecutionable(sequence, updateState);

        try {
            return executionable.execute(variables);
        } catch (SequenceExecutionException e) {
            Stepper.callbacks.printError("Error occurred while executing sequence: " + e);
            return new HashMap<String, StepVariable>();
        }
    }
}
