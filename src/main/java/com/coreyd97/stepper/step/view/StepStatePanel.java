package com.coreyd97.stepper.step.view;

import burp.IMessageEditor;
import com.coreyd97.BurpExtenderUtilities.Alignment;
import com.coreyd97.BurpExtenderUtilities.PanelBuilder;
import com.coreyd97.stepper.StepStateChangeListener;
import com.coreyd97.stepper.Stepper;
import com.coreyd97.stepper.exception.SequenceCancelledException;
import com.coreyd97.stepper.exception.SequenceExecutionException;
import com.coreyd97.stepper.sequence.view.SequenceStateContainer;
import com.coreyd97.stepper.step.StepExecutionInfo;
import com.coreyd97.stepper.step.StepExecutionable;
import com.coreyd97.stepper.step.StepState;
import com.coreyd97.stepper.util.Utils;
import com.coreyd97.stepper.variable.StepVariable;
import com.coreyd97.stepper.variable.listener.StepVariableListener;
import com.formdev.flatlaf.FlatLaf;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

public class StepStatePanel extends JPanel implements StepVariableListener {

    private final SequenceStateContainer sequenceContainer;
    private final StepState step;

    private IMessageEditor requestEditor;
    private IMessageEditor responseEditor;
    private JSplitPane reqRespSplitPane;
    private JLabel responseLengthLabel;
    private JLabel responseTimeLabel;

    private JLabel targetLabel;

    public StepStatePanel(SequenceStateContainer sequenceContainer, StepState step){
        super(new BorderLayout());
        this.sequenceContainer = sequenceContainer;
        this.step = step;

        //During message editor creation, the VariableReplacementTab will be matched with the actual
        //IHttpController implementation, not the proxy class.
        this.requestEditor = Stepper.callbacks.createMessageEditor(step, true);
        this.requestEditor.setMessage(step.getRequest(), true);
        this.responseEditor = Stepper.callbacks.createMessageEditor(step, false);
        this.responseEditor.setMessage(step.getResponse(), false);
        this.step.registerRequestEditor(this.requestEditor);
        this.step.registerResponseEditor(this.responseEditor);

        JPanel responseWrapper = new JPanel(new BorderLayout());
        JPanel responseInfoWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        responseInfoWrapper.add(new JLabel("Response Length: "));
        responseLengthLabel = new JLabel("N/A");
        responseInfoWrapper.add(responseLengthLabel);
        responseInfoWrapper.add(new JLabel(" |  Response Time: "));
        responseTimeLabel = new JLabel("N/A");
        responseInfoWrapper.add(responseTimeLabel);

        this.step.registerStateChangeListener(new StepStateChangeListener(){
            @Override
            public void onExecutionInfoChanged(StepExecutionInfo executionInfo) {
                String lengthMsg = String.format("%d bytes",executionInfo.getIRequestResponse().getResponse().length);
                StepStatePanel.this.responseLengthLabel.setText(lengthMsg);
                String timeMsg = String.format("%d ms", executionInfo.getResponseTime());
                StepStatePanel.this.responseTimeLabel.setText(timeMsg);
            }
        });

        responseWrapper.add(responseEditor.getComponent(), BorderLayout.CENTER);
        responseWrapper.add(responseInfoWrapper, BorderLayout.SOUTH);

        VariablePanel preExecVariablePanel = new PreExecVariablePanel(step.getVariableManager());
        JSplitPane requestSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                requestEditor.getComponent(), preExecVariablePanel);
        requestSplitPane.setResizeWeight(0.8);

