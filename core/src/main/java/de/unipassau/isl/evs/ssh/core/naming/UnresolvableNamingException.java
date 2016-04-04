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

package de.unipassau.isl.evs.ssh.core.naming;

/**
 * Thrown when a naming mapping cannot be resolved to a DeviceID or public key.
 *
 * @author Wolfgang Popp
 */
public class UnresolvableNamingException extends Exception {

    /**
     * Constructs a new UnresolvableNamingException with a null detail message.
     */
    public UnresolvableNamingException() {
        super();
    }

    /**
     * Constructs a new UnresolvableNamingException with the given detail message.
     *
     * @param detailMessage the detailed message
     */
    public UnresolvableNamingException(String detailMessage) {
        super(detailMessage);
    }

    /**
     * Constructs a new UnresolvableNamingException with the given detail message and cause.
     *
     * @param detailMessage the detailed message
     * @param throwable     the throwable which caused this exception
     */
    public UnresolvableNamingException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    /**
     * Constructs a new UnresolvableNamingException with the given cause.
     *
     * @param throwable the throwable which caused this exception
     */
    public UnresolvableNamingException(Throwable throwable) {
        super(throwable);
    }

}
