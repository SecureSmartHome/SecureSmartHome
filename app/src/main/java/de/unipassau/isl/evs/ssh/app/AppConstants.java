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

package de.unipassau.isl.evs.ssh.app;

/**
 * This Constants class provides constants needed by the app module.
 */
public enum AppConstants {
    ;

    /**
     * @author Phil Werli
     */
    public enum FragmentArguments {
        ;
        public static final String GROUP_ARGUMENT_FRAGMENT = "GROUP_ARGUMENT_FRAGMENT";
        public static final String USER_DEVICE_ARGUMENT_FRAGMENT = "USER_DEVICE_ARGUMENT_FRAGMENT";
    }

    /**
     * @author Phil Werli
     */
    public enum DialogArguments {
        ;
        public static final String EDIT_GROUP_DIALOG = "EDIT_GROUP_DIALOG";
        public static final String TEMPLATE_DIALOG = "TEMPLATE_DIALOG";
        public static final String ALL_GROUPS_DIALOG = "ALL_GROUPS_DIALOG";
        public static final String EDIT_USERDEVICE_DIALOG = "EDIT_USERDEVICE_DIALOG";
        public static final String DELETE_USERDEVICE_DIALOG = "DELETE_USERDEVICE_DIALOG";
    }
}
