import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {

    private final int port;
    private final ConcurrentMap<String, ClientHandler> users = new ConcurrentHashMap<>();
    private ServerSocket serverSocket;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public ChatServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("ChatServer listening on port " + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(clientSocket);
            new Thread(handler).start();
        }
    }

    private void broadcast(String message) {
        for (ClientHandler ch : users.values()) {
            ch.send(message);
        }
    }

    private void sendTo(String username, String message) {
        ClientHandler ch = users.get(username);
        if (ch != null) {
            ch.send(message);
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String username = null;
        private volatile long lastActive = System.currentTimeMillis();
        private final int IDLE_TIMEOUT_MS = 60_000;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        void send(String msg) {
            synchronized (out) {
                out.println(msg);
                out.flush();
            }
        }

        @Override
        public void run() {
            try {
                socket.setSoTimeout(1000);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

                if (!handleLogin()) {
                    closeConnection();
                    return;
                }

                ScheduledFuture<?> idleChecker = scheduler.scheduleAtFixedRate(() -> {
                    if (System.currentTimeMillis() - lastActive > IDLE_TIMEOUT_MS) {
                        send("INFO idle-timeout disconnected");
                        try { socket.close(); } catch (IOException ignored) {}
                    }
                }, 5, 5, TimeUnit.SECONDS);

                while (!socket.isClosed()) {
                    String line = null;
                    try {
                        line = in.readLine();
                    } catch (SocketTimeoutException ste) {
                        if (socket.isClosed()) break;
                        continue;
                    }

                    if (line == null) break;
                    lastActive = System.currentTimeMillis();
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    if (line.startsWith("MSG ")) {
                        String text = line.substring(4).trim();
                        if (!text.isEmpty()) {
                            broadcast("MSG " + username + " " + text);
                        }
                    } else if (line.equals("WHO")) {
                        for (String u : users.keySet()) {
                            send("USER " + u);
                        }
                    } else if (line.startsWith("DM ")) {
                        String rest = line.substring(3).trim();
                        int sp = rest.indexOf(' ');
                        if (sp == -1) {
                            send("ERR invalid-dm-format");
                        } else {
                            String target = rest.substring(0, sp).trim();
                            String text = rest.substring(sp + 1).trim();
                            if (target.equals(username)) {
                                send("ERR cannot-dm-yourself");
                            } else {
                                ClientHandler targetHandler = users.get(target);
                                if (targetHandler == null) {
                                    send("ERR user-not-found");
                                } else {
                                    targetHandler.send("MSG " + username + " " + text);
                                }
                            }
                        }
                    } else if (line.equals("PING")) {
                        send("PONG");
                    } else {
                        send("ERR unknown-command");
                    }
                }

                idleChecker.cancel(true);
                removeAndNotify();

            } catch (IOException e) {
                removeAndNotify();
            } finally {
                closeConnection();
            }
        }

        private boolean handleLogin() throws IOException {
            long start = System.currentTimeMillis();
            while (true) {
                String line = null;
                try {
                    line = in.readLine();
                } catch (SocketTimeoutException ste) {
                    if (System.currentTimeMillis() - start > 30_000) {
                        sendRaw("ERR login-timeout");
                        return false;
                    }
                    continue;
                }

                if (line == null) return false;
                line = line.trim();
                if (line.isEmpty()) continue;

                if (!line.startsWith("LOGIN ")) {
                    sendRaw("ERR expected-login");
                    continue;
                }

                String candidate = line.substring(6).trim();
                if (candidate.isEmpty() || candidate.contains(" ")) {
                    sendRaw("ERR invalid-username");
                    continue;
                }

                ClientHandler existing = users.putIfAbsent(candidate, this);
                if (existing != null) {
                    sendRaw("ERR username-taken");
                    return false;
                } else {
                    this.username = candidate;
                    sendRaw("OK");
                    broadcast("INFO " + username + " connected");
                    lastActive = System.currentTimeMillis();
                    return true;
                }
            }
        }

        private void sendRaw(String msg) {
            if (out != null) {
                synchronized (out) {
                    out.println(msg);
                    out.flush();
                }
            }
        }

        private void removeAndNotify() {
            if (username != null) {
                boolean removed = users.remove(username, this);
                if (removed) {
                    broadcast("INFO " + username + " disconnected");
                }
            }
        }

        private void closeConnection() {
            try { if (in != null) in.close(); } catch (IOException ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
            try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}
        }
    }

    public static void main(String[] args) {
        int port = 4000;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port, using default 4000.");
            }
        } else {
            String env = System.getenv("PORT");
            if (env != null) {
                try {
                    port = Integer.parseInt(env);
                } catch (NumberFormatException ignored) {}
            }
        }

        ChatServer server = new ChatServer(port);
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }
}
