import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.net.URLDecoder;

public class SimpleFileHttpServer {

    public static void main(String[] args) {
        int port = 1989;
        Path webRoot = Paths.get("src");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Sunucu başlatıldı: http://localhost:" + port + "/");

            while (true) {
                Socket client = serverSocket.accept();
                new Thread(() -> handleClient(client, webRoot)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket client, Path webRoot) {
        try (client;
             InputStream in = client.getInputStream();
             OutputStream out = client.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

            // Request satırını oku
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isBlank()) return;

            String[] parts = requestLine.split("\\s+");
            if (parts.length < 2) return;

            String method = parts[0];
            String rawPath = parts[1];

            // Favicon isteğini yoksay
            if (rawPath.equals("/favicon.ico")) {
                sendSimpleResponse(out, "404 Not Found", "<h1>404 Not Found</h1>");
                return;
            }


            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) break; // boş satır -> header sonu
            }

            if (!"GET".equalsIgnoreCase(method)) {
                sendSimpleResponse(out, "405 Method Not Allowed", "<h1>405 Method Not Allowed</h1>");
                return;
            }


            String path = URLDecoder.decode(rawPath, "UTF-8");
            if (path.startsWith("/")) path = path.substring(1);
            if (path.isBlank()) path = "index.html";

            Path target = webRoot.resolve(path).normalize();
            if (!Files.exists(target) || Files.isDirectory(target)) {
                sendSimpleResponse(out, "404 Not Found", "<h1>404 Not Found</h1>");
                return;
            }

            byte[] body = Files.readAllBytes(target);
            String contentType = guessContentType(target);

            PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"), false);
            writer.printf("HTTP/1.1 200 OK\r\n");
            writer.printf("Content-Type: %s; charset=UTF-8\r\n", contentType);
            writer.printf("Content-Length: %d\r\n", body.length);
            writer.printf("Connection: close\r\n");
            writer.printf("\r\n");
            writer.flush();

            out.write(body);
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String guessContentType(Path target) {
        String name = target.getFileName().toString().toLowerCase();
        if (name.endsWith(".html") || name.endsWith(".htm")) return "text/html";
        if (name.endsWith(".css")) return "text/css";
        if (name.endsWith(".js")) return "application/javascript";
        return "application/octet-stream";
    }

    private static void sendSimpleResponse(OutputStream out, String status, String body) throws IOException {
        byte[] bodyBytes = body.getBytes("UTF-8");
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"), false);
        writer.printf("HTTP/1.1 %s\r\n", status);
        writer.printf("Content-Type: text/html; charset=UTF-8\r\n");
        writer.printf("Content-Length: %d\r\n", bodyBytes.length);
        writer.printf("Connection: close\r\n");
        writer.printf("\r\n");
        writer.flush();
        out.write(bodyBytes);
        out.flush();
    }
}
