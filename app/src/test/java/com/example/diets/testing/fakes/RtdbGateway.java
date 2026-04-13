package com.example.diets.testing.fakes;

import androidx.annotation.Nullable;

public interface RtdbGateway {
    @Nullable Integer getCurrentLoad(String exerciseId, String uid);
    void saveData(String path, Object value, String actingUid);
}
