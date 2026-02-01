package net.knightsandkings.knk.paper.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.knightsandkings.knk.paper.tasks.WgRegionIdTaskHandler;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Minimal HTTP server to expose region management endpoints for the Web API.
 * Only supports POST /Regions/rename?oldRegionId=...&newRegionId=...
 */
public class RegionHttpServer {
    private static final Logger LOGGER = Logger.getLogger(RegionHttpServer.class.getName());

    private final Plugin plugin;
    private final WgRegionIdTaskHandler handler;
    private final int port;
    private HttpServer server;
    private ExecutorService executor = Executors.newCachedThreadPool();

    public RegionHttpServer(Plugin plugin, WgRegionIdTaskHandler handler, int port) {
        this.plugin = plugin;
        this.handler = handler;
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/Regions/rename", new RenameHandler());
        server.setExecutor(executor);
        server.start();
        LOGGER.info("RegionHttpServer started on port " + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            LOGGER.info("RegionHttpServer stopped");
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private class RenameHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, "Method Not Allowed");
                return;
            }

            Map<String, String> query = parseQuery(exchange.getRequestURI());
            String oldRegionId = query.get("oldRegionId");
            String newRegionId = query.get("newRegionId");

            if (oldRegionId == null || newRegionId == null || oldRegionId.isBlank() || newRegionId.isBlank()) {
                send(exchange, 400, "oldRegionId and newRegionId are required");
                return;
            }

            // Run rename on main thread to keep WorldGuard safe
            plugin.getServer().getScheduler().callSyncMethod(plugin, () -> {
                boolean result = handler.renameRegion(oldRegionId, newRegionId);
                try {
                    send(exchange, 200, Boolean.toString(result));
                } catch (IOException e) {
                    LOGGER.warning("Failed to send response: " + e.getMessage());
                }
                return null;
            });
        }
    }

    private Map<String, String> parseQuery(URI uri) {
        Map<String, String> map = new HashMap<>();
        String query = uri.getRawQuery();
        if (query == null || query.isEmpty()) return map;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(urlDecode(kv[0]), urlDecode(kv[1]));
            }
        }
        return map;
    }

    private String urlDecode(String value) {
        try {
            return java.net.URLDecoder.decode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
