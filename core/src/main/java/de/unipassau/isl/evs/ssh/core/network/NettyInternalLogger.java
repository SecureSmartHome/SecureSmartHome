package de.unipassau.isl.evs.ssh.core.network;

import android.util.Log;

import org.slf4j.helpers.MessageFormatter;

import io.netty.util.internal.logging.AbstractInternalLogger;

/**
 * Simple InternalNetty logger for the netty framework which will, unlike the default logger, also log VERBOSE and DEBUG entries.
 */
public class NettyInternalLogger extends AbstractInternalLogger {
    public NettyInternalLogger(String name) {
        super(name);
    }

    public boolean isTraceEnabled() {
        return true;
        //return Log.isLoggable(name, Log.VERBOSE);
    }

    public boolean isDebugEnabled() {
        return true;
        //return Log.isLoggable(name, Log.DEBUG);
    }

    public boolean isInfoEnabled() {
        return true;
        //return Log.isLoggable(name, Log.INFO);
    }

    public boolean isWarnEnabled() {
        return true;
        //return Log.isLoggable(name, Log.WARN);
    }

    public boolean isErrorEnabled() {
        return true;
        //return Log.isLoggable(name, Log.ERROR);
    }

    public void trace(final String msg) {
        Log.v(name(), msg);
    }

    public void trace(final String format, final Object param1) {
        Log.v(name(), format(format, param1, null));
    }

    public void trace(final String format, final Object param1, final Object param2) {
        Log.v(name(), format(format, param1, param2));
    }

    public void trace(final String format, final Object... arguments) {
        Log.v(name(), format(format, arguments));
    }

    public void trace(final String msg, final Throwable t) {
        Log.v(name(), msg, t);
    }

    public void debug(final String msg) {
        Log.d(name(), msg);
    }

    public void debug(final String format, final Object arg1) {
        Log.d(name(), format(format, arg1, null));
    }

    public void debug(final String format, final Object param1, final Object param2) {
        Log.d(name(), format(format, param1, param2));
    }

    public void debug(final String format, final Object... arguments) {
        Log.d(name(), format(format, arguments));
    }

    public void debug(final String msg, final Throwable t) {
        Log.d(name(), msg, t);
    }

    public void info(final String msg) {
        Log.i(name(), msg);
    }

    public void info(final String format, final Object arg) {
        Log.i(name(), format(format, arg, null));
    }

    public void info(final String format, final Object arg1, final Object arg2) {
        Log.i(name(), format(format, arg1, arg2));
    }

    public void info(final String format, final Object... arguments) {
        Log.i(name(), format(format, arguments));
    }

    public void info(final String msg, final Throwable t) {
        Log.i(name(), msg, t);
    }

    public void warn(final String msg) {
        Log.w(name(), msg);
    }

    public void warn(final String format, final Object arg) {
        Log.w(name(), format(format, arg, null));
    }

    public void warn(final String format, final Object arg1, final Object arg2) {
        Log.w(name(), format(format, arg1, arg2));
    }

    public void warn(final String format, final Object... arguments) {
        Log.w(name(), format(format, arguments));
    }

    public void warn(final String msg, final Throwable t) {
        Log.w(name(), msg, t);
    }

    public void error(final String msg) {
        Log.e(name(), msg);
    }

    public void error(final String format, final Object arg) {
        Log.e(name(), format(format, arg, null));
    }

    public void error(final String format, final Object arg1, final Object arg2) {
        Log.e(name(), format(format, arg1, arg2));
    }

    public void error(final String format, final Object... arguments) {
        Log.e(name(), format(format, arguments));
    }

    public void error(final String msg, final Throwable t) {
        Log.e(name(), msg, t);
    }

    private String format(final String format, final Object arg1, final Object arg2) {
        return MessageFormatter.format(format, arg1, arg2).getMessage();
    }

    private String format(final String format, final Object[] args) {
        return MessageFormatter.arrayFormat(format, args).getMessage();
    }
}
