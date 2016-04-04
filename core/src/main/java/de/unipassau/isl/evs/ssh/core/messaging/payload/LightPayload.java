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

package de.unipassau.isl.evs.ssh.core.messaging.payload;

import de.unipassau.isl.evs.ssh.core.database.dto.Module;

/**
 * Payload class for Light Messages
 *
 * @author Christoph Fraedrich
 */
public class LightPayload implements MessagePayload {

    private final Module module;
    private final boolean on;

    public LightPayload(boolean on, Module module) {
        this.on = on;
        this.module = module;
    }

    /**
     * Returns a boolean indicating whether the light should switched on or is on.
     * If not it means the opposite, that is, the light should be switched off, not just "not switched on".
     * <p/>
     * Whether the light status is only checked or switched depends on the used routing key.
     *
     * @return true if the light should be switched on, false if the light should be switched off.
     */
    public boolean getOn() {
        return on;
    }

    /**
     * Returns a String indicating which lamped should be switched or checked.
     *
     * @return String indicating the module name
     */
    public Module getModule() {
        return module;
    }
}
