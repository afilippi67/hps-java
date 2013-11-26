package org.hps.monitoring;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * This is the panel for controlling the application when it is in pause mode,
 * e.g. to step to the next event, etc.
 */
public class EventButtonsPanel extends JPanel {

    JButton nextEventsButton;
    JButton pauseButton;
    JButton connectButton;

    /**
     * Class constructor.
     */
    EventButtonsPanel() {

        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 0, 10);
        connectButton = new JButton("Connect");
        connectButton.setEnabled(true);
        connectButton.setActionCommand(MonitoringCommands.connectCmd);
        add(connectButton, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 0, 10);
        pauseButton = new JButton("Pause");
        pauseButton.setActionCommand(MonitoringCommands.pauseCmd);
        pauseButton.setEnabled(false);
        add(pauseButton, c);

        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 0;
        nextEventsButton = new JButton("Next Event");
        nextEventsButton.setEnabled(false);
        nextEventsButton.setActionCommand(MonitoringCommands.nextCmd);
        add(nextEventsButton, c);
    }

    /**
     * Toggle the connect button from its current state.  If it is in "Connect" state
     * then it will be toggled to "Disconnect" and vice versa.
     */
    void toggleConnectButton() {
        if (connectButton.getText().equals("Connect")) {
            connectButton.setText("Disconnect");
            connectButton.setActionCommand(MonitoringCommands.disconnectCmd);
        } else {
            connectButton.setText("Connect");
            connectButton.setActionCommand(MonitoringCommands.connectCmd);
        }
    }

    /**
     * Add an ActionListener to this component.
     * @param listener The ActionListener.
     */
    void addActionListener(ActionListener listener) {
        nextEventsButton.addActionListener(listener);
        pauseButton.addActionListener(listener);
        connectButton.addActionListener(listener);
    }

    /**
     * Eanble the pause button.
     * @param e Set to true to enable the pause button; false to disable.
     */
    void enablePauseButton(boolean e) {
        this.pauseButton.setEnabled(e);
    }

    /**
     * Enable the "next events" button.  
     * @param e Set to true to enable; false to disable.
     */
    void enableNextEventsButton(boolean e) {
        this.nextEventsButton.setEnabled(e);
    }

    /**
     * Set the pause mode state.
     * @param enable Set to true to enable pause mode; false to disable.
     */
    void setPauseModeState(boolean enable) {
        this.nextEventsButton.setEnabled(enable);
        if (enable) {
            pauseButton.setText("Resume");
            pauseButton.setActionCommand(MonitoringCommands.resumeCmd);
        } else {
            pauseButton.setText("Pause");
            pauseButton.setActionCommand(MonitoringCommands.pauseCmd);
        }
    }
}