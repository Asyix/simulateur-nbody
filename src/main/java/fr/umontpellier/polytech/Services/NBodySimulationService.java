package fr.umontpellier.polytech.Services;

import fr.umontpellier.polytech.Controllers.SimulationUpdateListener;
import fr.umontpellier.polytech.Models.Body;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@ApplicationScoped
public class NBodySimulationService {

    private List<Body> bodies = new CopyOnWriteArrayList<>();
    private boolean running = true;
    private double gravity;
    private SimulationUpdateListener updateListener;

    public void setUpdateListener(SimulationUpdateListener updateListener) {
        this.updateListener = updateListener;
    }

    public List<Body> getBodies() {
        return bodies;
    }

    public double getGravity() {
        return gravity;
    }

    public void startSimulation(Integer numBodies, Double gravity) {
        if (numBodies == null || numBodies < 0) {
            numBodies = 0;
        }
        if (gravity == null || gravity < 0) {
            gravity = 0.0;
        }
        //System.out.println("🚀 Starting simulation with " + numBodies + " bodies, gravity: " + gravity);
        initializeBodies(numBodies);
        updateGravity(gravity);

        new Thread(() -> {
            running = true;
            while (running) {
                double dt = 0.005;
                updateSimulation(dt);
                updateListener.onSimulationUpdate(getCurrentStateAsJson());
                try {
                    Thread.sleep((long)(dt*1000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    public void updateSimulationSettings(Integer numBodies, Double gravity) {
        if (numBodies == null || numBodies < 0) {
            numBodies = 0;
        }
        if (gravity == null || gravity < 0) {
            gravity = 0.0;
        }
        updateBodies(numBodies);
        updateGravity(gravity);
    }

    public void stopSimulation() {
        //System.out.println("🛑 Stopping simulation.");
        running = false;
        bodies.clear();
    }

    private void initializeBodies(Integer numBodies) {
        if (numBodies == null || numBodies < 0) {
            numBodies = 0;
        }
        else if (numBodies > 1000) {
            numBodies = 1000;
        }
        bodies.clear();
        Random random = new Random();
        for (int i = 0; i < numBodies; i++) {
            double x = random.nextDouble(-1.0, 1) * 1000;
            double y = random.nextDouble(-1.0, 1) * 1000;
            double mass = random.nextDouble() * 10;
            bodies.add(new Body(x, y, mass));
        }
    }

    private void updateGravity(Double gravity) {
        if (gravity == null || gravity < 0) {
            gravity = 0.0;
        }
        // gravitational constant scaled for the simulation purposes
        double g = 6.67430 * 100;
        this.gravity = gravity * g;
    }

    private void updateBodies(Integer numBodies) {
        if (numBodies == null || numBodies < 0) {
            numBodies = 0;
        }
        else if (numBodies > 1000) {
            numBodies = 1000;
        }
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

    private void updateSimulation(Double dt) {
        if (dt == null || dt < 0) {
            dt = 0.005;
        }
        if (bodies.isEmpty()) {
            return;
        }
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
        if (bodies.isEmpty()) {
            return "[]";
        }
        return bodies.stream().map(Body::toJson).collect(Collectors.joining(",", "[", "]"));
    }
}