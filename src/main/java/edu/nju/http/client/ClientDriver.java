package edu.nju.http.client;

import edu.nju.http.message.HttpRequest;
import edu.nju.http.message.HttpResponse;
import edu.nju.http.message.MIME;
import edu.nju.http.message.constant.Header;
import edu.nju.http.utils.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;
import java.util.Vector;

/**
 * ClientDriver - 简单的客户端交互接口
 */
public class ClientDriver {
    private static final HttpClient client = new HttpClient();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        Log.init(Config.LOG_LEVEL);

        System.out.println("Welcome to the HTTP Client!");
        help();
        boolean running = true;

        while (running) {
            String inputLine = scanner.nextLine().trim();
            if (inputLine.isEmpty()) {
                continue;
            }

            String[] tokens = parseLine(inputLine);
            if (tokens.length == 0) {
                continue;
            }

            String command = tokens[0].toLowerCase();

            switch (command) {
                case "send":
                    send(tokens);
                    break;
                case "disconnect":
                    disconnect(tokens);
                    break;
                case "stop":
                    stop();
                    break;
                case "exit":
                    running = false;
                    stop();
                    System.out.println("Goodbye!");
                    break;
                case "help":
                    help();
                    break;
                default:
                    System.out.println("Unknown command: " + command);
            }
        }

        scanner.close();
    }

    private static String[] parseLine(String inputLine) {
        Vector<String> tokensList = new Vector<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < inputLine.length(); i++) {
            char c = inputLine.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (!inQuotes && Character.isWhitespace(c)) {
                if (sb.length() > 0) {
                    tokensList.add(sb.toString());
                    sb.setLength(0);
                }
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0) {
            tokensList.add(sb.toString());
        }

        return tokensList.toArray(new String[0]);
    }

    /**
     * 发送 HTTP 请求
     */
    private static void send(String[] tokens) {
        if (tokens.length < 2) {
            System.out.println("Usage: send <host>[:<port>][/<path>][?<query>] [-m <method>] [-h <header>:<value> ...] [-b \"<plain text>\"]");
            return;
        }

        if(tokens[1].contains("://")){
            if(!tokens[1].startsWith("http://")){
                System.out.println("Unsupported protocol. Please use HTTP.");
                return;
            }
        } else {
            tokens[1] = "http://" + tokens[1];
        }

        HttpRequest request = new HttpRequest();
        request.setTarget(tokens[1]);
        setCommonHeaders(request);

        String[] hostPort = request.getHeaderVal(Header.Host).split(":");
        String host = hostPort[0];
        int port = hostPort.length == 2 ? Integer.parseInt(hostPort[1]) : 80;

        for (int i = 2; i < tokens.length; i++) {
            String opt = tokens[i];
            switch (opt) {
                case "-m":
                    if (++i < tokens.length) {
                        request.setMethod(tokens[i].toUpperCase());
                    } else {
                        System.out.println("Missing value for -m <method>");
                    }
                    break;
                case "-h":
                    while (++i < tokens.length && !tokens[i].startsWith("-")) {
                        String[] headerParts = tokens[i].split(":", 2);
                        if (headerParts.length == 2) {
                            request.setHeader(headerParts[0].trim(), headerParts[1].trim());
                        } else {
                            System.out.println("Invalid header format: " + tokens[i] + ". Expected key:value");
                        }
                    }
                    i--;
                    break;
                case "-b":
                    if (++i < tokens.length) {
                        String bodyToken = tokens[i];
                        if (bodyToken.startsWith("\"") && bodyToken.endsWith("\"") && bodyToken.length() >= 2) {
                            bodyToken = bodyToken.substring(1, bodyToken.length() - 1);
                        }
                        request.setBody(bodyToken);
                    } else {
                        System.out.println("Missing value for -b <plain text>");
                    }
                    break;
                default:
                    System.out.println("Unknown option: " + tokens[i]);
                    break;
            }
        }

        System.out.println("\n--- HTTP Request ---");
        System.out.println(request);

        try {
            HttpResponse response = client.send(host, port, request);

            if (response != null) {
                System.out.println("\n--- HTTP Response ---");
                System.out.println(response.getStartLine());
                System.out.println(response.getFormattedHeaders());

                String contentType = response.getHeaderVal("Content-Type").split(";")[0];
                if (MIME.isTextType(contentType == null ? "" : contentType)
                        && Integer.parseInt(response.getHeaderVal("Content-Length")) < Config.MAX_DISPLAY_SIZE) {
                    System.out.println("[Body]:\n" + response.getBodyAsString());
                } else {
                    String target = request.getTarget();
                    if (target == null || target.isEmpty() || target.equals("/")) {
                        target = "response";
                    }
                    saveData(contentType, response.getBody(), target);
                }
            } else {
                System.out.println("Failed to receive a valid response.");
            }
        } catch (Exception e) {
            Log.error("Client", "Failed to send HTTP request.", e);
        }
    }

    /**
     * 断开连接
     */
    private static void disconnect(String[] tokens) {
        if (tokens.length < 2) {
            System.out.println("Usage: disconnect <host>:<port>");
            return;
        }
        String[] hostPort = tokens[1].split(":");
        if (hostPort.length != 2) {
            System.out.println("Invalid host:port format. Example: localhost:8080");
            return;
        }
        String host = hostPort[0];
        int port;
        try {
            port = Integer.parseInt(hostPort[1]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid port. Please enter a valid number.");
            return;
        }

        client.disconnect(host, port);
    }

    private static void stop() {
        client.stop();
        System.out.println(".");
    }

    private static void help() {
        System.out.println("1. send <host>[:<port>][/<path>][?<query>] [-m <method>] [-h <header>:<value> ...] [-b \"<body>\"]");
        System.out.println("2. disconnect <host>:<port>");
        System.out.println("3. stop");
        System.out.println("4. exit");
        System.out.println("5. help");
    }

    /**
     * 设置通用头部信息
     */
    private static void setCommonHeaders(HttpRequest request) {
        request.setHeader(Header.User_Agent, Config.USER_AGENT);
        if (Config.KEEP_ALIVE) {
            request.setHeader(Header.Connection, "keep-alive");
        } else {
            request.setHeader(Header.Connection, "close");
        }
        if (Config.ENABLE_CACHE) {
            request.setHeader(Header.Cache_Control, Config.CACHE_CONTROL);
        }
    }

    /**
     * 将二进制数据存储到 Config.DATA_DIR 下
     */
    private static void saveData(String type, byte[] data, String path) {
        if (data == null || data.length == 0) {
            System.out.println("No binary data to save.");
            return;
        }

        String[] pathParts = path.split("/");
        String fileName = pathParts[pathParts.length - 1];
        if (!fileName.matches(".*\\.[a-zA-Z0-9]+$")) {
            fileName = fileName + "." + MIME.toExtension(type);
        }

        File outFile = getFile(fileName);

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(data);
            fos.flush();
            System.out.println("Binary data saved to: " + outFile.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("Failed to save binary data: " + e.getMessage());
        }
    }

    private static File getFile(String fileName) {
        File outFile = new File(Config.DATA_DIR, fileName);
        String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        String extension = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : "";

        int counter = 1;
        while (outFile.exists()) {
            outFile = new File(Config.DATA_DIR, baseName + "(" + counter + ")" + extension);
            counter++;
        }

        if (!outFile.getParentFile().exists()) {
            outFile.getParentFile().mkdirs();
        }
        return outFile;
    }

}
