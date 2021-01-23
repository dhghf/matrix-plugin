package dev.dhdf.polo.types;

import org.json.JSONObject;

import java.util.UUID;

public class PoloPlayer {
    public final String name;
    public final String uuid;
    public final String displayName;
    public final String texture;

    public PoloPlayer(String name, UUID uuid, String displayName, String texture) {
        this.name = name;
        this.uuid = uuid.toString().replace("-", "");
        this.displayName = displayName;
        this.texture = texture;
    }

    public JSONObject toJSON() {
        return new JSONObject()
                .put("name", this.name)
                .put("uuid", this.uuid)
                .put("displayName", this.displayName)
                .put("texture", this.texture);
    }
}
