/*
 * MIT License
 *
 * Copyright (c) 2016.
 * Bucher Andreas, Fink Simon Dominik, Fraedrich Christoph, Popp Wolfgang,
 * Sell Leon, Werli Philemon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.unipassau.isl.evs.ssh.app.handler;

import android.support.annotation.NonNull;

import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.CoreConstants;
import de.unipassau.isl.evs.ssh.core.container.Component;
import de.unipassau.isl.evs.ssh.core.database.dto.Module;
import de.unipassau.isl.evs.ssh.core.database.dto.Slave;
import de.unipassau.isl.evs.ssh.core.handler.AbstractMessageHandler;
import de.unipassau.isl.evs.ssh.core.messaging.Message;
import de.unipassau.isl.evs.ssh.core.messaging.RoutingKey;
import de.unipassau.isl.evs.ssh.core.messaging.payload.ModulesPayload;

import static de.unipassau.isl.evs.ssh.core.messaging.RoutingKeys.GLOBAL_MODULES_UPDATE;

/**
 * AppModuleHandler manages information of all modules and slaves that are active in the System.
 *
 * @author Andreas Bucher
 * @author Wolfgang Popp
 */
public class AppModuleHandler extends AbstractMessageHandler implements Component {
    public static final Key<AppModuleHandler> KEY = new Key<>(AppModuleHandler.class);
    /**
     * Use sample code to filter for specific components
     * <pre>
     * Iterable<Module> filtered = Iterables.filter(components, PREDICATE_GPIO);
     * ArrayList<Module> modules = Lists.newArrayList(filtered);
     * </pre>
     */
    private static final Predicate<Module> PREDICATE_LIGHT = new Predicate<Module>() {
        @Override
        public boolean apply(Module input) {
            return Objects.equals(input.getModuleType(), CoreConstants.ModuleType.Light);
        }
    };
    private static final Predicate<Module> PREDICATE_DOOR_SENSOR = new Predicate<Module>() {
        @Override
        public boolean apply(Module input) {
            return Objects.equals(input.getModuleType(), CoreConstants.ModuleType.DoorSensor);
        }
    };
    private static final Predicate<Module> PREDICATE_DOOR_BUZZER = new Predicate<Module>() {
        @Override
        public boolean apply(Module input) {
            return Objects.equals(input.getModuleType(), CoreConstants.ModuleType.DoorBuzzer);
        }
    };
    private static final Predicate<Module> PREDICATE_WEATHER = new Predicate<Module>() {
        @Override
        public boolean apply(Module input) {
            return Objects.equals(input.getModuleType(), CoreConstants.ModuleType.WeatherBoard);
        }
    };
    private static final Predicate<Module> PREDICATE_CAMERA = new Predicate<Module>() {
        @Override
        public boolean apply(Module input) {
            return Objects.equals(input.getModuleType(), CoreConstants.ModuleType.Webcam);
        }
    };
    private final Set<Module> components = new HashSet<>();
    private final List<Slave> slaves = new LinkedList<>();
    private final ListMultimap<Slave, Module> modulesAtSlave = ArrayListMultimap.create();
    private final List<AppModuleListener> listeners = new LinkedList<>();

    @Override
    public RoutingKey[] getRoutingKeys() {
        return new RoutingKey[]{
                GLOBAL_MODULES_UPDATE
        };
    }

    @Override
    public void handle(Message.AddressedMessage message) {
        if (GLOBAL_MODULES_UPDATE.matches(message)) {
            ModulesPayload payload = GLOBAL_MODULES_UPDATE.getPayload(message);
            Set<Module> modules = payload.getModules();
            List<Slave> slaves = payload.getSlaves();
            ListMultimap<Slave, Module> modulesAtSlave = payload.getModulesAtSlaves();
            updateList(modules, slaves, modulesAtSlave);
        } else {
            invalidMessage(message);
        }
    }

    /**
     * Adds the given AppModuleListener to this handler.
     *
     * @param listener the listener to add
     */
    public void addAppModuleListener(AppModuleListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes the given AppModuleListener from this handler
     *
     * @param listener the listener to remove
     */
    public void removeAppModuleListener(AppModuleListener listener) {
        listeners.remove(listener);
    }

    private void fireModulesUpdated() {
        for (AppModuleListener listener : listeners) {
            listener.onModulesRefreshed();
        }
    }

    private void updateList(Set<Module> components, List<Slave> slaves, ListMultimap<Slave, Module> modulesAtSlave) {
        this.components.clear();
        if (components != null) {
            this.components.addAll(components);
        }

        this.slaves.clear();
        if (slaves != null) {
            this.slaves.addAll(slaves);
        }

        this.modulesAtSlave.clear();
        if (modulesAtSlave != null) {
            this.modulesAtSlave.putAll(modulesAtSlave);
        }
        fireModulesUpdated();
    }

    /**
     * Gets all currently installed Modules.
     *
     * @return a list of currently installed Modules
     */
    @NonNull
    public List<Module> getComponents() {
        return ImmutableList.copyOf(components);
    }

    /**
     * Gets all currently installed lights.
     *
     * @return a list of currently installed lights
     */
    @NonNull
    public List<Module> getLights() {
        Iterable<Module> filtered = Iterables.filter(components, PREDICATE_LIGHT);
        return Lists.newArrayList(filtered);
    }

    /**
     * Gets all currently installed door sensors.
     *
     * @return a list of all installed door sensors
     */
    @NonNull
    public List<Module> getDoorSensors() {
        Iterable<Module> filtered = Iterables.filter(components, PREDICATE_DOOR_SENSOR);
        return Lists.newArrayList(filtered);
    }

    /**
     * Gets all currently installed door buzzers.
     *
     * @return a list of all installed door buzzers
     */
    @NonNull
    public List<Module> getDoorBuzzers() {
        Iterable<Module> filtered = Iterables.filter(components, PREDICATE_DOOR_BUZZER);
        return Lists.newArrayList(filtered);
    }

    /**
     * Gets all currently installed cameras.
     *
     * @return a list of all installed cameras
     */
    @NonNull
    public List<Module> getCameras() {
        Iterable<Module> filtered = Iterables.filter(components, PREDICATE_CAMERA);
        return Lists.newArrayList(filtered);
    }

    /**
     * Gets all currently installed weather boards.
     *
     * @return a list of all installed weather boards
     */
    @NonNull
    public List<Module> getWeather() {
        Iterable<Module> filtered = Iterables.filter(components, PREDICATE_WEATHER);
        return Lists.newArrayList(filtered);
    }

    /**
     * Gets all currently installed slaves.
     *
     * @return a list of all installed slaves
     */
    @NonNull
    public List<Slave> getSlaves() {
        return ImmutableList.copyOf(slaves);
    }

    /**
     * Returns a list of connected modules at the given slave.
     *
     * @param slave the slave
     * @return a list of connected modules at the given slave
     */
    @NonNull
    public List<Module> getModulesAtSlave(Slave slave) {
        return modulesAtSlave.get(slave);
    }

    /**
     * The AppModuleListener is the listener interface used to be notified when the module or slave information changes.
     */
    public interface AppModuleListener {

        /**
         * Called when module or slave information changes.
         */
        void onModulesRefreshed();
    }
}
