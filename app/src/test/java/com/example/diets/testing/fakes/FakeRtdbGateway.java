package com.example.diets.testing.fakes;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class FakeRtdbGateway implements RtdbGateway {

    private final Map<String, Object> db = new HashMap<>();
    private final Map<String, Integer> loads = new HashMap<>();
    private String ownerUid = "OWNER_UID"; // configurable si lo necesitas

    public void setOwnerUid(String ownerUid) { this.ownerUid = ownerUid; }

    public void seedCurrentLoad(String exerciseId, String uid, int value){
        loads.put(exerciseId + "|" + uid, value);
    }

    @Override public @Nullable Integer getCurrentLoad(String exerciseId, String uid) {
        return loads.get(exerciseId + "|" + uid);
    }

    @Override public void saveData(String path, Object value, String actingUid) {
        // Regla simulada: solo escribir dentro de /users/{actingUid}/...
        if (!path.contains("/users/" + actingUid + "/")) {
            throw new SecurityException("Write outside owner UID: " + actingUid);
        }
        db.put(path, value);
    }

    // Helpers para asserts en tests
    public boolean exists(String path){ return db.containsKey(path); }
    public Object read(String path){ return db.get(path); }
}
