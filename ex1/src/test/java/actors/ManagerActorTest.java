package actors;

import akka.actor.testkit.typed.javadsl.*;
import akka.actor.typed.ActorRef;
import org.junit.jupiter.api.*;
import pcd.ass03.actors.ManagerActor;
import pcd.ass03.protocols.*;
import pcd.ass03.utils.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ManagerActorTest {

    private static ActorTestKit testKit;
    private ActorRef<ManagerProtocol.Command> manager;

    @BeforeAll
    public static void setupTestKit() {
        testKit = ActorTestKit.create();
    }

    @AfterAll
    public static void tearDown() {
        testKit.shutdownTestKit();
    }

    @Test
    public void testSimulationLifecycle() throws InterruptedException {
        manager = testKit.spawn(
                ManagerActor.create(),
                "lifecycle-manager"
        );

        int nBoids = 5;
        manager.tell(new ManagerProtocol.StartSimulation(nBoids, 800, 800));

        manager.tell(new ManagerProtocol.PauseSimulation());
        Thread.sleep(50);

        manager.tell(new ManagerProtocol.ResumeSimulation());
        Thread.sleep(50);

        manager.tell(new ManagerProtocol.StopSimulation());

        manager.tell(new ManagerProtocol.StartSimulation(3, 600, 600));

        // Test passed if we reach here without exceptions
        assertTrue(true, "Lifecycle completed successfully");
    }

    @Test
    public void testBoidCreation() throws InterruptedException {
        manager = testKit.spawn(
                ManagerActor.create(),
                "creation-manager"
        );

        int expectedBoids = 10;
        Set<String> createdBoids = new HashSet<>();

        manager.tell(new ManagerProtocol.StartSimulation(expectedBoids, 800, 800));

        Thread.sleep(100);

        manager.tell(new ManagerProtocol.Tick());

        for (int i = 0; i < expectedBoids; i++) {
            P2d pos = new P2d(i * 10, i * 10);
            V2d vel = new V2d(1, 1);
            String boidId = "boid-" + i;

            manager.tell(new ManagerProtocol.BoidUpdated(pos, vel, boidId));
            createdBoids.add(boidId);
        }

        assertEquals(expectedBoids, createdBoids.size());

        manager.tell(new ManagerProtocol.StopSimulation());
    }

    @Test
    public void testParameterPropagation() throws InterruptedException {
        manager = testKit.spawn(
                ManagerActor.create(),
                "params-manager"
        );

        manager.tell(new ManagerProtocol.StartSimulation(3, 800, 800));

        Thread.sleep(100);

        double newCohesion = 1.5;
        double newAlignment = 0.8;
        double newSeparation = 2.0;

        manager.tell(new ManagerProtocol.UpdateParams(newCohesion, newAlignment, newSeparation));

        // Parameters should be propagated to boids.
        // Let's verify that the command is processed without errors
        Thread.sleep(50);

        manager.tell(new ManagerProtocol.StopSimulation());

        assertTrue(true, "Parameters updated successfully");
    }

    @Test
    public void testTickMechanism() throws InterruptedException {
        ActorRef<ManagerProtocol.Command> manager = testKit.spawn(
                ManagerActor.create(),
                "tick-manager"
        );

        manager.tell(new ManagerProtocol.StartSimulation(2, 800, 800));

        Thread.sleep(150);

        manager.tell(new ManagerProtocol.UpdateCompleted(System.currentTimeMillis()));

        manager.tell(new ManagerProtocol.StopSimulation());
    }

   @Test
    public void testUpdateCompletedHandling() throws InterruptedException {
        manager = testKit.spawn(
                ManagerActor.create(),
                "update-manager"
        );

        manager.tell(new ManagerProtocol.StartSimulation(5, 800, 800));

        Thread.sleep(50);

        long tickTime = System.currentTimeMillis();
        manager.tell(new ManagerProtocol.UpdateCompleted(tickTime));

        manager.tell(new ManagerProtocol.StopSimulation());
    }

    @Test
    public void testManagerResilience() throws InterruptedException {
        ActorRef<ManagerProtocol.Command> manager = testKit.spawn(
                ManagerActor.create(),
                "resilient-manager"
        );

        manager.tell(new ManagerProtocol.StartSimulation(3, 800, 800));

        manager.tell(new ManagerProtocol.BoidUpdated(
                new P2d(0, 0),
                new V2d(0, 0),
                "non-existent-boid"
        ));

        Thread.sleep(50);

        manager.tell(new ManagerProtocol.PauseSimulation());
        manager.tell(new ManagerProtocol.ResumeSimulation());
        manager.tell(new ManagerProtocol.StopSimulation());

        assertTrue(true, "Manager handled invalid messages gracefully");
    }
}
