package de.unipassau.isl.evs.ssh.core;

import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class Container {
    public static final Set<String> components = new HashSet<>();

    static {
        Log.i("Container", "class load");
        components.add("core");
    }
}
