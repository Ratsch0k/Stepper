package com.coreyd97.stepper.preferences.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import com.coreyd97.stepper.Stepper;
import com.coreyd97.stepper.hotkey.HotKeyManager;

/**
 * Dialog to let a user configure a hotkey.
 * 
 * The user can enter a hotkey by simply pressing the desired key combination.
 * If this combination is not used yet as a hotkey, the user can then apply it.
 * Otherwise they are shown an error.
 */
public class HotKeyDialog extends JDialog {
    Result input;

    /**
     * Interface for the possible result values.
     * 
     * Results of this dialog implement this interface.
     */
    public interface Result {}

    /**
     * Result if the user cancels the dialog.
     */
    public class CancelResult implements Result {}

    /**
     * Result if the user applies a hotkey.
     */
    public class ApplyResult implements Result {
        private String value;

        public ApplyResult(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }
    }

    public HotKeyDialog() {
        super(Stepper.montoyaApi.userInterface().swingUtils().suiteFrame(), "Configure Hotkey");
        this.input = new CancelResult();
    }

    /**
     * Promps the user to configure a hotkey.
     * @return The user's input as a result
     */
    public Result prompt() {
        this.setModal(true);
        this.setMinimumSize(new Dimension(400, 200));
        this.setResizable(false);

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.weighty = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.insets = new Insets(10, 20, 5, 20);
        c.anchor = GridBagConstraints.PAGE_START;
        c.fill = GridBagConstraints.HORIZONTAL;

        panel.add(new JLabel("Enter a new hotkey"), c);

        JTextField textField = new JTextField();
        textField.setEditable(false);

        c.gridy = 1;
        c.insets = new Insets(5, 20, 5, 20);
        c.anchor = GridBagConstraints.PAGE_START;
        panel.add(textField, c);

        JLabel errorLabel = new JLabel(" ");
        errorLabel.setForeground(UIManager.getColor("ColourPalette.textError"));
        errorLabel.setVerticalAlignment(SwingConstants.TOP);
        c.gridy = 2;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(errorLabel, c);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton();
        cancelButton.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                HotKeyDialog.this.setVisible(false);
                HotKeyDialog.this.dispose();
            }
        });
        cancelButton.setText("Cancel");
        buttonPanel.add(cancelButton);

        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));

        JButton okButton = new JButton();
        Color primaryBackground = UIManager.getColor("Burp.primaryButtonBackground");
        Color primaryForeground =  UIManager.getColor("Burp.primaryButtonForeground");
        okButton.setBackground(primaryBackground);
        okButton.setForeground(primaryForeground);
        okButton.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String newKeyStroke = textField.getText();

                HotKeyDialog.this.input = new ApplyResult(newKeyStroke);

                HotKeyDialog.this.setVisible(false);
                HotKeyDialog.this.dispose();
            }
        });
        okButton.setText("Apply");
        Action okAction = okButton.getAction();
        okAction.setEnabled(false);
        buttonPanel.add(okButton);

        c.gridy = 3;
        c.weighty = 0;
        c.anchor = GridBagConstraints.LAST_LINE_END;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 20, 10, 20);
        panel.add(buttonPanel, c);

        textField.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                int anyMask = KeyEvent.SHIFT_DOWN_MASK | KeyEvent.ALT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK;

                if (e.getKeyCode() > 65 && (e.getModifiersEx() & anyMask) > 0) {
                    String keyStroke = KeyEvent.getModifiersExText(e.getModifiersEx()) + "+" + KeyEvent.getKeyText(e.getKeyCode());

                    try {
                        boolean alreadyUsed = HotKeyManager.getInstance().isHotKeyUsed(keyStroke);
                        if (alreadyUsed) {
                            errorLabel.setText("Hotkey already assigned");
                            textField.setText("");
                            okAction.setEnabled(false);
                        } else {
                            errorLabel.setText(" ");
                            textField.setText(keyStroke);
                            okAction.setEnabled(true);
                        }
                    } catch (Exception err) {
                        Stepper.callbacks.printError(err.getMessage());
                    }

                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }
        });
        
        InputMap inputMap = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = panel.getActionMap();

        actionMap.put("Cancel", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                cancelButton.doClick();;
            }
        });
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Cancel");

        actionMap.put("Apply", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (okButton.isEnabled()) {
                    okButton.doClick();
                }
            }
        });
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Apply");

        this.add(panel);

        this.setVisible(true);

        return this.input;
    }
}
