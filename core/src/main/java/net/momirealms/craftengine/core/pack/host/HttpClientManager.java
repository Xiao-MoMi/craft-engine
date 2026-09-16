package net.momirealms.craftengine.core.pack.host;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.*;

public final class HttpClientManager {
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(5);
    private static HttpClient client = null;
    private static String lastEnableProxy = null;
    private static String lastHost = null;
    private static int lastPort = -1;
    private static String lastUsername = null;
    private static String lastPassword = null;

    public static void init(boolean enableProxy, String host, int port, String username, String password) {
        String currentEnableProxy = String.valueOf(enableProxy);

        boolean hasChanged =
                !Objects.equals(lastEnableProxy, currentEnableProxy) ||
                        !Objects.equals(lastHost, host) ||
                        !Objects.equals(lastPort, port) ||
                        !Objects.equals(lastUsername, username) ||
                        !Objects.equals(lastPassword, password);

        if (!hasChanged && client != null) {
            return;
        }

        lastEnableProxy = currentEnableProxy;
        lastHost = host;
        lastPort = port;
        lastUsername = username;
        lastPassword = password;

        HttpClient.Builder builder = HttpClient.newBuilder()
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .connectTimeout(Duration.ofSeconds(10));

        if (enableProxy) {
            builder.proxy(ProxySelector.of(new java.net.InetSocketAddress(host, port)));

            if (username != null && !username.isEmpty() && password != null) {
                builder.authenticator(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password.toCharArray());
                    }
                });
            }
        } else {
            builder.proxy(ProxySelector.getDefault());
        }

        HttpClient newClient = builder.build();
        HttpClient oldClient = client;
        client = newClient;

        if (oldClient != null) {
            oldClient.close();
        }
    }

    public static HttpClient get() {
        return client;
    }

    public static HttpRequest.Builder requestBuilder() {
        return HttpRequest.newBuilder().timeout(REQUEST_TIMEOUT);
    }

    public static <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
        CompletableFuture<HttpResponse<T>> response = get().sendAsync(request, handler);
        // Time out a dependent future so cancellation can still abort the original HTTP exchange.
        CompletableFuture<HttpResponse<T>> timed = response.copy()
                .orTimeout(request.timeout().orElse(REQUEST_TIMEOUT).toMillis(), TimeUnit.MILLISECONDS);
        CompletableFuture<HttpResponse<T>> result = timed.whenComplete((ignored, error) -> {
            if (error instanceof TimeoutException) response.cancel(true);
        });
        result.whenComplete((ignored, error) -> {
            if (error instanceof CancellationException) response.cancel(true);
        });
        return result;
    }

    public static <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) throws IOException, InterruptedException {
        CompletableFuture<HttpResponse<T>> response = sendAsync(request, handler);
        try {
            return response.get();
        } catch (InterruptedException e) {
            response.cancel(true);
            throw e;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IOException io) throw io;
            throw new IOException("Resource pack HTTP request failed", e.getCause());
        }
    }
}
