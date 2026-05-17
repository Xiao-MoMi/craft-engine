package net.momirealms.craftengine.core.pack.host;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;

public final class HttpClientManager {
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
}