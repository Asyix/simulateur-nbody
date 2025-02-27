package fr.umontpellier.polytech.Services;

import fr.umontpellier.polytech.Models.Body;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.StringReader;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@ServerEndpoint("/simulation")
@ApplicationScoped
public class NBodySimulationService {

    private final Set<Session> sessions = new CopyOnWriteArraySet<>();
    private List<Body> bodies = new ArrayList<>();
    private boolean running = true;

    private double gravity;

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        session.getAsyncRemote().sendText(getCurrentStateAsJson());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("📩 Received WebSocket message: " + message); // Log the message

        try {
            JsonObject json = Json.createReader(new StringReader(message)).readObject();
            String action = json.getString("action", "");

            double gravityConstant = 6.67430e-11;
            if (action.equals("start")) {
                int numBodies = json.getInt("numBodies", 5);
                double gravity = json.getJsonNumber("gravity").doubleValue() * gravityConstant;

                System.out.println("🚀 Starting simulation with " + numBodies + " bodies, gravity: " + gravity);

                initializeBodies(numBodies);  // Ensure this method exists
                updateGravity(gravity);  // Ensure this updates gravity
                startSimulation();
            } else if (action.equals("update")) {
                int numBodies = json.getInt("numBodies");
                double gravity = json.getJsonNumber("gravity").doubleValue() * gravityConstant;

                updateBodies(numBodies);  // Ensure this method exists
                updateGravity(gravity);  // Ensure this updates gravity
            }
            else if (action.equals("stop")) {
                System.out.println("🛑 Stopping simulation.");
                running = false;
            } else {
                System.out.println("❌ Unknown action received.");
            }
        } catch (Exception e) {
            System.err.println("❌ Error processing WebSocket message: " + e.getMessage());
            session.getAsyncRemote().sendText("Invalid message format");
        }
    }

    private void initializeBodies(int numBodies) {
        bodies.clear();
        Random random = new Random();
        for (int i = 0; i < numBodies; i++) {
            double x = random.nextDouble(-1.0, 1) * 1000;
            double y = random.nextDouble(-1.0, 1) * 1000;
            double mass = random.nextDouble() * 10000000;
            bodies.add(new Body(x, y, mass));
        }
    }

    private void updateGravity(double gravity) {
        this.gravity = gravity;
    }

    private void updateBodies(int numBodies) {
        if (numBodies < bodies.size()) {
            bodies = bodies.subList(0, numBodies);
        } else if (numBodies > bodies.size()) {
            Random random = new Random();
            for (int i = bodies.size(); i < numBodies; i++) {
                double x = random.nextDouble(-1.0, 1) * 1000;
                double y = random.nextDouble(-1.0, 1) * 1000;
                double mass = random.nextDouble() * 10000000;
                bodies.add(new Body(x, y, mass));
            }
        }
    }

    private void startSimulation() {
        new Thread(() -> {
            running = true;
            while (running) {
                updateSimulation();
                sendUpdateToClients();
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    private void updateSimulation() {
        double G = this.gravity;
        for (Body body : bodies) {
            for (Body other : bodies) {
                if (body != other) {
                    double dx = other.x - body.x;
                    double dy = other.y - body.y;
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    double force = G * body.mass * other.mass / (distance * distance);
                    double ax = force * dx / distance / body.mass;
                    double ay = force * dy / distance / body.mass;
                    body.vx += ax;
                    body.vy += ay;
                }
            }
            body.x += body.vx;
            body.y += body.vy;
        }
    }

    private void sendUpdateToClients() {
        String json = getCurrentStateAsJson();
        for (Session session : sessions) {
            session.getAsyncRemote().sendText(json);
        }
    }

    private String getCurrentStateAsJson() {
        return bodies.stream().map(Body::toJson).collect(Collectors.joining(",", "[", "]"));
    }
}