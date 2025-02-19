package fr.umontpellier.polytech.Models;

import jakarta.json.Json;
import jakarta.json.JsonObject;

public class Body {
    public double x, y, vx, vy, mass;

    public Body(double x, double y, double mass) {
        this.x = x;
        this.y = y;
        this.mass = mass;
        this.vx = 0;
        this.vy = 0;
    }

    public String toJson() {
        JsonObject json = Json.createObjectBuilder()
                .add("x", x)
                .add("y", y)
                .add("vx", vx)
                .add("vy", vy)
                .add("mass", mass)
                .build();
        return json.toString();
    }
}
