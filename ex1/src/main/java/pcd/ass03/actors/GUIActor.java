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
    private final ActorRef<ManagerProtocol.Command> managerActor;
    private final BoidsParams boidsParams;

    private JFrame frame;
    private BoidsPanel boidsPanel;
    private JSlider cohesionSlider, separationSlider, alignmentSlider;
    private JButton resumeButton, pauseButton, stopButton;
    private JLabel statusLabel;

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

            SwingUtilities.invokeLater(() -> statusLabel.setText("Status: " + GUIProtocol.SimulationStatus.STARTING.getDisplayText()));

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
        frame.setSize((int) Math.round(boidsParams.getWidth()),(int) Math.round(boidsParams.getHeight()));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());

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
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        resumeButton.addActionListener(e -> onPauseResume());
        pauseButton.addActionListener(e -> onPauseResume());
        stopButton.addActionListener(e -> onStop());

        cpTop.add(resumeButton);
        cpTop.add(pauseButton);
        cpTop.add(stopButton);
        cpTop.add(Box.createHorizontalStrut(10));
        cpTop.add(statusLabel);

        mainPanel.add(BorderLayout.NORTH, cpTop);

        JPanel slidersPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
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

        updateButtonStates(true, false, true);
    }

    private void updateGUIForRestart(int nBoids) {
        if(boidsPanel != null) {
            boidsPanel.setNBoids(nBoids);
            boidsPanel.updateBoids(new ArrayList<>());
            boidsPanel.repaint();
        }
    }

    public static Behavior<GUIProtocol.Command> create(BoidsParams boidsParams,
                                                       ActorRef<ManagerProtocol.Command> managerActor) {
        return Behaviors.setup(ctx ->
                Behaviors.supervise(new GUIActor(boidsParams, managerActor).behavior())
                        .onFailure(SupervisorStrategy.restart()));
    }

    private Behavior<GUIProtocol.Command> behavior() {
        return waitingForStart();
    }

    private Behavior<GUIProtocol.Command> waitingForStart() {
        return Behaviors.receive(GUIProtocol.Command.class)
                .onMessage(GUIProtocol.UpdateStatus.class, msg -> {
                    if(msg.status() == GUIProtocol.SimulationStatus.RUNNING) {
                        updateButtonStates(true, false, true);
                        return running();
                    }
                    return Behaviors.same();
                })
                .onMessage(GUIProtocol.UpdateWeights.class, this::onUpdateWeights)
                .onMessage(GUIProtocol.RenderFrame.class, this::onRenderFrame)
                .build();
    }

    private Behavior<GUIProtocol.Command> running() {
        return Behaviors.receive(GUIProtocol.Command.class)
                .onMessage(GUIProtocol.RenderFrame.class, this::onRenderFrame)
                .onMessage(GUIProtocol.UpdateWeights.class, this::onUpdateWeights)
                .onMessage(GUIProtocol.UpdateStatus.class, this::onUpdateStatus)
                .onMessage(GUIProtocol.ConfirmPause.class, msg -> {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Status: " + GUIProtocol.SimulationStatus.PAUSED.getDisplayText());
                        updateButtonStates(false, true, true);
                    });
                    return paused();
                })
                .onMessage(GUIProtocol.ConfirmStop.class, msg -> {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Status: " + GUIProtocol.SimulationStatus.STOPPED.getDisplayText());
                        frame.dispose();
                    });
                    return stopped();
                })
                .onMessage(GUIProtocol.ConfirmParamsUpdate.class, this::onConfirmParamsUpdate)
                .build();
    }

    private Behavior<GUIProtocol.Command> paused() {
        return Behaviors.receive(GUIProtocol.Command.class)
                .onMessage(GUIProtocol.UpdateWeights.class, this::onUpdateWeights)
                .onMessage(GUIProtocol.ConfirmResume.class, msg -> {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Status: " + GUIProtocol.SimulationStatus.RESUMED.getDisplayText());
                        updateButtonStates(true, false, true);
                    });
                    return running();
                })
                .onMessage(GUIProtocol.ConfirmStop.class, msg -> {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Status: " + GUIProtocol.SimulationStatus.STOPPED.getDisplayText());
                        frame.dispose();
                    });
                    return stopped();
                })
                .onMessage(GUIProtocol.ConfirmParamsUpdate.class, this::onConfirmParamsUpdate)
                .build();
    }

    private Behavior<GUIProtocol.Command> stopped() {
        return Behaviors.stopped();
    }

    private void updateButtonStates(boolean pauseEnabled, boolean resumeEnabled, boolean stopEnabled) {
        SwingUtilities.invokeLater(() -> {
            if (this.pauseButton != null && this.resumeButton != null && this.stopButton != null) {
                this.pauseButton.setEnabled(pauseEnabled);
                this.resumeButton.setEnabled(resumeEnabled);
                this.stopButton.setEnabled(stopEnabled);
            }
        });
    }

    private void onStop() {
        updateButtonStates(false, false, false);
        managerActor.tell(new ManagerProtocol.StopSimulation());
    }

    private void onPauseResume() {
        if(pauseButton.isEnabled()) {
            managerActor.tell(new ManagerProtocol.PauseSimulation());
        } else if (resumeButton.isEnabled()) {
            managerActor.tell(new ManagerProtocol.ResumeSimulation());
        }
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

        SwingUtilities.invokeLater(() ->
                updateSlidersWithoutTriggering(
                    msg.separationWeight(),
                    msg.alignmentWeight(),
                    msg.cohesionWeight()
            )
        );

        return Behaviors.same();
    }

    private Behavior<GUIProtocol.Command> onUpdateStatus(GUIProtocol.UpdateStatus msg) {
        SwingUtilities.invokeLater(() -> statusLabel.setText("Status: " + msg.status().getDisplayText()));
        return Behaviors.same();
    }

    private Behavior<GUIProtocol.Command> onConfirmPause(GUIProtocol.ConfirmPause msg) {
        SwingUtilities.invokeLater(() -> statusLabel.setText("Status: " + GUIProtocol.SimulationStatus.PAUSED.getDisplayText()));
        updateButtonStates(false, true, true);
        return Behaviors.same();
    }

    private Behavior<GUIProtocol.Command> onConfirmResume (GUIProtocol.ConfirmResume msg) {
        SwingUtilities.invokeLater(() -> statusLabel.setText("Status: " + GUIProtocol.SimulationStatus.RESUMED.getDisplayText()));
        updateButtonStates(true, false, true);
        return Behaviors.same();
    }

    private Behavior<GUIProtocol.Command> onConfirmStop(GUIProtocol.Command msg) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Status: " + GUIProtocol.SimulationStatus.STOPPED.getDisplayText());
            frame.dispose();
        });
        updateButtonStates(false, false, false);
        return Behaviors.same();
    }

    private Behavior<GUIProtocol.Command> onConfirmParamsUpdate(GUIProtocol.ConfirmParamsUpdate msg) {
        SwingUtilities.invokeLater(() -> {
            cohesionSlider.setEnabled(true);
            separationSlider.setEnabled(true);
            alignmentSlider.setEnabled(true);
        });
        return Behaviors.same();
    }

    private void updateSlidersWithoutTriggering(double sep, double ali, double coh) {
        if (separationSlider != null && alignmentSlider != null && cohesionSlider != null) {
            separationSlider.removeChangeListener(this);
            alignmentSlider.removeChangeListener(this);
            cohesionSlider.removeChangeListener(this);

            separationSlider.setValue((int)(sep * 10));
            alignmentSlider.setValue((int)(ali * 10));
            cohesionSlider.setValue((int)(coh * 10));

            separationSlider.addChangeListener(this);
            alignmentSlider.addChangeListener(this);
            cohesionSlider.addChangeListener(this);
        }
    }

    private JSlider makeSlider() {
        var slider = new JSlider(JSlider.HORIZONTAL, 0, 20, 10);
        slider.setMajorTickSpacing(10);
        slider.setMinorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);

        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel(("")));
        labelTable.put(10, new JLabel(""));
        slider.setLabelTable(labelTable);
        
        slider.addChangeListener(this);
        return slider;
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        double sep = separationSlider.getValue() * 0.1;
        double coh = cohesionSlider.getValue() * 0.1;
        double ali = alignmentSlider.getValue() * 0.1;

        SwingUtilities.invokeLater(() -> {
            cohesionSlider.setEnabled(false);
            separationSlider.setEnabled(false);
            alignmentSlider.setEnabled(false);
        });

        managerActor.tell(new ManagerProtocol.UpdateParams(coh, ali, sep));
    }
}
