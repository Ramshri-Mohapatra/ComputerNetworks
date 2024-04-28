// IN2011 Computer Networks
// Coursework 2023/2024
//
// Submission by
// YOUR_NAME_GOES_HERE
// YOUR_STUDENT_ID_NUMBER_GOES_HERE
// YOUR_EMAIL_GOES_HERE
import java.io.*;
import java.net.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

// DO NOT EDIT starts
interface FullNodeInterface {
    public boolean listen(String ipAddress, int portNumber);
    public void handleIncomingConnections(String startingNodeName, String startingNodeAddress);
}
// DO NOT EDIT ends




class NodeInfo {
    private String nodeName;
    private String nodeAddress;
    private byte[] hashID;

    public NodeInfo(String nodeName, String nodeAddress) {
        this.nodeName = nodeName;
        this.nodeAddress = nodeAddress;
        try {
            this.hashID = HashID.computeHashID(nodeName + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getNodeAddress() {
        return nodeAddress;
    }

    public byte[] getHashID() {
        return hashID;
    }
}

public class FullNode implements FullNodeInterface {

    private ServerSocket serverSocket;
    private Map<Integer, List<NodeInfo>> networkMap;
    private String startingNodeName;
    private String startingNodeAddress;
    private byte[] startingNodeHashID;
    private Map<String, String> keyValueStore;

    public FullNode() {

        try {
            this.startingNodeHashID = HashID.computeHashID(startingNodeName + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
        networkMap = new HashMap<>();
        keyValueStore = new HashMap<>();
        updateNetworkMap(startingNodeName, startingNodeAddress);
    }

    public void setNodeName(String nodeName, String nodeAddress) {
        this.startingNodeName = nodeName;
        this.startingNodeAddress = nodeAddress;
        try {
            this.startingNodeHashID = HashID.computeHashID(nodeName + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Update the network map after setting the name
        updateNetworkMap(nodeName, nodeAddress);
    }

    // Setter method for setting the address


    public boolean listen(String ipAddress, int portNumber) {
        try {
            serverSocket = new ServerSocket(portNumber, 50, InetAddress.getByName(ipAddress));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void handleIncomingConnections(String startingNodeName, String startingNodeAddress) {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                // Send confirmation message to the client
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                writer.println("START"); // Sending START message to confirm connection
                new Thread(new ClientHandler(clientSocket, startingNodeName, startingNodeAddress)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader reader;
        private PrintWriter writer;
        private String startingNodeName;
        private String startingNodeAddress;

        public ClientHandler(Socket clientSocket, String startingNodeName, String startingNodeAddress) {
            this.clientSocket = clientSocket;
            this.startingNodeName = startingNodeName;
            this.startingNodeAddress = startingNodeAddress;
            try {
                reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                writer = new PrintWriter(clientSocket.getOutputStream(), true);
                writer.println("START 1 " + startingNodeName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                String message;
                while ((message = reader.readLine()) != null) {
                    System.out.println("Received message from client: " + message);

                    if (message.equals("END")) {
                        clientSocket.close();
                        break;
                    } else if (message.startsWith("PUT?")) {
                        handlePutRequest(message);
                    } else if (message.startsWith("GET?")) {
                        handleGetRequest(message);
                    } else if (message.equals("NOTIFY?")) {
                        handleNotifyRequest();
                    } else if (message.startsWith("NEAREST?")) {
                        handleNearestRequest(message);
                    } else if (message.equals("ECHO?")) {
                        writer.println("OHCE");
                    } else {
                        writer.println("Unknown command");
                    }
                }
            } catch (SocketException e) {
                System.out.println("Connection reset by peer.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void handlePutRequest(String message) throws IOException {
            String[] parts = message.split(" ");
            int keyLines = Integer.parseInt(parts[1]);
            int valueLines = Integer.parseInt(parts[2]);

            StringBuilder keyBuilder = new StringBuilder();
            for (int i = 0; i < keyLines; i++) {
                keyBuilder.append(reader.readLine()).append("\n");
            }
            String key = keyBuilder.toString();

            StringBuilder valueBuilder = new StringBuilder();
            for (int i = 0; i < valueLines; i++) {
                valueBuilder.append(reader.readLine()).append("\n");
            }
            String value = valueBuilder.toString();

            if (shouldStoreValue(key)) {
                storeKeyValue(key, value);
                writer.println("SUCCESS");
            } else {
                writer.println("FAILED");
            }
        }

        private void handleGetRequest(String message) throws IOException {
            String[] parts = message.split(" ");
            int keyLines = Integer.parseInt(parts[1]);
            String key = readLines(reader, keyLines);

            String value = getValueForKey(key);
            if (value != null) {
                writer.println("VALUE " + value.split("\n").length);
                writer.println(value);
            } else {
                writer.println("NOPE");
            }
        }

        private void handleNotifyRequest() throws IOException {
            String nodeName = reader.readLine();
            String nodeAddress = reader.readLine();
            updateNetworkMap(nodeName, nodeAddress);
            writer.println("NOTIFIED");
        }

        private void handleNearestRequest(String message) throws IOException {
            String[] parts = message.split(" ");
            byte[] hashID = hexStringToByteArray(parts[1]);
            List<NodeInfo> closestNodes = findClosestNodes(hashID);
            writer.println("NODES " + closestNodes.size());
            for (NodeInfo nodeInfo : closestNodes) {
                writer.println(nodeInfo.getNodeName());
                writer.println(nodeInfo.getNodeAddress());
            }
        }
    }

    private boolean shouldStoreValue(String key) {
        byte[] keyHashID = null;
        try {
            keyHashID = HashID.computeHashID(key + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<NodeInfo> closestNodes = findClosestNodes(keyHashID);

        for (NodeInfo nodeInfo : closestNodes) {
            if (Arrays.equals(nodeInfo.getHashID(), startingNodeHashID)) {
                return true;
            }
        }

        return false;
    }

    private String getValueForKey(String key) {
        return keyValueStore.get(key);
    }

    private void storeKeyValue(String key, String value) {
        keyValueStore.put(key, value);
        System.out.println("Storing key: " + key + ", value: " + value);
    }

    private List<NodeInfo> findClosestNodes(byte[] hashID) {
        List<NodeInfo> closestNodes = new ArrayList<>();
        int minDistance = Integer.MAX_VALUE;
        for (Map.Entry<Integer, List<NodeInfo>> entry : networkMap.entrySet()) {
            int distance = entry.getKey();
            if (distance < minDistance) {
                minDistance = distance;
                closestNodes = entry.getValue();
            }
        }
        return closestNodes;
    }

    private void updateNetworkMap(String nodeName, String nodeAddress) {
        NodeInfo nodeInfo = new NodeInfo(nodeName, nodeAddress);
        byte[] nodeHashID = nodeInfo.getHashID();
        int distance = getDistance(startingNodeHashID, nodeHashID);

        List<NodeInfo> nodes = networkMap.getOrDefault(distance, new ArrayList<>());
        if (nodes.size() < 3) {
            nodes.add(nodeInfo);
        } else {
            // Implement logic to select the three most reliable nodes
            // For now, just replace the first node
            nodes.set(0, nodeInfo);
        }
        networkMap.put(distance, nodes);
    }

    private int getDistance(byte[] hashID1, byte[] hashID2) {
        int distance = 0;
        for (int i = 0; i < hashID1.length; i++) {
            byte b1 = hashID1[i];
            byte b2 = hashID2[i];
            int bitsDifferent = Integer.bitCount(b1 ^ b2);
            distance += bitsDifferent;
        }
        return distance;
    }

    private String readLines(BufferedReader reader, int numLines) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numLines; i++) {
            sb.append(reader.readLine()).append("\n");
        }
        return sb.toString();
    }

    private byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }
}