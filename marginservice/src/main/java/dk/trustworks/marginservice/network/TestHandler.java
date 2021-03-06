package dk.trustworks.marginservice.network;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.vertx.core.Handler;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.ext.web.RoutingContext;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

public class TestHandler implements Handler<RoutingContext> {

    private static class BufferWriter extends Writer {

        private final Buffer buffer = Buffer.buffer();

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            buffer.appendString(new String(cbuf, off, len));
        }

        @Override
        public void flush() throws IOException {
            // NO-OP
        }

        @Override
        public void close() throws IOException {
            // NO-OP
        }

        Buffer getBuffer() {
            return buffer;
        }
    }

    private CollectorRegistry registry;

    /**
     * Construct a MetricsHandler for the default registry.
     */
    public TestHandler() {
        this(CollectorRegistry.defaultRegistry);
    }

    /**
     * Construct a MetricsHandler for the given registry.
     */
    public TestHandler(CollectorRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void handle(RoutingContext ctx) {
        try {
            final BufferWriter writer = new BufferWriter();
            TextFormat.write004(writer, registry.filteredMetricFamilySamples(parse(ctx.request())));
            ctx.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", TextFormat.CONTENT_TYPE_004)
                    .end(writer.getBuffer());
        } catch (IOException e) {
            ctx.fail(e);
        }
    }

    private Set<String> parse(HttpServerRequest request) {
        return new HashSet(request.params().getAll("name[]"));
    }
}
