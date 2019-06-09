package me.msamoilov.log4j2.stackdriver;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.MonitoredResource;
import com.google.cloud.logging.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.status.StatusLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * @author Maksim Samoilov <samoylov.md@gmail.com>
 * @since 09.06.19
 */
@Plugin(
    name = "Stackdriver",
    category = Node.CATEGORY,
    elementType = Appender.ELEMENT_TYPE,
    printObject = true)
public class StackdriverAppender extends AbstractAppender {

    private static final Map<Level, Severity> log4jLevelToStackdriverSeverityMap = Map.of(
        Level.TRACE, Severity.NOTICE,
        Level.DEBUG, Severity.DEBUG,
        Level.INFO, Severity.INFO,
        Level.WARN, Severity.WARNING,
        Level.ERROR, Severity.ERROR,
        Level.FATAL, Severity.CRITICAL
    );


    //region Standard Log4j2 boilerplate
    /**
     * Builds KafkaAppender instances.
     * @param <B> The type to build
     */
    public static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B>
        implements org.apache.logging.log4j.core.util.Builder<me.msamoilov.log4j2.stackdriver.StackdriverAppender> {

        @PluginAttribute("Credentials")
        private String credentials;

        @PluginAttribute("AppName")
        private String appName;

        public B setCredentials(String credentials) {
            this.credentials = credentials;
            return asBuilder();
        }

        public String getCredentials() {
            return credentials;
        }

        public B setAppName(String appName) {
            this.appName = appName;
            return asBuilder();
        }

        public String getAppName() {
            return appName;
        }

        @SuppressWarnings("resource")
        @Override
        public me.msamoilov.log4j2.stackdriver.StackdriverAppender build() {
            return new me.msamoilov.log4j2.stackdriver.StackdriverAppender(getName(), getFilter(), isIgnoreExceptions(), getAppName(), getCredentials());
        }
    }

    @Deprecated
    public static me.msamoilov.log4j2.stackdriver.StackdriverAppender createAppender(
        final Layout<? extends Serializable> layout,
        final Filter filter,
        final String name,
        final boolean ignoreExceptions,
        final String appName,
        final String credentials) {

        return new me.msamoilov.log4j2.stackdriver.StackdriverAppender(name, filter, ignoreExceptions, appName, credentials);
    }

    /**
     * Creates a builder for a KafkaAppender.
     * @return a builder for a KafkaAppender.
     */
    @PluginBuilderFactory
    public static <B extends me.msamoilov.log4j2.stackdriver.StackdriverAppender.Builder<B>> B newBuilder() {
        return new me.msamoilov.log4j2.stackdriver.StackdriverAppender.Builder<B>().asBuilder();
    }
    //endregion

    private final String appName;

    private final Logging logging;
    private final MonitoredResource monitoredResource;
    private static final StatusLogger STATUS_LOGGER = StatusLogger.getLogger();

    public StackdriverAppender(String name,
                               Filter filter,
                               boolean ignoreExceptions,
                               String appName,
                               String credentials) {
        super(name, filter, null, ignoreExceptions);
        this.appName = appName;
        try {
            InputStream credsIs = this.getClass().getClassLoader().getResourceAsStream(credentials);
            if (credsIs == null) {
                throw new IOException("Current classloader doesn't have access to resource: " + credentials);
            }
            ServiceAccountCredentials parsedCredentials = ServiceAccountCredentials.fromStream(credsIs);
            LoggingOptions loggingOptions = LoggingOptions.newBuilder().setCredentials(parsedCredentials).build();
            this.logging = loggingOptions.getService();
            this.monitoredResource = MonitoredResource.newBuilder("global")
                .addLabel("app", appName)
                .build();
        } catch (IOException e) {
            STATUS_LOGGER.error("Cannot load credentials", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void append(LogEvent event) {
        LogEntry.Builder builder = LogEntry
            .newBuilder(Payload.StringPayload.of(event.getMessage().getFormattedMessage()))
            .setLogName(event.getLoggerName())
            .setTimestamp(event.getInstant().getEpochMillisecond())
            .setSeverity(log4jLevelToStackdriverSeverityMap.get(event.getLevel()));
        if (event.isIncludeLocation()) {
            builder.setSourceLocation(SourceLocation.newBuilder()
                .setFile(event.getSource().getClassName())
                .setFunction(event.getSource().getMethodName())
                .setLine((long) event.getSource().getLineNumber())
                .build());
        }
        LogEntry logEntry = builder.build();
        logging.write(Collections.singletonList(logEntry),
            Logging.WriteOption.logName(event.getLoggerName()),
            Logging.WriteOption.labels(Map.of(
                "app", appName,
                "logger", event.getLoggerName(),
                "threadName", event.getThreadName()
            )),
            Logging.WriteOption.resource(monitoredResource));
    }

}
