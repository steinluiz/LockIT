package com.stein.lockit.data;

/** The traps that can go in the trap slot of the lock menu. The id is the item/config key. */
public enum TrapType {
    INK("ink_trap"),
    SOUND("sound_trap"),
    SPIKE("spike_trap");

    private final String id;

    TrapType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static TrapType fromId(String id) {
        if (id == null) return null;
        for (TrapType t : values()) {
            if (t.id.equalsIgnoreCase(id)) return t;
        }
        return null;
    }
}
