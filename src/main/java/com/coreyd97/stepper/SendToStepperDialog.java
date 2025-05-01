package com.coreyd97.stepper;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import com.coreyd97.stepper.sequence.StepSequenceState;
import com.coreyd97.stepper.sequencemanager.SequenceManager;

/**
 * Dialog for the "Send To Stepper" feature.
 * 
 * Prompts the user to either select a sequence to send the currently focused request to
 * or create a completely new sequence based on the current user input and send the
 * request to the new sequence.
 * 
 * The user is presented with a list of all available sequences from which they can select one.
 * With the user input, the user can either search for a sequence or enter the name to create a
 * new sequence.
 * 
 * The dialog tries to be completely usable only using the keyboard.
 */
public class SendToStepperDialog extends JDialog {
    private JList<ListEntry> sequenceList;
    private JTextField searchField;
    private SequenceListModel listModel;
    private Result result;
    private JLabel errorLabel;

    public SendToStepperDialog(Frame frame) {
        super(frame, "Send To Stepper");
    }

    /**
     * List entry class used by the sequence list.
     * 
     * Subclasses implement the different potential entries.
     * There is a subclass for a list entry which represents a sequence and
     * one which represents creating a new sequence.
     */
    public interface ListEntry {}

    /**
     * List entry which represents creating a new sequence.
     */
    public class CreateSequenceListEntry implements ListEntry {
        private String name;

        public CreateSequenceListEntry(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public String toString() {
            return "Create new sequence '" + this.name + "'";
        }
    }

    /**
     * List entry which represents a sequence.
     */
    public class SequenceListEntry implements ListEntry {
        private StepSequenceState sequence;

        public SequenceListEntry(StepSequenceState sequence) {
            this.sequence = sequence;
        }

        public StepSequenceState getSequence() {
            return this.sequence;
        }

        public String toString() {
            return sequence.getTitle();
        }
    }

    /**
     * Custom list model for the sequence list.
     * 
     * The underyling data is mutable and implements a search functionality to
     * filter based on the sequence's name.
     */
    class SequenceListModel implements ListModel<ListEntry> {
        /**
         * Listeners that are notified when the list view changes.
         */
        private Set<ListDataListener> listeners;

        /**
         * Actual list entries.
         */
        private final List<SequenceListEntry> entries;

        /**
         * View of the list.
         * 
         * Depending on the current search, this list may be only a subset of the available entries.
         */
        private List<ListEntry> viewSequences;

        public SequenceListModel(List<StepSequenceState> sequences) {
            this.listeners = new HashSet<>();
            this.entries = sequences.stream().map((StepSequenceState sequence) -> new SequenceListEntry(sequence)).toList();
            this.viewSequences = this.entries.stream().map((entry) -> (ListEntry) entry).toList();
        }

        @Override
        public void addListDataListener(ListDataListener l) {
            this.listeners.add(l);
        }

        @Override
        public ListEntry getElementAt(int index) {
            return viewSequences.get(index);
        }

        @Override
        public int getSize() {
            return this.viewSequences.size();
        }

        @Override
        public void removeListDataListener(ListDataListener l) {
            this.listeners.remove(l);
        }

        /**
         * Filters the list entrie's using the given search.
         * 
         * Only sequences where their name contains the search string will then be showed in the list.
         * @param search The search string
         */
        public void filterWithSearch(String search) {
            if (search == null || search.length() <= 0) {
                this.viewSequences = this.entries.stream().map((SequenceListEntry entry) -> (ListEntry) entry).toList();
            } else {
                this.viewSequences = this.entries.stream()
                    .filter((SequenceListEntry entry) -> entry.getSequence().getTitle().contains(search))
                    .map((SequenceListEntry entry) -> (ListEntry) entry).toList();
            }

            // If no sequence matches the search, insert an entry to create a new sequnce from the search
            if (this.viewSequences.size() == 0) {
                ListEntry createNew = (ListEntry) new CreateSequenceListEntry(search);
                this.viewSequences =  Arrays.asList(new ListEntry[]{createNew});
            }

            ListDataEvent event = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, this.getSize());

            for (ListDataListener listener : this.listeners) {
                listener.contentsChanged(event);
            }
        }
    }

    /**
     * Interface for the results of this dialog.
     * 
     * There are various different results that this dialog can return.
     * Each of them implements this interface.
     */
    public interface Result {}

    /**
     * Result when the user wants to create a new sequence.
     */
    public class CreateNewResult implements Result {
        /**
         * The name the new sequence should have.
         */
        private final String name;

        public CreateNewResult(String name) {
            this.name = name;
        }

        /**
         * Get the name for the new sequence.
         * @return The sequence name
         */
        public String getName() {
            return this.name;
        }
    }

