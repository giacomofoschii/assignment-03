package pcd.ass03.actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import pcd.ass03.BoidsView;
import pcd.ass03.protocols.GUIProtocol;

import javax.swing.SwingUtilities;

public class GUIActor {

    private final BoidsView view;

    private GUIActor(BoidsView view) {
        this.view = view;
    }

    public static Behavior<GUIProtocol.Command> create(BoidsView view) {
        return Behaviors.setup(ctx -> new GUIActor(view).behavior());
    }

    private Behavior<GUIProtocol.Command> behavior() {
        return Behaviors.receive(GUIProtocol.Command.class)
            .onMessage(GUIProtocol.RenderFrame.class, this::onRenderFrame)
            .build();
    }

    private Behavior<GUIProtocol.Command> onRenderFrame(GUIProtocol.RenderFrame msg) {
        SwingUtilities.invokeLater(() ->
            view.update(msg.metrics().fps()));
        return Behaviors.same();
    }
}
