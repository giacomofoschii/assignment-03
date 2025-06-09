package pcd.ass03;

import akka.actor.typed.ActorSystem;
import pcd.ass03.actors.ManagerActor;
import pcd.ass03.protocols.ManagerProtocol;

public class BoidsSimulation {
    public static void main(String[] args) {
        ActorSystem<ManagerProtocol.Command> system =
                ActorSystem.create(ManagerActor.create(), "boids-system");

        Runtime.getRuntime().addShutdownHook(new Thread(system::terminate));
    }
}
