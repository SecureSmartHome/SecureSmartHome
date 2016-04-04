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

import android.content.Intent;
import android.test.ServiceTestCase;

import de.ncoder.typedmap.Key;

public class ContainerServiceTest extends ServiceTestCase<ContainerService> {
    private final Key<TestComponent> k = new Key<>(TestComponent.class);
    private final TestComponent c = new TestComponent();

    public ContainerServiceTest() {
        super(ContainerService.class);
    }

    public void test() {
        ContainerService.Binder binder = (ContainerService.Binder) bindService(new Intent(getContext(), ContainerService.class));

        assertNotNull(binder);
        assertNotNull(binder.getData());

        assertFalse(c.isActive());
        assertNull(binder.get(k));

        binder.register(k, c);

        assertTrue(c.isActive());
        assertNotNull(binder.get(k));

        shutdownService();

        assertFalse(c.isActive());
        assertEquals(c.destroyCount, 1);
    }
}