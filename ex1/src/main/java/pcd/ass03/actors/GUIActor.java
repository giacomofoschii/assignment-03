package pcd.ass03.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import pcd.ass03.protocols.ManagerProtocol;
import pcd.ass03.view.BoidsPanel;
import pcd.ass03.protocols.GUIProtocol;
import pcd.ass03.view.InitialDialog;

import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import java.awt.*;
import java.util.Hashtable;

public class GUIActor implements ChangeListener {
    public static final int SCREEN_WIDTH = 800, SCREEN_HEIGHT = 800;

    private final ActorRef<ManagerProtocol.Command> managerActor;
    private final BoidsParams boidsParams;

    private JFrame frame;
    private BoidsPanel boidsPanel;
    private JSlider cohesionSlider, separationSlider, alignmentSlider;
    private JButton resumeButton, pauseButton, stopButton;
    private JLabel statusLabel;
    private boolean isPaused = false;
    private boolean isRunning = false;

    private GUIActor(BoidsParams boidsParams,
                     ActorRef<ManagerProtocol.Command> managerActor) {
        this.boidsParams = boidsParams;
        this.managerActor = managerActor;

        SwingUtilities.invokeLater(this::showInitialDialog);
    }

    private void showInitialDialog() {
        // Create temporary frame for dialog parent
        JFrame parentFrame = (frame != null) ? frame : new JFrame();
        if (frame == null) {
            parentFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }

        InitialDialog dialog = new InitialDialog(parentFrame);
        if (dialog.showDialog()) {
            int nBoids = dialog.getNBoids();

            if(frame == null) {
                parentFrame.dispose();
            }

            // Create main GUI
            if (frame == null) {
                this.createMainGUI(nBoids);
            } else {
                updateGUIForRestart(nBoids);
            }


            // Tell the manager to start simulation
            managerActor.tell(new ManagerProtocol.StartSimulation(
                    nBoids,
                    boidsParams.getWidth(),
                    boidsParams.getHeight()
            ));

            isRunning = true;
            isPaused = false;
            updateButtonStates();
            statusLabel.setText("Status: Running");
        } else {
            if (frame == null) {
                System.exit(0);
            }
        }
    }

    private void createMainGUI(int nBoids) {
        final int envWidth = (int) Math.round(boidsParams.getWidth());
        final int envHeight = (int) Math.round(boidsParams.getHeight());

        frame = new JFrame("Boids Simulation");
        frame.setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());

        // Boids panel
        this.boidsPanel = new BoidsPanel(envWidth, envHeight, nBoids, new ArrayList<>());
        mainPanel.add(BorderLayout.CENTER, boidsPanel);

        JPanel cpTop = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        cpTop.setBorder(BorderFactory.createTitledBorder("Simulation Controls"));

        resumeButton = new JButton("Resume");
        pauseButton = new JButton("Pause");
        stopButton = new JButton("Stop");
        statusLabel = new JLabel("Status: Starting...");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));

        // Add action listeners
        resumeButton.addActionListener(e -> onPauseResume());
        pauseButton.addActionListener(e -> onPauseResume());
        stopButton.addActionListener(e -> onStop());

        cpTop.add(resumeButton);
        cpTop.add(pauseButton);
        cpTop.add(stopButton);
        cpTop.add(Box.createHorizontalStrut(20));
        cpTop.add(statusLabel);

        // Sliders panel at bottom
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel slidersPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        slidersPanel.setBorder(BorderFactory.createTitledBorder("Parameters"));

        this.cohesionSlider = makeSlider();
        this.separationSlider = makeSlider();
        this.alignmentSlider = makeSlider();

        slidersPanel.add(new JLabel("  Cohesion:"));
        slidersPanel.add(cohesionSlider);
        slidersPanel.add(new JLabel("  Separation:"));
        slidersPanel.add(separationSlider);
        slidersPanel.add(new JLabel("  Alignment:"));
        slidersPanel.add(alignmentSlider);

        bottomPanel.add(cpTop, BorderLayout.NORTH);
        bottomPanel.add(slidersPanel, BorderLayout.CENTER);

        mainPanel.add(BorderLayout.SOUTH, bottomPanel);

        frame.setContentPane(mainPanel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        updateButtonStates();
    }

    private void updateGUIForRestart(int nBoids) {
        if(boidsPanel != null) {
            boidsPanel.setNBoids(nBoids);
            boidsPanel.updateBoids(new ArrayList<>());
            boidsPanel.repaint();
        }
    }

    private void updateButtonStates() {
        if (this.pauseButton != null
                && this.resumeButton != null
                && this.stopButton != null) {
            if (!isRunning) {
                this.pauseButton.setEnabled(false);
                this.resumeButton.setEnabled(false);
                this.stopButton.setEnabled(false);
            } else if (isPaused) {
                this.pauseButton.setEnabled(false);
                this.resumeButton.setEnabled(true);
                this.stopButton.setEnabled(true);
            } else {
                this.pauseButton.setEnabled(true);
                this.resumeButton.setEnabled(false);
                this.stopButton.setEnabled(true);
            }
        }
    }

    private void onStop() {
        if (isRunning) {
            managerActor.tell(new ManagerProtocol.StopSimulation());
            updateButtonStates();
            statusLabel.setText("Status: Stopped");
        }
    }

    private void onPauseResume() {
        if(isRunning) {
            if (isPaused) {
                managerActor.tell(new ManagerProtocol.ResumeSimulation());
                isPaused = false;
                statusLabel.setText("Status: Paused");
            } else {
                managerActor.tell(new ManagerProtocol.PauseSimulation());
                isPaused = true;
                statusLabel.setText("Status: Running - Resumed");
            }
            updateButtonStates();
        }
    }

    public static Behavior<GUIProtocol.Command> create(BoidsParams boidsParams,
                                                       ActorRef<ManagerProtocol.Command> managerActor) {
        return Behaviors.setup(ctx -> new GUIActor(boidsParams, managerActor).behavior());
    }

    private Behavior<GUIProtocol.Command> behavior() {
        return Behaviors.receive(GUIProtocol.Command.class)
            .onMessage(GUIProtocol.RenderFrame.class, this::onRenderFrame)
            .onMessage(GUIProtocol.UpdateWeights.class, this::onUpdateWeights)
            .build();
    }

    private Behavior<GUIProtocol.Command> onRenderFrame(GUIProtocol.RenderFrame msg) {
        SwingUtilities.invokeLater(() -> {
            if (boidsPanel != null) {
                this.boidsPanel.updateBoids(msg.boids());
                this.boidsPanel.setFrameRate(msg.metrics().fps());
                this.boidsPanel.repaint();
            }
        });
        return Behaviors.same();
    }

    private Behavior<GUIProtocol.Command> onUpdateWeights(GUIProtocol.UpdateWeights msg) {
        this.boidsParams.setSeparationWeight(msg.separationWeight());
        this.boidsParams.setCohesionWeight(msg.cohesionWeight());
        this.boidsParams.setAlignmentWeight(msg.alignmentWeight());
        return Behaviors.same();
    }

    private JSlider makeSlider() {
        var slider = new JSlider(JSlider.HORIZONTAL, 0, 20, 10);
        slider.setMajorTickSpacing(100);
        slider.setMinorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);

        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        slider.setLabelTable(labelTable);
        slider.addChangeListener(this);
        return slider;
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        double sep = separationSlider.getValue() * 0.1;
        double coh = cohesionSlider.getValue() * 0.1;
        double ali = alignmentSlider.getValue() * 0.1;
        managerActor.tell(new ManagerProtocol.UpdateParams(coh, ali, sep));
    }
}
