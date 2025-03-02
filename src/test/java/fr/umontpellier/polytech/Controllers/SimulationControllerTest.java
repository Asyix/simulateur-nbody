// src/test/java/fr/umontpellier/polytech/Controllers/SimulationControllerTest.java
package fr.umontpellier.polytech.Controllers;

import fr.umontpellier.polytech.Services.NBodySimulationService;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class SimulationControllerTest {

    @Mock
    private NBodySimulationService simulationService;

    @Mock
    private Session session;

    @Mock
    private RemoteEndpoint.Async asyncRemote;

    @InjectMocks
    private SimulationController simulationController;

    @BeforeEach
    void setUp() {
        try (var ignored = MockitoAnnotations.openMocks(this)) {
            when(session.getAsyncRemote()).thenReturn(asyncRemote);
            when(simulationService.getCurrentStateAsJson()).thenReturn("test");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void onOpen() {
        simulationController.onOpen(session);
        assertTrue(simulationController.sessions.contains(session));
        verify(asyncRemote).sendText("test");
    }

    @Test
    void onMessage_start() {
        String message = "{\"action\":\"start\",\"numBodies\":10,\"gravity\":1.0}";
        simulationController.onMessage(message, session);
        verify(simulationService).setUpdateListener(simulationController);
        verify(simulationService).startSimulation(10, 1.0);
    }

    @Test
    void onMessage_update() {
        String message = "{\"action\":\"update\",\"numBodies\":20,\"gravity\":2.0}";
        simulationController.onMessage(message, session);
        verify(simulationService).updateSimulationSettings(20, 2.0);
    }

    @Test
    void onMessage_stop() {
        String message = "{\"action\":\"stop\"}";
        simulationController.onMessage(message, session);
        verify(simulationService).stopSimulation();
    }

    @Test
    void onMessage_invalid() {
        String message = "{\"action\":\"invalid\"}";
        simulationController.onMessage(message, session);
        verify(asyncRemote).sendText("❌ Unknown action received.");
    }

    @Test
    void onSimulationUpdate() {
        String json = "{\"x\":0,\"y\":0,\"mass\":1}";
        simulationController.sessions.add(session);
        simulationController.onSimulationUpdate(json);
        verify(asyncRemote).sendText(json);
    }
}