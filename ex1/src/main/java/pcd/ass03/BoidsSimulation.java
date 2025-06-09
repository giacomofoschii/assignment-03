package pcd.ass03;

import akka.actor.typed.ActorSystem;
import pcd.ass03.actors.ManagerActor;
import pcd.ass03.protocols.ManagerProtocol;

public class BoidsSimulation {
    public static void main(String[] args) {
        ActorSystem<ManagerProtocol.Command> system =
                ActorSystem.create(ManagerActor.create(), "boids-system");

        system.tell(new ManagerProtocol.StartSimulation(2000, 800, 800));
    }
}
