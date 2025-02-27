package fr.umontpellier.polytech.Services;

import fr.umontpellier.polytech.Controllers.SimulationController;
import fr.umontpellier.polytech.Models.Body;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@ApplicationScoped
public class NBodySimulationService {

    private List<Body> bodies = new CopyOnWriteArrayList<>();
    private boolean running = true;
    private double gravity;

    @Inject
    private SimulationController simulationController;

    public void startSimulation(int numBodies, double gravity) {
        System.out.println("🚀 Starting simulation with " + numBodies + " bodies, gravity: " + gravity);
        initializeBodies(numBodies);
        updateGravity(gravity);

        new Thread(() -> {
            running = true;
            while (running) {
                double dt = 0.005;
                updateSimulation(dt);
                simulationController.sendUpdateToClients(getCurrentStateAsJson());
                try {
                    Thread.sleep((long)(dt*1000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    public void updateSimulation(int numBodies, double gravity) {
        updateBodies(numBodies);
        updateGravity(gravity);
    }

    public void stopSimulation() {
        System.out.println("🛑 Stopping simulation.");
        running = false;
    }

    private void initializeBodies(int numBodies) {
        bodies.clear();
        Random random = new Random();
        for (int i = 0; i < numBodies; i++) {
            double x = random.nextDouble(-1.0, 1) * 1000;
            double y = random.nextDouble(-1.0, 1) * 1000;
            double mass = random.nextDouble() * 10;
            bodies.add(new Body(x, y, mass));
        }
    }

    private void updateGravity(double gravity) {
        // gravitational constant scaled for the simulation purposes
        double g = 6.67430 * 100;
        this.gravity = gravity * g;
    }

    private void updateBodies(int numBodies) {
        if (numBodies < bodies.size()) {
            bodies = bodies.subList(0, numBodies);
        } else if (numBodies > bodies.size()) {
            Random random = new Random();
            for (int i = bodies.size(); i < numBodies; i++) {
                double x = random.nextDouble(-1.0, 1) * 1000;
                double y = random.nextDouble(-1.0, 1) * 1000;
                double mass = random.nextDouble() * 10;
                bodies.add(new Body(x, y, mass));
            }
        }
    }

    private void updateSimulation(double dt) {
        double G = this.gravity;
        double epsilon = 1.0;
        for (Body body : bodies) {
            for (Body other : bodies) {
                if (body != other) {
                    double dx = other.x - body.x;
                    double dy = other.y - body.y;
                    double distanceSquared = dx * dx + dy * dy + epsilon * epsilon; // Apply softening
                    double distance = Math.sqrt(distanceSquared);
                    double force = G * body.mass * other.mass / (distance * distance);
                    double ax = force * dx / distance / body.mass;
                    double ay = force * dy / distance / body.mass;
                    body.vx += ax;
                    body.vy += ay;
                }
            }
            body.x += body.vx * dt;
            body.y += body.vy * dt;
        }
    }

    public String getCurrentStateAsJson() {
        return bodies.stream().map(Body::toJson).collect(Collectors.joining(",", "[", "]"));
    }
}