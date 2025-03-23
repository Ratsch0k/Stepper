package com.coreyd97.stepper;

import burp.*;

import com.coreyd97.BurpExtenderUtilities.Preferences;
import com.coreyd97.stepper.sequence.StepSequence;
import com.coreyd97.stepper.sequencemanager.SequenceManager;
import com.coreyd97.stepper.util.ReplacingInputStream;
import com.coreyd97.stepper.variable.PreExecutionStepVariable;
import com.coreyd97.stepper.variable.StepVariable;

import javax.swing.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageProcessor implements IHttpListener {

    private final SequenceManager sequenceManager;
    private final Preferences preferences;
    public static final String EXECUTE_BEFORE_HEADER = "X-Stepper-Execute-Before";
    public static final String EXECUTE_AFTER_HEADER = "X-Stepper-Execute-After";
    public static final String EXECUTE_VAR_HEADER = "X-Stepper-Var";
    public static final String EXECUTE_BEFORE_REGEX = EXECUTE_BEFORE_HEADER + ":(.*)";
    public static final String EXECUTE_AFTER_REGEX = EXECUTE_AFTER_HEADER+":(.*)";
    public static final String EXECUTE_VAR_REGEX = EXECUTE_VAR_HEADER + ":(.*)";
    public static final String EXECUTE_AFTER_COMMENT_DELIMITER = "#%~%#";
    public static final Pattern EXECUTE_BEFORE_HEADER_PATTERN = Pattern.compile("^" + EXECUTE_BEFORE_REGEX + "$", Pattern.CASE_INSENSITIVE);
    public static final Pattern EXECUTE_AFTER_HEADER_PATTERN = Pattern.compile("^" + EXECUTE_AFTER_REGEX + "$", Pattern.CASE_INSENSITIVE);
    public static final Pattern EXECUTE_VAR_HEADER_PATTERN = Pattern.compile("^" + EXECUTE_VAR_REGEX + "$", Pattern.CASE_INSENSITIVE);
    public static final String STEPPER_IGNORE_HEADER = "X-Stepper-Ignore";
    public static final Pattern STEPPER_IGNORE_PATTERN = Pattern.compile("^"+STEPPER_IGNORE_HEADER, Pattern.CASE_INSENSITIVE);
    public static final Pattern STEPPER_SEQUENCE_NAME_PATTERN = Pattern.compile("^([^:]+)(?::?)", Pattern.CASE_INSENSITIVE);
    public static final Pattern VARIABLE_LIST_PATTERN = Pattern.compile("[^:]+(:\\s*(?<variables>.+))?", Pattern.CASE_INSENSITIVE);
    public static final Pattern VARIABLE_PATTERN = Pattern.compile("(?<key>[^=]+)=(?<value>[^;]+);?", Pattern.CASE_INSENSITIVE);
    public static final Pattern SINGLE_VARIABLE_PATTERN = Pattern.compile("(?<key>[^=]+)=(?<value>.*)", Pattern.CASE_INSENSITIVE);

    public MessageProcessor(SequenceManager sequenceManager, Preferences preferences){
        this.sequenceManager = sequenceManager;
        this.preferences = preferences;
    }

    public static boolean hasStepVariable(byte[] content) {
        Pattern identifierFinder = StepVariable.createIdentifierCaptureRegex();
        Matcher m = identifierFinder.matcher(new String(content));
        return m.find();
    }

    public static boolean isUnprocessable(byte[] content){
        //Check for charset decoding errors
        return new String(content).indexOf('\uFFFD') != -1;
    }

    @Override
    public void processHttpMessage(int toolFlag, boolean messageIsRequest, IHttpRequestResponse messageInfo) {
        IRequestInfo requestInfo = Stepper.callbacks.getHelpers().analyzeRequest(messageInfo.getRequest());

        if(hasHeaderMatchingPattern(requestInfo, STEPPER_IGNORE_PATTERN)){
            byte[] request = removeHeaderMatchingPattern(messageInfo.getRequest(), STEPPER_IGNORE_PATTERN);
            messageInfo.setRequest(request);
            return;
        }

        if(isValidTool(toolFlag)){

            if(messageIsRequest){
                byte[] request = messageInfo.getRequest();

                // Extract variable headers from request
                Map<String, String> sequenceArguments = extractSequenceArgumentsFromRequest(requestInfo, EXECUTE_VAR_HEADER_PATTERN);

                List<RequestSequenceInformation> preExecSequences = extractExecSequencesFromRequest(requestInfo, EXECUTE_BEFORE_HEADER_PATTERN);
                if(preExecSequences.size() > 0){
                    //Remove the headers from the request
                    request = removeHeaderMatchingPattern(request, EXECUTE_BEFORE_HEADER_PATTERN);

                    //Execute the sequences
                    for (RequestSequenceInformation requestSequenceInformation : preExecSequences) {
                        StepSequence sequence = requestSequenceInformation.sequence;
                        Map<String, String> variables = requestSequenceInformation.variables;

                        // Merge arguments from variable headers into arguments for this sequence
                        // Prioritize arguments specifically set for this sequence
                        for (Map.Entry<String, String> entry : sequenceArguments.entrySet()) {
                            variables.putIfAbsent(entry.getKey(), entry.getValue());
                        }

                        sequence.executeBlocking(variables);
                    }
                }

                List<RequestSequenceInformation> postExecSequences = extractExecSequencesFromRequest(requestInfo, EXECUTE_AFTER_HEADER_PATTERN);

                if(postExecSequences.size() > 0){
                    //Remove the headers from the request
                    request = removeHeaderMatchingPattern(request, EXECUTE_AFTER_HEADER_PATTERN);

                    //Joining the sequences as string to set them as a comment to catch them in the response
                    String postExecSequencesJoined = EXECUTE_AFTER_HEADER + ":";
                    for (RequestSequenceInformation requestSequenceInformation : postExecSequences) {
                        StepSequence sequence = requestSequenceInformation.sequence;
                        Map<String, String> variables = requestSequenceInformation.variables;
                        String variableString = "";
                        if (variables.size() > 0) {
                            variableString += ":";
                            for (Map.Entry<String, String> entry : variables.entrySet()) {
                                variableString += entry.getKey() + "=";
                                variableString += entry.getValue() + ";";
                            }
                        }

                        postExecSequencesJoined += sequence.getTitle() + variableString + EXECUTE_AFTER_COMMENT_DELIMITER;
                    }

                    if(!postExecSequencesJoined.isEmpty()){
                        messageInfo.setComment(messageInfo.getComment() + postExecSequencesJoined);
                    }
                }


                HashMap<StepSequence, List<StepVariable>> allVariables = sequenceManager.getRollingVariablesFromAllSequences();

                if(allVariables.size() > 0 && hasStepVariable(request)) {

                    if(isUnprocessable(messageInfo.getRequest())){
                        //If there's unicode issues, we're likely acting on binary data. Warn the user.
                        int result = JOptionPane.showConfirmDialog(Stepper.getUI().getUiComponent(),
                                "The request contains non UTF characters.\nStepper is able to make the replacements, " +
                                        "but some of the binary data may be lost. Continue?",
                                "Stepper Replacement Error", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        if(result == JOptionPane.NO_OPTION) return;
                    }

                    try {
                        request = makeReplacementsForAllSequences(request, allVariables);

                        if(preferences.getSetting(Globals.PREF_UPDATE_REQUEST_LENGTH)){
                            request = updateContentLength(request);
                        }
                    } catch (UnsupportedOperationException e) { /**Read-only message**/ }
                }

                //Save any changes made to the request.
                messageInfo.setRequest(request);
            }else{
                // this is a response so we check for the comments
                List<RequestSequenceInformation> postExecSequences = extractExecSequencesFromComment(messageInfo.getComment(), EXECUTE_AFTER_HEADER_PATTERN);
                if(postExecSequences.size() > 0){
                    //Execute the sequences
                    for (RequestSequenceInformation requestSequenceInformation : postExecSequences) {
                        StepSequence sequence = requestSequenceInformation.sequence;
                        Map<String, String> variables = requestSequenceInformation.variables;
                        sequence.executeBlocking(variables);
                    }
                    // remove the added comment from the request
                    messageInfo.setComment(messageInfo.getComment().replaceAll(EXECUTE_AFTER_REGEX+EXECUTE_AFTER_COMMENT_DELIMITER,""));
                }
            }
        }
    }

    private boolean isValidTool(int toolFlag){
        if(preferences.getSetting(Globals.PREF_VARS_IN_ALL_TOOLS)) return true;
        switch (toolFlag){
            case IBurpExtenderCallbacks.TOOL_PROXY:
                return (boolean) preferences.getSetting(Globals.PREF_VARS_IN_PROXY);
            case IBurpExtenderCallbacks.TOOL_REPEATER:
                return (boolean) preferences.getSetting(Globals.PREF_VARS_IN_REPEATER);
            case IBurpExtenderCallbacks.TOOL_INTRUDER:
                return (boolean) preferences.getSetting(Globals.PREF_VARS_IN_INTRUDER);
            case IBurpExtenderCallbacks.TOOL_SCANNER:
                return (boolean) preferences.getSetting(Globals.PREF_VARS_IN_SCANNER);
            case IBurpExtenderCallbacks.TOOL_SEQUENCER:
                return (boolean) preferences.getSetting(Globals.PREF_VARS_IN_SEQUENCER);
            case IBurpExtenderCallbacks.TOOL_SPIDER:
                return (boolean) preferences.getSetting(Globals.PREF_VARS_IN_SPIDER);
            case IBurpExtenderCallbacks.TOOL_EXTENDER:
                return (boolean) preferences.getSetting(Globals.PREF_VARS_IN_EXTENDER);
            default:
                return false;
        }
    }

    /**
     * Used to make replacements with variables from this sequence only.
     * Used for steps within a sequence.
     * @param originalContent
     * @param variables
     * @return
     */
    public static byte[] makeReplacementsForSingleSequence(byte[] originalContent, List<StepVariable> variables) {
        byte[] request = Arrays.copyOf(originalContent, originalContent.length);

        List<ReplacingInputStream.Replacement> replacements = new ArrayList<>();
        for (StepVariable variable : variables) {
            String match = StepVariable.createVariableString(variable.getIdentifier());
            String replace = variable.getValue();
            ReplacingInputStream.Replacement replacement = new ReplacingInputStream.Replacement(match.getBytes(StandardCharsets.UTF_8), replace.getBytes(StandardCharsets.UTF_8));
            replacements.add(replacement);
        }
        ReplacingInputStream inputStream = new ReplacingInputStream(new ByteArrayInputStream(request), replacements);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int b;
        try {
            while (-1 != (b = inputStream.read())) {
                bos.write(b);
            }
        }catch (IOException e){ /**TODO**/ }

        return bos.toByteArray();
    }

    /**
     * Used to make replacements with variables from multiple sequences
     * Used when variables have been used in tools other than stepper.
     * @param originalContent
     * @param sequenceVariableMap
     * @return
     */
    public static byte[] makeReplacementsForAllSequences(byte[] originalContent,
                                                         HashMap<StepSequence, List<StepVariable>> sequenceVariableMap) {
        byte[] request = Arrays.copyOf(originalContent, originalContent.length);

        List<ReplacingInputStream.Replacement> replacements = new ArrayList<>();
        for (Map.Entry<StepSequence, List<StepVariable>> sequenceEntry : sequenceVariableMap.entrySet()) {
            StepSequence sequence = sequenceEntry.getKey();
            List<StepVariable> variables = sequenceEntry.getValue();
            for (StepVariable variable : variables) {
                String match = StepVariable.createVariableString(sequence.getTitle(), variable.getIdentifier());
                String replace = variable.getValue();
                ReplacingInputStream.Replacement replacement = new ReplacingInputStream.Replacement(match.getBytes(StandardCharsets.UTF_8), replace.getBytes(StandardCharsets.UTF_8));
                replacements.add(replacement);
            }
        }
        ReplacingInputStream inputStream = new ReplacingInputStream(new ByteArrayInputStream(request), replacements);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int b;
        try {
            while (-1 != (b = inputStream.read())) {
                bos.write(b);
            }
        }catch (IOException e){ /**TODO**/ }

        return bos.toByteArray();
    }

    public static byte[] updateContentLength(byte[] request){
        IRequestInfo requestInfo = Stepper.callbacks.getHelpers().analyzeRequest(request);
        List<String> newRequestHeaders = requestInfo.getHeaders();
        byte[] newBody = Arrays.copyOfRange(request, requestInfo.getBodyOffset(), request.length);

        //The method below automatically updates content-length.
        return Stepper.callbacks.getHelpers().buildHttpMessage(newRequestHeaders, newBody);
    }

    /**
     * Extract all defined variables from the request sequence information string.
     * 
     * If a variable has more than one definition, the later one is used. 
     * It expects the string to have a format that matches the definition in RequesteSequenceInformation.
     * 
     * For example, the string: `test: firstVariable=old; secondVariable=someValue; firstVariable=new`
     * would be converted to a map with the following content:
     * - firstVariable: new
     * - secondVariable: someValue
     * 
     * @see RequestSequenceInformation
     * @param sequenceInfoString The string that contains sequence information
     * @return All defined variables
     */
    public Map<String, String> extractVariablesFromSequenceInfoString(String sequenceInfoString) {
        HashMap<String, String> map = new HashMap<>();

        Matcher variableListMatcher = VARIABLE_LIST_PATTERN.matcher(sequenceInfoString);

        if (variableListMatcher.find() && variableListMatcher.group("variables") != null) {
            String variableList = variableListMatcher.group("variables");
            Matcher variablesMatcher = VARIABLE_PATTERN.matcher(variableList);

            while (variablesMatcher.find()) {
                String variableKey = variablesMatcher.group("key");
                String variableValue = variablesMatcher.group("value");

                map.put(variableKey, variableValue);
            }
        }

        return map;
    }

    /**
     * Extract a sequence's name from the request info string.
     * @param sequenceInfoString String that contains the name and variables of a step sequence
     * @return The sequence name
     */
    private Optional<String> extractSequenceNameFromSequenceInfoString(String sequenceInfoString) {
        Matcher nameMatcher = STEPPER_SEQUENCE_NAME_PATTERN.matcher(sequenceInfoString);
        if (!nameMatcher.find()) {
            return Optional.empty();
        }

        return Optional.of(nameMatcher.group(1).trim());
    }

    /**
     * Extract arguments from variable headers from the given request.
     * @param requestInfo The request
     * @param pattern Pattern used to identify variable header
     * @return Map of all sequence arguments
     */
    private Map<String, String> extractSequenceArgumentsFromRequest(IRequestInfo requestInfo, Pattern pattern) {
        Stepper.callbacks.printOutput("[MessageProcessor] extract arguments");        
        //Check if headers ask us to execute a request before the request.
        List<String> requestHeaders = requestInfo.getHeaders();
        Map<String, String> arguments = new HashMap<>();

        for (Iterator<String> iterator = requestHeaders.iterator(); iterator.hasNext(); ) {
            String header = iterator.next();
            Stepper.callbacks.printOutput("[MessageProcessor] processing header: " + header);

            Matcher m = pattern.matcher(header);
            if (!m.matches()) {
                continue;
            }

            String variableInfo = m.group(1).trim();

            Stepper.callbacks.printOutput("[MessageProcessor] processing variable info: " + variableInfo);

            Matcher argumentMatcher = SINGLE_VARIABLE_PATTERN.matcher(variableInfo);
            if (!argumentMatcher.matches()) {
                Stepper.callbacks.printOutput("[MessageProcessor] found variable header without value");
                continue;
            }

            String variableKey = argumentMatcher.group("key");
            String variableValue = argumentMatcher.group("value");

            Stepper.callbacks.printOutput("[MessageProcessor] got argument: " + variableKey + "=" + variableValue);

            arguments.put(variableKey, variableValue);
        }

        return arguments;
    }

    /**
     * Locates the X-Stepper-Execute-Before or X-Stepper-Execute-After headers and returns the matching sequences.
     * @param requestInfo
     * @param pattern
     * @return Optional value of step sequence to execute before or after the request.
     */
    public List<RequestSequenceInformation> extractExecSequencesFromRequest(IRequestInfo requestInfo, Pattern pattern){
        //Check if headers ask us to execute a request before the request.
        List<String> requestHeaders = requestInfo.getHeaders();
        ArrayList<RequestSequenceInformation> execSequences = new ArrayList<>();

        for (Iterator<String> iterator = requestHeaders.iterator(); iterator.hasNext(); ) {
            String header = iterator.next();

            Matcher m = pattern.matcher(header);
            if (!m.matches()) {
                continue;
            }

            ArrayList<StepSequence> currentSequences = new ArrayList<>();
            String stepperHeader = m.group(1).trim();

            Optional<String> optionalName = this.extractSequenceNameFromSequenceInfoString(stepperHeader);

            if (optionalName.isEmpty()) {
                continue;
            }

            String sequenceName = optionalName.get();

            Optional<StepSequence> execSequence = sequenceManager.getSequences().stream()
                    .filter(sequence -> sequence.getTitle().equalsIgnoreCase(sequenceName))
                    .findFirst();

            if(execSequence.isPresent())
                currentSequences.add(execSequence.get());
            else
                JOptionPane.showMessageDialog(Stepper.getUI().getUiComponent(), "Could not find execution sequence named: \"" + sequenceName + "\".");

            // Extract variables and store them in the sequences
            Map<String, String> arguments = extractVariablesFromSequenceInfoString(stepperHeader);

            for (StepSequence sequence : currentSequences) {
                execSequences.add(new RequestSequenceInformation(sequence, arguments));
            }
        }
        return execSequences;
    }

    /**
     * Locates the X-Stepper-Execute-After pattern in the comment and returns the matching sequences.
     * @param comment
     * @param pattern
     * @return Optional value of step sequence to execute after the request.
     */
    public List<RequestSequenceInformation> extractExecSequencesFromComment(String comment, Pattern pattern){
        ArrayList<RequestSequenceInformation> execSequences = new ArrayList<>();
        Matcher m = pattern.matcher(comment);
        if (m.find()) {
            String[] allSequences = m.group(1).split(EXECUTE_AFTER_COMMENT_DELIMITER);
            for(String sequenceInfo : allSequences){
                if(sequenceInfo != null && !sequenceInfo.isEmpty()){
                    Optional<String> optionalName = this.extractSequenceNameFromSequenceInfoString(sequenceInfo);

                    if (optionalName.isEmpty()) {
                        continue;
                    }

                    String sequenceName = optionalName.get();

                    Optional<StepSequence> execSequence = sequenceManager.getSequences().stream()
                            .filter(sequence -> sequence.getTitle().equalsIgnoreCase(sequenceName))
                            .findFirst();

                    Map<String, String> variables = this.extractVariablesFromSequenceInfoString(sequenceInfo);

                    if(execSequence.isPresent())
                        execSequences.add(new RequestSequenceInformation(execSequence.get(), variables));
                    else
                        JOptionPane.showMessageDialog(Stepper.getUI().getUiComponent(), "Could not find execution sequence named: \"" + sequenceInfo + "\".");
                }
            }
        }

        return execSequences;
    }

    public static boolean hasHeaderMatchingPattern(IRequestInfo requestInfo, Pattern pattern){
        return requestInfo.getHeaders().stream().anyMatch(s -> pattern.asPredicate().test(s));
    }

    public static byte[] addHeaderToRequest(byte[] request, String header){
        IRequestInfo requestInfo = Stepper.callbacks.getHelpers().analyzeRequest(request);
        List<String> headers = requestInfo.getHeaders();
        headers.add(header);

        byte[] messageBody = Arrays.copyOfRange(request, requestInfo.getBodyOffset(), request.length);

        return Stepper.callbacks.getHelpers().buildHttpMessage(headers, messageBody);
    }

    public static byte[] removeHeaderMatchingPattern(byte[] request, Pattern pattern){
        IRequestInfo newRequestInfo = Stepper.callbacks.getHelpers().analyzeRequest(request);
        List<String> newRequestHeaders = newRequestInfo.getHeaders();
        newRequestHeaders.removeIf(s -> {
            Matcher m = pattern.matcher(s);
            return m.matches();
        });

        byte[] messageBody = Arrays.copyOfRange(request, newRequestInfo.getBodyOffset(), request.length);

        return Stepper.callbacks.getHelpers().buildHttpMessage(newRequestHeaders, messageBody);
    }
}
