package actors;

import akka.actor.testkit.typed.javadsl.*;
import akka.actor.typed.ActorRef;
import org.junit.jupiter.api.*;
import pcd.ass03.actors.BarrierActor;
import pcd.ass03.protocols.*;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class BarrierActorTest {

    private static ActorTestKit testKit;
    private TestProbe<ManagerProtocol.Command> managerProbe;
    private ActorRef<BarrierProtocol.Command> barrier;

    @BeforeAll
    public static void setupTestKit() {
        testKit = ActorTestKit.create();
    }

    @AfterAll
    public static void tearDown() {
        testKit.shutdownTestKit();
    }

    @Test
    public void testBarrierSynchronization() {
        managerProbe = testKit.createTestProbe();

        int numBoids = 3;
        barrier = testKit.spawn(
            BarrierActor.create(numBoids, managerProbe.getRef()),
            "test-barrier"
        );

        barrier.tell(new BarrierProtocol.StartPhase(1L));

        barrier.tell(new BarrierProtocol.BoidCompleted("boid-1", 1L));
        barrier.tell(new BarrierProtocol.BoidCompleted("boid-2", 1L));

        managerProbe.expectNoMessage(Duration.ofMillis(100));

        barrier.tell(new BarrierProtocol.BoidCompleted("boid-3", 1L));

        ManagerProtocol.UpdateCompleted completion = managerProbe.expectMessageClass(
            ManagerProtocol.UpdateCompleted.class
        );
        assertEquals(1L, completion.tick());
    }

    @Test
    public void testBarrierResetBetweenPhases() {
        managerProbe = testKit.createTestProbe();

        int nBoids = 2;
        barrier = testKit.spawn(
                BarrierActor.create(nBoids, managerProbe.ref()),
                "reset-test-barrier"
        );

        barrier.tell(new BarrierProtocol.StartPhase(1L));
        barrier.tell(new BarrierProtocol.BoidCompleted("boid-1", 1L));
        barrier.tell(new BarrierProtocol.BoidCompleted("boid-2", 1L));

        managerProbe.expectMessageClass(ManagerProtocol.UpdateCompleted.class);

        barrier.tell(new BarrierProtocol.StartPhase(2L));
        barrier.tell(new BarrierProtocol.BoidCompleted("boid-1", 2L));

        managerProbe.expectNoMessage(Duration.ofMillis(100));

        barrier.tell(new BarrierProtocol.BoidCompleted("boid-2", 2L));

        ManagerProtocol.UpdateCompleted completion = managerProbe.expectMessageClass(
                ManagerProtocol.UpdateCompleted.class
        );
        assertEquals(2L, completion.tick());
    }

    @Test
    public void testBarrierIgnoresDuplicate() {
        managerProbe = testKit.createTestProbe();

        int nBoids = 2;
        barrier = testKit.spawn(
                BarrierActor.create(nBoids, managerProbe.ref()),
                "duplicate-test-barrier"
        );

        barrier.tell(new BarrierProtocol.StartPhase(1L));
        barrier.tell(new BarrierProtocol.BoidCompleted("boid-1", 1L));
        //Duplicate
        barrier.tell(new BarrierProtocol.BoidCompleted("boid-1", 1L));

        managerProbe.expectNoMessage(Duration.ofMillis(100));

        barrier.tell(new BarrierProtocol.BoidCompleted("boid-2", 1L));

        managerProbe.expectMessageClass(ManagerProtocol.UpdateCompleted.class);
    }

    @Test
    public void testBarrierWithSingleBoid() {
        managerProbe = testKit.createTestProbe();

        // Test edge case con un solo boid
        barrier = testKit.spawn(
                BarrierActor.create(1, managerProbe.ref()),
                "single-boid-barrier"
        );

        barrier.tell(new BarrierProtocol.StartPhase(1L));
        barrier.tell(new BarrierProtocol.BoidCompleted("boid-1", 1L));

        ManagerProtocol.UpdateCompleted completion = managerProbe.expectMessageClass(
                ManagerProtocol.UpdateCompleted.class
        );
        assertEquals(1L, completion.tick());
    }

    @Test
    public void testBarrierConcurrentCompletions() {
        managerProbe = testKit.createTestProbe();

        int nBoids = 50;
        barrier = testKit.spawn(
                BarrierActor.create(nBoids, managerProbe.ref()),
                "concurrent-barrier"
        );

        barrier.tell(new BarrierProtocol.StartPhase(1L));

        for( int i = 0; i < nBoids; i++) {
            barrier.tell(new BarrierProtocol.BoidCompleted("boid-" + i, 1L));
        }

        ManagerProtocol.UpdateCompleted completion = managerProbe.expectMessageClass(
                ManagerProtocol.UpdateCompleted.class
        );
        assertEquals(1L, completion.tick());

        managerProbe.expectNoMessage(Duration.ofMillis(100));
    }
}
