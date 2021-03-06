package ru.vyarus.dropwizard.guice.module.installer.internal;

import com.google.inject.Injector;
import io.dropwizard.Application;
import io.dropwizard.cli.Command;
import io.dropwizard.cli.EnvironmentCommand;
import io.dropwizard.setup.Bootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.module.installer.scanner.ClassVisitor;
import ru.vyarus.dropwizard.guice.module.installer.scanner.ClasspathScanner;
import ru.vyarus.dropwizard.guice.module.installer.util.FeatureUtils;

import java.util.List;

/**
 * Provides support for commands injection support and classpath scanning resolution of commands.
 *
 * @author Vyacheslav Rusakov
 * @since 01.09.2014
 */
public final class CommandSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandSupport.class);

    private CommandSupport() {
    }

    /**
     * Scans classpath to find commands and register them. Default constructor used to instantiate command.
     * Commands are instantiated using default constructor, but {@link io.dropwizard.cli.EnvironmentCommand}
     * should have constructor with {@link io.dropwizard.Application} argument.
     *
     * @param bootstrap bootstrap object
     * @param scanner   configured scanner instance
     */
    public static void registerCommands(final Bootstrap bootstrap, final ClasspathScanner scanner) {
        scanner.scan(new CommandClassVisitor(bootstrap));
    }

    /**
     * Inject dependencies into all registered environment commands. (only field and setter injection could be used)
     * There is no need to process other commands, because only environment commands will run bundles and so will
     * start the injector.
     *
     * @param commands registered commands
     * @param injector guice injector object
     */
    public static void initCommands(final List<Command> commands, final Injector injector) {
        if (commands != null) {
            for (Command cmd : commands) {
                if (cmd instanceof EnvironmentCommand) {
                    injector.injectMembers(cmd);
                }
            }
        }
    }

    /**
     * Search catch all {@link Command} derived classes.
     * Instantiate command with default constructor and {@link io.dropwizard.cli.EnvironmentCommand}
     * using constructor with {@link io.dropwizard.Application} argument.
     */
    private static class CommandClassVisitor implements ClassVisitor {
        private final Bootstrap bootstrap;

        CommandClassVisitor(final Bootstrap bootstrap) {
            this.bootstrap = bootstrap;
        }

        @Override
        public void visit(final Class<?> type) {
            if (FeatureUtils.is(type, Command.class)) {
                try {
                    Command cmd;
                    if (EnvironmentCommand.class.isAssignableFrom(type)) {
                        cmd = (Command) type.getConstructor(Application.class)
                                .newInstance(bootstrap.getApplication());
                    } else {
                        cmd = (Command) type.newInstance();
                    }
                    bootstrap.addCommand(cmd);
                    LOGGER.debug("Command registered: {}", type.getSimpleName());
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to instantiate command: "
                            + type.getSimpleName(), e);
                }
            }
        }
    }
}
