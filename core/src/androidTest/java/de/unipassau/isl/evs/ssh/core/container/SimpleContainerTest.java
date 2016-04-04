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

package de.unipassau.isl.evs.ssh.core.container;

import junit.framework.TestCase;

import de.ncoder.typedmap.Key;

public class SimpleContainerTest extends TestCase {
    private final Key<TestComponent> k = new Key<>(TestComponent.class);
    private final Container container = new SimpleContainer();
    private final Key<TestComponent> k1 = new Key<>(TestComponent.class, "1");
    private final Key<TestComponent> k2 = new Key<>(TestComponent.class, "2");
    private final TestComponent c1 = new TestComponent();
    private final TestComponent c2 = new TestComponent();

    public void testRegister() {
        assertFalse(c1.isActive());
        assertFalse(c2.isActive());
        assertNull(container.get(k1));
        assertNull(container.get(k2));
        assertNull(container.get(k));

        container.register(k1, c1);

        assertTrue(c1.isActive());
        assertFalse(c2.isActive());
        assertEquals(container.get(k1), c1);
        assertNull(container.get(k2));
        assertNull(container.get(k));

        container.register(k2, c2);

        assertTrue(c1.isActive());
        assertTrue(c2.isActive());
        assertEquals(container.get(k1), c1);
        assertEquals(container.get(k2), c2);
        assertEquals(container.require(k1), c1);
        assertEquals(container.require(k2), c2);
        assertNull(container.get(k));

        try {
            container.register(k1, c2);
            fail("registered a component for an already registered key");
        } catch (Container.ComponentException e) {
        }

        assertEquals(c1.initCount, 1);
        assertEquals(c2.initCount, 1);

        container.unregister(k1);

        assertFalse(c1.isActive());
        assertTrue(c2.isActive());
        assertNull(container.get(k1));
        assertEquals(container.get(k2), c2);
        assertEquals(c1.destroyCount, 1);
        assertEquals(c2.destroyCount, 0);

        container.unregister(c2);

        assertFalse(c1.isActive());
        assertFalse(c2.isActive());
        assertNull(container.get(k1));
        assertNull(container.get(k2));
        assertEquals(c1.destroyCount, 1);
        assertEquals(c2.destroyCount, 1);

        try {
            container.require(k1);
            fail("missing required dependency");
        } catch (Container.ComponentException e) {
        }
        try {
            container.require(k2);
            fail("missing required dependency");
        } catch (Container.ComponentException e) {
        }
    }

    public void testShutdown() {
        container.register(k1, c1);
        container.register(k2, c2);

        assertTrue(c1.isActive());
        assertTrue(c2.isActive());

        container.shutdown();

        assertFalse(c1.isActive());
        assertFalse(c2.isActive());
        assertNull(container.get(k1));
        assertNull(container.get(k2));
        assertEquals(c1.destroyCount, 1);
        assertEquals(c2.destroyCount, 1);
    }
}