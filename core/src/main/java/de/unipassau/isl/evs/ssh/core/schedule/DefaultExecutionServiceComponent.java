package de.unipassau.isl.evs.ssh.core.schedule;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultExecutorServiceFactory;

/**
 * An ExecutionServiceComponent which uses the default EventLoopGroup configuration provided by Netty.
 *
 * @author Christoph Fraedrich
 */
public class DefaultExecutionServiceComponent extends ExecutionServiceComponent {
    private final String name;

    public DefaultExecutionServiceComponent(String name) {
        this.name = name;
    }

    @Override
    protected EventLoopGroup createEventLoopGroup() {
        return new NioEventLoopGroup(0, new DefaultExecutorServiceFactory(name));
    }
}
