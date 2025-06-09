package pcd.ass03.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import pcd.ass03.model.BoidState;
import pcd.ass03.protocols.ManagerProtocol;
import pcd.ass03.view.BoidsPanel;
import pcd.ass03.protocols.GUIProtocol;

import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import java.awt.*;
import java.util.Hashtable;

public class GUIActor implements ChangeListener {
    public static final int SCREEN_WIDTH = 800, SCREEN_HEIGHT = 800;

    private final BoidsPanel boidsPanel;
    private final JSlider cohesionSlider, separationSlider, alignmentSlider;
    private final BoidsParams boidsParams;
    private final ActorRef<ManagerProtocol.Command> managerActor;

    private GUIActor(BoidsParams boidsParams, double width, double height, Map<String, BoidState> boids,
                     ActorRef<ManagerProtocol.Command> managerActor) {
        this.boidsParams = boidsParams;
        this.managerActor = managerActor;
        int width1 = (int) Math.round(width);
        int height1 = (int) Math.round(height);

        // Initialize the GUI components
        JFrame frame = new JFrame("Boids Simulation");
        frame.setSize(width1, height1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel cp = new JPanel(new BorderLayout());
        this.boidsPanel = new BoidsPanel(width1, height1, boids.size(), boids.values().stream().toList());
        cp.add(BorderLayout.CENTER, boidsPanel);

        JPanel slidersPanel = new JPanel(new GridLayout(3, 1));
        this.cohesionSlider = makeSlider();
        this.separationSlider = makeSlider();
        this.alignmentSlider = makeSlider();

        slidersPanel.add(new JLabel("Cohesion"));
        slidersPanel.add(cohesionSlider);
        slidersPanel.add(new JLabel("Separation"));
        slidersPanel.add(separationSlider);
        slidersPanel.add(new JLabel("Alignment"));
        slidersPanel.add(alignmentSlider);

        cp.add(BorderLayout.SOUTH, slidersPanel);
        frame.setContentPane(cp);
        frame.setVisible(true);
    }

    public static Behavior<GUIProtocol.Command> create(BoidsParams boidsParams, double width, double height,
                                                       Map<String, BoidState> boids,
                                                       ActorRef<ManagerProtocol.Command> managerActor) {
        return Behaviors.setup(ctx -> new GUIActor(boidsParams, width, height, boids, managerActor).behavior());
    }

    private Behavior<GUIProtocol.Command> behavior() {
        return Behaviors.receive(GUIProtocol.Command.class)
            .onMessage(GUIProtocol.RenderFrame.class, this::onRenderFrame)
            .onMessage(GUIProtocol.UpdateWeights.class, this::onUpdateWeights)
            .build();
    }

    private Behavior<GUIProtocol.Command> onRenderFrame(GUIProtocol.RenderFrame msg) {
        SwingUtilities.invokeLater(() -> {
            this.boidsPanel.setFrameRate(msg.metrics().fps());
            this.boidsPanel.repaint();
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
        slider.setMajorTickSpacing(10);
        slider.setMinorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("0"));
        labelTable.put(1, new JLabel("1"));
        labelTable.put(2, new JLabel("2"));
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