    /**
     * Result for when a request should be sent to a specific sequence.
     */
    public class SendResult implements Result {
        private final StepSequenceState sequence;

        public SendResult(StepSequenceState sequence) {
            this.sequence = sequence;
        }

        /**
         * Get the selected sequence.
         * @return The selected sequence
         */
        public StepSequenceState getSequence() {
            return this.sequence;
        }
    }

    /**
     * Result for when the user cancels the dialog.
     */
    public class CancelResult implements Result {}


    /**
     * Show the dialog to the user and return the user's input.
     * 
     * Depending on the user's selection this function returns on the result implementations.
     * 
     * If the user wants to create a new sequence this function returns {@code CreateNewResult},
     * If the user selects a sequence to send a request to, this  function returns {@code SendResult}.
     * Finally, if the user cancels this prompt, this function returns {@code CancelResult}.
     * @return The result representing the user's input.
     */
    public Result prompt() {
        this.result = new CancelResult();
        
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        JLabel label = new JLabel("Search for a sequence to add this to or create a new sequence");
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.5;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.anchor = GridBagConstraints.PAGE_START;
        panel.add(label, constraints);

        this.searchField =  new JTextField();
        this.searchField.setMaximumSize(new Dimension(Short.MAX_VALUE, 25));
        constraints.gridy = 1;
        panel.add(this.searchField, constraints);

        JLabel footer = new JLabel("Select a sequence:");
        constraints.gridy = 2;
        panel.add(footer, constraints);

        SequenceManager sequenceManager =  Stepper.getSequenceManager();

        this.listModel = new SequenceListModel(sequenceManager.getStepSequenceStates());
        this.sequenceList = new JList<ListEntry>(this.listModel);
        this.sequenceList.setSelectedIndex(0);
        JScrollPane listScroller = new JScrollPane(sequenceList);
        constraints.gridy = 3;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1;
        panel.add(listScroller, constraints);

        this.errorLabel =  new JLabel(" ");
        this.errorLabel.setForeground(UIManager.getColor("ColourPalette.textError"));
        constraints.gridy = 4;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weighty = 0;
        panel.add(this.errorLabel, constraints);

        this.add(panel);
        this.setMinimumSize(new Dimension(400, 300));

        searchField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void changedUpdate(DocumentEvent e) {
                SendToStepperDialog.this.updateListWithSearch();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                SendToStepperDialog.this.updateListWithSearch(); 
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                SendToStepperDialog.this.updateListWithSearch(); 
            }
            
        });

        ActionMap actionMap = panel.getActionMap();
        InputMap inputMap = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        actionMap.put("SelectDown", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sequenceList.setSelectedIndex(sequenceList.getSelectedIndex() + 1);
            }
        });
        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "SelectDown");

        actionMap.put("SelectUp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sequenceList.setSelectedIndex(sequenceList.getSelectedIndex() - 1);
            }
        });
        inputMap.put(KeyStroke.getKeyStroke("UP"), "SelectUp");

        actionMap.put("Send", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ListEntry selectedSequence = SendToStepperDialog.this.sequenceList.getSelectedValue();

                if (selectedSequence instanceof SequenceListEntry) {
                    SendToStepperDialog.this.result = new SendResult(((SequenceListEntry) selectedSequence).getSequence());
                } else {
                    SendToStepperDialog.this.result = new  CreateNewResult(((CreateSequenceListEntry) selectedSequence).getName());
                }

                SendToStepperDialog.this.dispose();
            }
        });
        inputMap.put(KeyStroke.getKeyStroke("ENTER"), "Send");

        actionMap.put("CreateNew", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String name = SendToStepperDialog.this.searchField.getText();

                if (name.length() <= 0) {
                    SendToStepperDialog.this.errorLabel.setText("Error: Enter a name to create a new sequence");
                    return;
                }

                List<StepSequenceState> sequences = Stepper.getSequenceManager().getStepSequenceStates();
                boolean sequenceAlreadyExists = sequences.stream().anyMatch((StepSequenceState sequence) -> sequence.getTitle().equals(name));
                if (sequenceAlreadyExists) {
                    SendToStepperDialog.this.errorLabel.setText("Error: Sequence with this name already exists");
                    return;
                }

                SendToStepperDialog.this.result = new CreateNewResult(name);
                SendToStepperDialog.this.dispose();
            }
        });
        inputMap.put(KeyStroke.getKeyStroke("control ENTER"), "CreateNew");

        actionMap.put("Close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SendToStepperDialog.this.dispose();
            }
        });
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Close");

        this.setModal(true);
        this.setVisible(true);

        return this.result;
    }

    /**
     * Update the list search based on the current search field's text.
     */
    private void updateListWithSearch() {
        this.errorLabel.setText(" ");
        this.listModel.filterWithSearch(this.searchField.getText());
    }
}
