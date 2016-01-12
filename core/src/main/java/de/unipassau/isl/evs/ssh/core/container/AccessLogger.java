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
    private Set<Integer> knownStackTraces = new HashSet<>();
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