        VariablePanel postExecVariablePanel = new PostExecVariablePanel(step.getVariableManager());
        JSplitPane responseSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                responseWrapper, postExecVariablePanel);
        responseSplitPane.setResizeWeight(0.8);

        //Make resizing one split pane also resize the other
        requestSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent pce) {
                        responseSplitPane.setDividerLocation(requestSplitPane.getDividerLocation());
                    }
                });
        //Same as above
        responseSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent pce) {
                        requestSplitPane.setDividerLocation(responseSplitPane.getDividerLocation());
                    }
                });

        this.reqRespSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, requestSplitPane, responseSplitPane);
        this.reqRespSplitPane.setResizeWeight(0.5);

        JButton executeStepButton = new JButton("Execute Step");
        executeStepButton.setMargin(new Insets(7,7,7,7));

        executeStepButton.addActionListener(actionEvent -> {
            new Thread(() -> {
                executeStepButton.setEnabled(false);
                try {
                    StepExecutionable executionable = new StepExecutionable(step, true);

                    executionable.executeStep(new ArrayList<StepVariable>());
                }catch (SequenceCancelledException ignored){
                }catch (SequenceExecutionException e) {
                    JOptionPane.showMessageDialog(this, e.getMessage(),
                            "Step Error", JOptionPane.ERROR_MESSAGE);
                }catch (Exception e){
                    JOptionPane.showMessageDialog(this, e.getMessage(),
                            "Step Error", JOptionPane.ERROR_MESSAGE);
                }finally {
                    executeStepButton.setEnabled(true);
                }
            }).start();
        });

        JButton editTargetButton = new JButton("Edit");

        Runnable setEditButtonIcon = () -> {
            String editIconURI = FlatLaf.isLafDark() ? "resources/Media/dark/edit.png" : "resources/Media/edit.png";
            ImageIcon editIcon = Utils.loadImage(editIconURI, 15, 15);
            editTargetButton.setIcon(editIcon);
            if(editIcon != null) {
                editTargetButton.setText(null);
            }else{
                editTargetButton.setText("Edit");
            }
        };
        setEditButtonIcon.run();
        editTargetButton.addPropertyChangeListener("UI", propertyChangeEvent -> setEditButtonIcon.run());

        editTargetButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showHttpDialog();
            }
        });

        targetLabel = new JLabel(step.getTargetString());
        targetLabel.setBorder(BorderFactory.createEmptyBorder(0,10,0,10));

        JSeparator horizontalSeparator = new JSeparator(JSeparator.HORIZONTAL);
        PanelBuilder panelBuilder = new PanelBuilder();
        panelBuilder.setComponentGrid(new Component[][]{
                new Component[]{executeStepButton, new JLabel("Target:", SwingConstants.TRAILING), targetLabel, editTargetButton},
                new Component[]{horizontalSeparator, horizontalSeparator, horizontalSeparator, horizontalSeparator},
                new Component[]{reqRespSplitPane, reqRespSplitPane, reqRespSplitPane, reqRespSplitPane},
        });
        panelBuilder.setGridWeightsX(new int[][]{
                new int[]{0, 1, 0, 0},
                new int[]{0, 0, 0, 0},
                new int[]{0, 0, 0, 0}
        });
        panelBuilder.setGridWeightsY(new int[][]{
                new int[]{0, 0, 0, 0},
                new int[]{0, 0, 0, 0},
                new int[]{1, 1, 1, 1}
        });
        panelBuilder.setAlignment(Alignment.FILL);

        JPanel builtPanel = panelBuilder.build();
        this.add(builtPanel, BorderLayout.CENTER);

        this.revalidate();
        this.repaint();
    }

    private void showHttpDialog(){

        JTextField httpAddressField = new JTextField();
        httpAddressField.setText(step.getHostname());
        JSpinner httpPortSpinner = new JSpinner(new SpinnerNumberModel(step.getPort().intValue(),1,65535,1));
        httpPortSpinner.setEditor(new JSpinner.NumberEditor(httpPortSpinner,"#"));
        JCheckBox httpIsSecure = new JCheckBox("Is HTTPS?");
        httpIsSecure.setSelected(step.isSSL());
        JLabel info = new JLabel("Specify the details of the server to which the request will be sent.");

        PanelBuilder panelBuilder = new PanelBuilder();
        panelBuilder.setAlignment(Alignment.FILL);
        panelBuilder.setComponentGrid(new Component[][]{
                new Component[]{info, info},
                new Component[]{new JLabel("Host"), httpAddressField},
                new Component[]{new JLabel("Port"), httpPortSpinner},
                new Component[]{httpIsSecure, null},
        });

        int answer = JOptionPane.showConfirmDialog(this, panelBuilder.build(), "HTTP Configuration", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if(answer == JOptionPane.YES_OPTION){
            step.setHostname(httpAddressField.getText());
            step.setPort((Integer) httpPortSpinner.getValue());
            step.setSSL(httpIsSecure.isSelected());
            targetLabel.setText(step.getTargetString());
        }
    }

    public IMessageEditor getRequestEditor() {
        return requestEditor;
    }

    public IMessageEditor getResponseEditor() {
        return responseEditor;
    }

    public void refreshRequestPanel() {
        //Reload the request viewer content. (Updates the With Replacements Tab!)
        this.requestEditor.setMessage(this.step.getRequest(), true);
    }

    public StepState getStep() {
        return step;
    }


    @Override
    public void onVariableAdded(StepVariable variable) {
        this.sequenceContainer.updateSubsequentPanels(this);
    }

    @Override
    public void onVariableRemoved(StepVariable variable) {
        this.sequenceContainer.updateSubsequentPanels(this);
    }

    @Override
    public void onVariableChange(StepVariable variable) {
        this.sequenceContainer.updateSubsequentPanels(this);
    }
}
