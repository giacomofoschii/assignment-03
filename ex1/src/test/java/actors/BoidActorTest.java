package actors;

import akka.actor.testkit.typed.javadsl.*;
import akka.actor.typed.ActorRef;
import org.junit.jupiter.api.*;
import pcd.ass03.actors.*;
import pcd.ass03.model.BoidState;
import pcd.ass03.protocols.*;
import pcd.ass03.utils.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class BoidActorTest {

    private static ActorTestKit testKit;
    private BoidsParams params;

    @BeforeAll
    public static void setupTestKit() {
        testKit = ActorTestKit.create();
    }

    @AfterAll
    public static void tearDown() {
        testKit.shutdownTestKit();
    }

    @BeforeEach
    public  void setup() {
        params = new BoidsParams(800, 800);
        params.setSeparationWeight(1.0);
        params.setAlignmentWeight(0.5);
        params.setCohesionWeight(1.5);
    }

    @Test
    public void testBoidForceCalculation() {
        P2d myPos = new P2d(0,0);
        P2d myVel = new P2d(1,0);

        List<BoidState> neighbors = Arrays.asList(
                new BoidState(new P2d(10,0), new V2d(0,1), "near-boid"),
                new BoidState(new P2d(30,30), new V2d(-1, 0), "mid-boid"),
                new BoidState(new P2d(45,0), new V2d(0,-1), "far-boid")
        );

        // Separation test
        V2d sep = calcSep(myPos, neighbors, params.getAvoidRadius());
        assertTrue(sep.x() < 0, "Separation force should push away from near-boid");
        assertEquals(0, sep.y(), 0.01, "Separation force Y should be 0");

        // Alignment test
        V2d align = calcAli(myVel, neighbors);
        assertTrue(align.x() < 0, "Alignment should tend towards average velocity");

        // Cohesion test
        V2d coh = calcCoh(myPos, neighbors);
        assertTrue(coh.x() > 0, "Cohesion should attract to the center of neighbors");
        assertTrue(coh.y() > 0, "Cohesion Y should be positive");
    }

    @Test
    public void testBoidPositionUpdate() {
        TestProbe<ManagerProtocol.Command> managerProde = testKit.createTestProbe();
        TestProbe<BarrierProtocol.Command> barrierProbe = testKit.createTestProbe();

        P2d initialPos = new P2d(0, 0);
        V2d initialVel = new V2d(2, 0);

        ActorRef<BoidProtocol.Command> boid = testKit.spawn(
                BoidActor.create("test-boid", initialPos, initialVel,
                        params, managerProde.ref(), barrierProbe.ref()),
                "test-boid-actor"
        );

        List<BoidState> neighbors = Arrays.asList(
                new BoidState(new P2d(15, 0), new V2d(0, 1), "neighbor1"),
                new BoidState(new P2d(30, 30), new V2d(-1, -1), "neighbor2")
        );

        boid.tell(new BoidProtocol.UpdateRequest(1L, neighbors));

        ManagerProtocol.BoidUpdated update = managerProde.expectMessageClass(
                ManagerProtocol.BoidUpdated.class
        );

        assertAll(
                () -> assertEquals("test-boid", update.boidId()),
                () -> assertNotNull(update.position()),
                () -> assertNotNull(update.velocity()),
                () -> assertNotEquals(initialPos.x(), update.position().x())
        );

        BarrierProtocol.BoidCompleted completion = barrierProbe.expectMessageClass(
                BarrierProtocol.BoidCompleted.class
        );

        assertEquals("test-boid", completion.boidId());
        assertEquals(1L, completion.tick());
    }

    @Test
    public void testBoidParameterUpdate() {
        TestProbe<ManagerProtocol.Command> managerProbe = testKit.createTestProbe();
        TestProbe<BarrierProtocol.Command> barrierProbe = testKit.createTestProbe();

        ActorRef<BoidProtocol.Command> boid = testKit.spawn(
                BoidActor.create("param-test-boid", new P2d(0, 0), new V2d(1, 0),
                        params, managerProbe.ref(), barrierProbe.ref()),
                "param-test-boid"
        );

        List<BoidState> closeNeighbors = List.of(
                new BoidState(new P2d(5, 0), new V2d(0, 0), "very-close-neighbor")
        );

        boid.tell(new BoidProtocol.UpdateRequest(1L, closeNeighbors));

        ManagerProtocol.BoidUpdated update = managerProbe.expectMessageClass(
                ManagerProtocol.BoidUpdated.class
        );

        double velXBase = update.velocity().x();

        BoidsParams newParams = new BoidsParams(800, 800);
        newParams.setSeparationWeight(2.0);
        newParams.setAlignmentWeight(0.5);
        newParams.setCohesionWeight(1.5);

        boid.tell(new BoidProtocol.UpdateParams(newParams));
        boid.tell(new BoidProtocol.UpdateRequest(2L, closeNeighbors));
        ManagerProtocol.BoidUpdated updateSep = managerProbe.expectMessageClass(
                ManagerProtocol.BoidUpdated.class
        );
        double velXSep = updateSep.velocity().x();

        assertAll(
                () -> assertEquals("param-test-boid", update.boidId()),
                () -> assertNotNull(update.position()),
                () -> assertNotNull(update.velocity()),
                () -> assertTrue(velXSep < velXBase,
                        "Velocity should decrease with increased separation weight")
        );

    }

    @Test
    public void testBoidWraparound() {
        TestProbe<ManagerProtocol.Command> managerProbe = testKit.createTestProbe();
        TestProbe<BarrierProtocol.Command> barrierProbe = testKit.createTestProbe();

        P2d edgePos = new P2d(params.getMaxX() - 1, 0);
        V2d rightVel = new V2d(5, 0);

        ActorRef<BoidProtocol.Command> boid = testKit.spawn(
                BoidActor.create("edge-boid", edgePos, rightVel,
                        params, managerProbe.ref(), barrierProbe.ref()),
                "edge-boid"
        );

        boid.tell(new BoidProtocol.UpdateRequest(3L, List.of()));

        ManagerProtocol.BoidUpdated update = managerProbe.expectMessageClass(
                ManagerProtocol.BoidUpdated.class
        );

        assertAll(
                () -> assertEquals("edge-boid", update.boidId()),
                () -> assertTrue(update.position().x() < params.getMaxX() + 10, "Boid should wrap around to the left side"),
                () -> assertEquals(0, update.position().y(), 0.01, "Y position should remain unchanged")
        );
    }

    private V2d calcSep(P2d pos, List<BoidState> neighbors, double avoidRadius) {
        double dx = 0;
        double dy = 0;
        int count = 0;

        for (BoidState other : neighbors) {
            P2d otherPos = other.pos();
            double distance = pos.distance(otherPos);
            if (distance < avoidRadius && distance > 0) {
                dx += pos.x() - otherPos.x();
                dy += pos.y() - otherPos.y();
                count++;
            }
        }

        if (count > 0) {
            dx /= count;
            dy /= count;
            return new V2d(dx, dy).getNormalized();
        }
        return new V2d(0, 0);
    }

    private V2d calcAli(P2d vel, List<BoidState> neighbors) {
        if (neighbors.isEmpty()) return new V2d(0, 0);

        double avgVx = 0;
        double avgVy = 0;

        for (BoidState other : neighbors) {
            avgVx += other.vel().x();
            avgVy += other.vel().y();
        }

        avgVx /= neighbors.size();
        avgVy /= neighbors.size();

        return new V2d(avgVx - vel.x(), avgVy - vel.y()).getNormalized();
    }

    private V2d calcCoh(P2d pos, List<BoidState> neighbors) {
        if (neighbors.isEmpty()) return new V2d(0, 0);

        double centerX = 0;
        double centerY = 0;

        for (BoidState other : neighbors) {
            centerX += other.pos().x();
            centerY += other.pos().y();
        }

        centerX /= neighbors.size();
        centerY /= neighbors.size();

        return new V2d(centerX - pos.x(), centerY - pos.y()).getNormalized();
    }
}
