package pcd.ass03.actors;

import akka.actor.typed.*;
import akka.actor.typed.javadsl.*;
import pcd.ass03.protocols.*;
import pcd.ass03.view.*;

import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import java.awt.*;

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

        JPanel cpTop = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        cpTop.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        cpTop.setBorder(BorderFactory.createTitledBorder("Simulation Controls"));

        Dimension buttonSize = new Dimension(85, 25);
        resumeButton = new JButton("Resume");
        resumeButton.setPreferredSize(buttonSize);
        pauseButton = new JButton("Pause");
        pauseButton.setPreferredSize(buttonSize);
        stopButton = new JButton("Stop");
        stopButton.setPreferredSize(buttonSize);

        statusLabel = new JLabel("Status: Starting...");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));

        // Add action listeners
        resumeButton.addActionListener(e -> onPauseResume());
        pauseButton.addActionListener(e -> onPauseResume());
        stopButton.addActionListener(e -> onStop());

        cpTop.add(resumeButton);
        cpTop.add(pauseButton);
        cpTop.add(stopButton);
        cpTop.add(Box.createHorizontalStrut(10));
        cpTop.add(statusLabel);

        mainPanel.add(BorderLayout.NORTH, cpTop);

        // Sliders panel at bottom
        JPanel slidersPanel = new JPanel(new GridLayout(3, 2, 3, 3));
        slidersPanel.setBorder(BorderFactory.createTitledBorder("Parameters"));

        Dimension sliderSize = new Dimension(100, 30);
        cohesionSlider = makeSlider();
        cohesionSlider.setPreferredSize(sliderSize);
        separationSlider = makeSlider();
        separationSlider.setPreferredSize(sliderSize);
        alignmentSlider = makeSlider();
        alignmentSlider.setPreferredSize(sliderSize);

        slidersPanel.add(new JLabel("  Cohesion:"));
        slidersPanel.add(cohesionSlider);
        slidersPanel.add(new JLabel("  Separation:"));
        slidersPanel.add(separationSlider);
        slidersPanel.add(new JLabel("  Alignment:"));
        slidersPanel.add(alignmentSlider);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        bottomPanel.add(slidersPanel);

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
            isRunning = false;
            isPaused = false;
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
                statusLabel.setText("Status: Running - Resumed");
            } else {
                managerActor.tell(new ManagerProtocol.PauseSimulation());
                isPaused = true;
                statusLabel.setText("Status: Paused");
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
                .onMessage(GUIProtocol.ShowInitialDialog.class, this::onShowInitialDialog)
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

    private Behavior<GUIProtocol.Command> onShowInitialDialog(GUIProtocol.ShowInitialDialog msg) {
        SwingUtilities.invokeLater(this::showInitialDialog);
        return Behaviors.same();
    }

    private JSlider makeSlider() {
        var slider = new JSlider(JSlider.HORIZONTAL, 0, 20, 10);
        slider.setMajorTickSpacing(100);
        slider.setMinorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);

        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel(("")));
        labelTable.put(20, new JLabel(""));
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
