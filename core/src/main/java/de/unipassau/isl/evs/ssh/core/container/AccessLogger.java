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

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import de.ncoder.typedmap.Key;
import de.unipassau.isl.evs.ssh.core.schedule.ExecutionServiceComponent;
import io.netty.channel.EventLoop;

/**
 * @author Niko Fink
 */
public class AccessLogger extends AbstractComponent {
    private static final String TAG = AccessLogger.class.getSimpleName();
    public static final Key<AccessLogger> KEY = new Key<>(AccessLogger.class);
    private final Set<Integer> knownStackTraces = new HashSet<>();
    private Writer writer;
    private EventLoop eventLoop;

    @Override
    public void init(Container container) {
        super.init(container);
        final Context context = requireComponent(ContainerService.KEY_CONTEXT);
        final String name = context.getPackageName() + "-access.log";
        File dir = context.getExternalFilesDir(null);
        if (dir == null) {
            dir = context.getFilesDir();
        }
        final File file = new File(dir, name);
        try {
            writer = new FileWriter(file, true);
            writer.append("\n===============================================================================\n")
                    .append("Logging for ").append(context.getPackageName())
                    .append(" started at ").append(new Date().toString()).append("\n");
            Log.i(TAG, "Logging to " + file);
            eventLoop = requireComponent(ExecutionServiceComponent.KEY).next();
        } catch (IOException e) {
            Log.w(TAG, "Could not open FileOutputStream to " + file, e);
        }
    }

    @Override
    public void destroy() {
        if (writer != null) {
            try {
                writer.append("Log closed at").append(new Date().toString()).append("\n");
                writer.close();
            } catch (IOException e) {
                Log.w(TAG, "Could not close FileOutputStream", e);
            }
        }
        super.destroy();
    }

    /**
     * Log that the given object has been access together with the current call stack.
     */
    public void logAccess(Object accessed) {
        if (writer == null) {
            return;
        }

        final String name;
        if (accessed != null) {
            name = accessed.getClass().getSimpleName() + "." + accessed.toString();
        } else {
            name = "";
        }
        final Date timestamp = new Date();
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();

        eventLoop.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final int hash = Arrays.hashCode(stackTrace);
                    if (knownStackTraces.contains(hash)) {
                        return;
                    } else {
                        knownStackTraces.add(hash);
                    }

                    writer.append(timestamp.toString())
                            .append("\naccess to ").append(name)
                            .append(" from\n");
                    for (int i = 1; i < stackTrace.length; i++) {
                        final String className = stackTrace[i].getClassName();
                        final String methodName = stackTrace[i].getMethodName();

                        // filter all low level calls like Thread.run()
                        if (!className.startsWith("de.unipassau")) continue;

                        // filter the calls through the library that led to logAccess being called,
                        // so only the relevant parts of the stack are logged
                        final boolean isMessage = methodName.startsWith("sendMessage") || methodName.startsWith("sendReply");
                        final boolean isMessageClass = className.endsWith("AbstractMessageHandler") || className.endsWith("OutgoingRouter");
                        final boolean isPermission = methodName.startsWith("hasPermission");
                        final boolean isPermissionClass = className.endsWith("AbstractMasterHandler") || className.endsWith("PermissionController");
                        if (!((isMessage && isMessageClass) || (isPermission && isPermissionClass))) {
                            writer.append(stackTrace[i].toString()).append("\n");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
