package fr.umontpellier.polytech.Controllers;

import fr.umontpellier.polytech.Services.NBodySimulationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.StringReader;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint("/simulation")
@ApplicationScoped
public class SimulationController implements SimulationUpdateListener {

    final Set<Session> sessions = new CopyOnWriteArraySet<>();

    @Inject
    NBodySimulationService simulationService;

    /** Called when a new WebSocket connection is opened */
    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        session.getAsyncRemote().sendText(simulationService.getCurrentStateAsJson());
    }

    /** Called when a new message is received from a WebSocket client
     * The message should be a JSON object with the following structure:
     * {
     *    "action": "start" | "update" | "stop",
     *    "numBodies": number,
     *    "gravity": number
     *    }
     * */
    @OnMessage
    public void onMessage(String message, Session session) {
        //System.out.println("📩 Received WebSocket message: " + message);

        try {
            JsonObject json = Json.createReader(new StringReader(message)).readObject();
            String action = json.getString("action", "");

            if (action.equals("start")) {
                int numBodies = json.getInt("numBodies", 5);
                double gravity = json.getJsonNumber("gravity").doubleValue();
                simulationService.setUpdateListener(this);
                simulationService.startSimulation(numBodies, gravity);
            } else if (action.equals("update")) {
                int numBodies = json.getInt("numBodies");
                double gravity = json.getJsonNumber("gravity").doubleValue();
                simulationService.updateSimulationSettings(numBodies, gravity);
            } else if (action.equals("stop")) {
                simulationService.stopSimulation();
            } else {
                session.getAsyncRemote().sendText("❌ Unknown action received.");
            }
        } catch (Exception e) {
            System.err.println("❌ Error processing WebSocket message: " + e.getMessage());
            session.getAsyncRemote().sendText("Invalid message format");
        }
    }

    /** Called when the simulation state is updated */
    @Override
    public void onSimulationUpdate(String json) {
        sendUpdateToClients(json);
    }

    /** Send the given JSON string to all connected WebSocket clients */
    public void sendUpdateToClients(String json) {
        for (Session session : sessions) {
            session.getAsyncRemote().sendText(json);
        }
    }
}
