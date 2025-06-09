package pcd.ass03.view;

import javax.swing.*;
import java.awt.*;

public class InitialDialog extends JDialog {
    private int numberOfBoids = 0;
    private boolean confirmed = false;

    public InitialDialog(Frame parent) {
        super(parent, "Boids Configuration", true);
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel label = new JLabel("Number of Boids:");
        JTextField boidsField = new JTextField("100");

        inputPanel.add(label);
        inputPanel.add(boidsField);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton startButton = new JButton("Start Simulation");
        JButton cancelButton = new JButton("Cancel");

        startButton.addActionListener(e -> {
            try {
                numberOfBoids = Integer.parseInt(boidsField.getText());
                if (numberOfBoids > 0 && numberOfBoids <= 1000) {
                    confirmed = true;
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Please enter a number between 1 and 1000",
                            "Invalid Input",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Please enter a valid number",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        buttonPanel.add(startButton);
        buttonPanel.add(cancelButton);

        add(inputPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    public boolean showDialog() {
        setVisible(true);
        return confirmed;
    }

    public int getNBoids() {
        return numberOfBoids;
    }
}