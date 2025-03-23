package com.coreyd97.stepper;

import java.util.Map;

import com.coreyd97.stepper.sequence.StepSequence;
import com.coreyd97.stepper.sequence.StepSequenceState;

/**
 * Holds information about a step sequence as defined in a request with the stepper headers.
 * Includes information about all defined variables.
 * 
 * This information is stored either in the request's X-Stepper-Execute-Before and X-Steper-Execute-After header
 * as well in the user comments in the following format:
 * <sequence_name>: (<key>=<value>;)*
 * 
 * Here some examples to demonstrate how the string representation of this looks like:
 * <pre>
 * - `simpleExample: first=value`
 *  - Name: simpleExample
 *  - Variables:
 *      first: value
 * - `advancedExample: firstKey=firstValue;secondKey=secondValue; firstKey=overwrittenValue;`
 *  - Name: advancedExample
 *  - Variables:
 *      firstKey: overwrittenValue
 *      secondKey: secondValue
 * </pre>
 */
class RequestSequenceInformation {
    /**
     * Defines step sequence.
     */
    public StepSequenceState sequence;

    /**
     * All variables for the sequence as defined in the stepper header.
     */
    public Map<String, String> variables;

    public RequestSequenceInformation(StepSequenceState sequence, Map<String, String> variables) {
        this.sequence = sequence;
        this.variables = variables;
    }
}