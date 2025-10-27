package com.example.diets.testing.fakes;

import androidx.annotation.Nullable;

public class FakeAuthProvider implements AuthProvider {
    private @Nullable String uid;

    public FakeAuthProvider() {}
    public FakeAuthProvider(@Nullable String uid) { this.uid = uid; }

    public void setUid(@Nullable String uid) { this.uid = uid; }

    @Override public @Nullable String getCurrentUid() { return uid; }
}
