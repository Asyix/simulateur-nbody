package fr.umontpellier.polytech.Services;

import fr.umontpellier.polytech.Controllers.SimulationUpdateListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NBodySimulationServiceTest {

    private NBodySimulationService simulationService;
    private TestSimulationUpdateListener updateListener;

    @BeforeEach
    void setUp() {
        simulationService = new NBodySimulationService();
        updateListener = new TestSimulationUpdateListener();
        simulationService.setUpdateListener(updateListener);
    }

    @AfterEach
    void tearDown() {
        simulationService.stopSimulation();
        assertTrue(simulationService.getBodies().isEmpty());
    }

    @Test
    void setUpdateListener() {
        assertNotNull(simulationService);
        assertNotNull(updateListener);
    }

    @Test
    void startSimulation() {
        simulationService.startSimulation(10, 1.0);
        assertEquals(10, simulationService.getBodies().size());
        assertEquals(667.430, simulationService.getGravity(), 0.1);
    }

    @Test
    void updateSimulationSettings() {
        simulationService.startSimulation(10, 1.0);
        simulationService.updateSimulationSettings(20, 2.0);
        assertEquals(20, simulationService.getBodies().size());
        assertEquals(667.430*2, simulationService.getGravity(), 0.1);
    }

    @Test
    void stopSimulation() {
        simulationService.startSimulation(10, 1.0);
        simulationService.stopSimulation();
        assertFalse(simulationService.getCurrentStateAsJson().isEmpty());
    }

    @Test
    void getCurrentStateAsJson() {
        simulationService.startSimulation(10, 1.0);
        String json = simulationService.getCurrentStateAsJson();
        assertNotNull(json);
        assertTrue(json.startsWith("["));
        assertTrue(json.endsWith("]"));
        assertTrue(simulationService.getCurrentStateAsJson().contains("\"x\":"));
        assertTrue(simulationService.getCurrentStateAsJson().contains("\"y\":"));
        assertTrue(simulationService.getCurrentStateAsJson().contains("\"mass\":"));
    }

    @Test
    void testSimulationUpdateListener() {
        try {
            simulationService.startSimulation(10, 1.0);
            Thread.sleep(100);
            String json = updateListener.getLastUpdate();
            assertNotNull(json);
            assertTrue(json.startsWith("["));
            assertTrue(json.endsWith("]"));
            assertTrue(json.contains("\"x\":"));
            assertTrue(json.contains("\"y\":"));
            assertTrue(json.contains("\"mass\":"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        }
    }

    private static class TestSimulationUpdateListener implements SimulationUpdateListener {
        private String lastUpdate;

        @Override
        public void onSimulationUpdate(String json) {
            this.lastUpdate = json;
        }

        public String getLastUpdate() {
            return lastUpdate;
        }
    }
}