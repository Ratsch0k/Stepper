package com.coreyd97.stepper.sequence.view;

import com.coreyd97.BurpExtenderUtilities.CustomTabComponent;
import com.coreyd97.stepper.Stepper;
import com.coreyd97.stepper.sequence.StepSequence;
import com.coreyd97.stepper.sequence.StepSequenceState;
import com.coreyd97.stepper.sequence.listener.SequenceStateListener;
import com.coreyd97.stepper.step.Step;
import com.coreyd97.stepper.step.StepState;
import com.coreyd97.stepper.step.listener.StepAdapter;
import com.coreyd97.stepper.step.view.StepStatePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.function.Consumer;

public class SequenceStateContainer extends JPanel {
    private final StepSequenceState stepSequence;
    private HashMap<StepState, StepStatePanel> stepToPanelMap;
    private JTabbedPane tabbedContainer;

    public SequenceStateContainer(StepSequenceState stepSequence){
        super(new BorderLayout());
        this.stepSequence = stepSequence;
        this.stepToPanelMap = new HashMap<>();
        this.tabbedContainer = buildTabbedContainer();
        this.add(tabbedContainer, BorderLayout.CENTER);

        //Add panels for existing steps
        for (StepState step : this.stepSequence.getSteps()) {
            addPanelForStep(step);
        }

        //Listen for new steps and changes to the sequence globals
        this.stepSequence.addStepListener(new SequenceStateListener(){
            @Override
            public void onStepAdded(StepState step) {
                addPanelForStep(step);
            }

            @Override
            public void onStepRemoved(StepState step) {
                removePanelForStep(step);
            }

            @Override
            public void onStepUpdated(StepState step) {

            }
        });
    }

    private JTabbedPane buildTabbedContainer(){
        JTabbedPane tabbedPanel = new JTabbedPane();

        tabbedPanel.addTab("Globals", new SequenceStateGlobalsPanel(this.stepSequence));
        tabbedPanel.addTab("Add Step", null);
        CustomTabComponent addStepTab = new CustomTabComponent("Add Step");
        tabbedPanel.setTabComponentAt(1, addStepTab);
        addStepTab.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isLeftMouseButton(e)){
                    stepSequence.addStep();
                }
            }
        });

        return tabbedPanel;
    }

    private void addTabForStep(StepState step, StepStatePanel panel){
        int tabNumber = tabbedContainer.getTabCount()-1;
        tabbedContainer.insertTab(null, null, panel, null, tabNumber);

        Consumer<String> onTitleChanged = step::setTitle;

        Consumer<Void> onRemoveClicked = (nothing) -> {
            int result = JOptionPane.showConfirmDialog(tabbedContainer, "Are you sure you want to remove this step? (" + step.getTitle() + ")", "Remove Step", JOptionPane.YES_NO_OPTION);
            if(result == JOptionPane.YES_OPTION) {
                this.stepSequence.removeStep(step);
                //Update indices for other tabs
                updateTabIndices();
            }
        };

        CustomTabComponent tabComponent = new CustomTabComponent(tabNumber,
                step.getTitle(), true,
                true, onTitleChanged,true, onRemoveClicked);
        tabComponent.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if(SwingUtilities.isRightMouseButton(e)){
                    JPopupMenu popupMenu = new JPopupMenu();
                    JMenu moveStep = new JMenu("Move Step");

                    int fromIndex = tabbedContainer.indexOfTabComponent(tabComponent);
                    Component tabBody = tabbedContainer.getComponentAt(fromIndex);
                    for(int i=1; i<tabbedContainer.getTabCount()-1; i++){ //Start at 1, globals is 0. Stop -1 for "add step" tab
                        if(i != fromIndex){
                            JMenuItem moveTo = new JMenuItem("Index: " + i);
                            int finalI = i;
                            moveTo.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent actionEvent) {
                                    int toIndex = finalI;
                                    stepSequence.moveStep(fromIndex - 1, toIndex-1); //Take 1, since globals tab is first
                                    tabbedContainer.removeTabAt(fromIndex); //Remove existing tab
                                    tabbedContainer.insertTab("null", null, tabBody, null, toIndex);
                                    tabbedContainer.setTabComponentAt(toIndex, tabComponent);
                                    tabbedContainer.setSelectedIndex(toIndex);
                                    updateTabIndices();
                                }
                            });
                            moveStep.add(moveTo);
                        }
                    }

                    popupMenu.add(moveStep);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        tabbedContainer.setTabComponentAt(tabNumber, tabComponent);
        tabbedContainer.setSelectedIndex(tabNumber);
    }

    private void updateTabIndices(){
        for (int i = 1; i < tabbedContainer.getTabCount()-1; i++) {
            CustomTabComponent tab = (CustomTabComponent) tabbedContainer.getTabComponentAt(i);
            tab.setIndex(i); //Since 0 is globals tab
        }
    }

    private void removeTabbedEntry(StepStatePanel stepPanel){
        tabbedContainer.remove(stepPanel);
        if(tabbedContainer.getSelectedIndex() == tabbedContainer.getTabCount()-1){
            //If we're now viewing the "Add Step" tab, view the previous tab instead
            tabbedContainer.setSelectedIndex(tabbedContainer.getSelectedIndex()-1);
        }
        updateTabIndices();
    }

    private void addPanelForStep(StepState step){
        StepStatePanel panel = new StepStatePanel(this, step);
        this.stepToPanelMap.put(step, panel);
        addTabForStep(step, panel);

        this.revalidate();
        this.repaint();
    }

    private void removePanelForStep(StepState step){
        StepStatePanel panel = this.stepToPanelMap.remove(step);
        updateSubsequentPanels(panel); //Update the panels before this one is removed...
        removeTabbedEntry(panel);
        this.revalidate();
        this.repaint();
    }

    public StepStatePanel getPanelForStep(StepState step){
        return this.stepToPanelMap.get(step);
    }

    public void setActivePanel(StepStatePanel stepPanel){
        this.tabbedContainer.setSelectedComponent(stepPanel);
    }

    public void updateSubsequentPanels(StepStatePanel panel){
        int tabIndex = this.tabbedContainer.indexOfComponent(panel) + 1;

        //Loop over panels, not including the Add Step panel
        for (; tabIndex < this.tabbedContainer.getTabCount()-1; tabIndex++) {
            //Loop over panels after the variables origin and update.
            ((StepStatePanel) tabbedContainer.getComponentAt(tabIndex)).refreshRequestPanel();
        }
    }

    private void updateAllPanels(){
        for (Component component : this.tabbedContainer.getComponents()) {
            if(component instanceof StepStatePanel) {
                ((StepStatePanel) component).refreshRequestPanel();
            }
        }
    }

    public StepStatePanel getSelectedStepPanel() {
        Component selectedTab = this.tabbedContainer.getSelectedComponent();
        if(selectedTab instanceof StepStatePanel){
            return (StepStatePanel) selectedTab;
        }
        return null;
    }
}
