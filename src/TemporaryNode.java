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

// DO NOT EDIT starts
interface TemporaryNodeInterface {
    public boolean start(String startingNodeName, String startingNodeAddress);
    public boolean store(String key, String value);
    public String get(String key);
}
// DO NOT EDIT ends



public class TemporaryNode implements TemporaryNodeInterface {
    private String startingNodeName;
    private String startingNodeAddress;
    private byte[] startingNodeHashID;
    private String nodeName;
    private Map<Integer, List<NodeInfo>> networkMap;

    public TemporaryNode() {
        networkMap = new HashMap<>();
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
        try {
            this.startingNodeHashID = HashID.computeHashID(nodeName + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean start(String startingNodeName, String startingNodeAddress) {
        try {
            this.startingNodeName = startingNodeName;
            this.startingNodeAddress = startingNodeAddress;

            Socket socket = new Socket(startingNodeAddress.split(":")[0], Integer.parseInt(startingNodeAddress.split(":")[1]));
            System.out.println("TemporaryNode connecting to " + startingNodeAddress);

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            writer.println("START 1 " + nodeName);
            String response = reader.readLine();
            if (response.startsWith("START")) {
                updateNetworkMap(startingNodeName, startingNodeAddress);
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean store(String key, String value) {
        try {
            // Connect to the FullNode
            Socket socket = new Socket(startingNodeAddress.split(":")[0], Integer.parseInt(startingNodeAddress.split(":")[1]));
            System.out.println("TempNode connecting to " + startingNodeAddress);
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send PUT request with key and value
            writer.println("PUT? " + key.split("\n").length + " " + value.split("\n").length);
            writer.println(key);
            writer.println(value);

            // Receive response from the FullNode
            String response = reader.readLine();
            System.out.println("Response from full node: " + response); // Add logging

            // Close the socket (optional)
            socket.close();

            // Check if the store operation was successful
            if (response.equals("SUCCESS")) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String get(String key) {
        try {
            List<NodeInfo> closestFullNodes = findClosestFullNodes(key);
            for (NodeInfo nodeInfo : closestFullNodes) {
                Socket socket = new Socket(nodeInfo.getNodeAddress().split(":")[0], Integer.parseInt(nodeInfo.getNodeAddress().split(":")[1]));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                writer.println("GET? " + key.split("\n").length);
                writer.println(key);

                String response = reader.readLine();
                if (response.startsWith("VALUE")) {
                    int valueLines = Integer.parseInt(response.split(" ")[1]);
                    String value = readLines(reader, valueLines);
                    socket.close();
                    return value;
                }
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<NodeInfo> findClosestFullNodes(String key) {
        List<NodeInfo> closestNodes = new ArrayList<>();
        byte[] keyHashID = null;
        try {
            keyHashID = HashID.computeHashID(key + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }

        int minDistance = Integer.MAX_VALUE;
        for (Map.Entry<Integer, List<NodeInfo>> entry : networkMap.entrySet()) {
            int distance = entry.getKey();
            if (distance < minDistance) {
                minDistance = distance;
                closestNodes = entry.getValue();
            }
        }

        List<NodeInfo> fullNodes = new ArrayList<>();
        for (NodeInfo nodeInfo : closestNodes) {
            if (isFullNode(nodeInfo.getNodeName(), nodeInfo.getNodeAddress())) {
                fullNodes.add(nodeInfo);
            }
        }

        return fullNodes;
    }

    public boolean notify(String nodeName, String nodeAddress) {
        try {
            Socket socket = new Socket(startingNodeAddress.split(":")[0], Integer.parseInt(startingNodeAddress.split(":")[1]));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send NOTIFY? command with the node name and address
            writer.println("NOTIFY?");
            writer.println(nodeName);
            writer.println(nodeAddress);

            // Receive response from the FullNode
            String response = reader.readLine();
            socket.close();

            return response.equals("NOTIFIED");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    public String getNearestNodes(String hashIDString) {
        try {
            byte[] hashID = hexStringToByteArray(hashIDString);
            List<NodeInfo> closestNodes = findClosestNodes(hashID);
            StringBuilder nearestNodesBuilder = new StringBuilder();
            for (NodeInfo nodeInfo : closestNodes) {
                nearestNodesBuilder.append("Node Name: ").append(nodeInfo.getNodeName()).append("\n");
                nearestNodesBuilder.append("Node Address: ").append(nodeInfo.getNodeAddress()).append("\n\n");
            }
            return nearestNodesBuilder.toString();
        } catch (NumberFormatException e) {
            System.out.println("Invalid hash ID format.");
            return null;
        }
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

    public List<NodeInfo> findClosestNodes(byte[] hashID) {
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

    private boolean isFullNode(String nodeName, String nodeAddress) {
        String[] parts = nodeName.split(":");
        String email = parts[0];
        String identifier = parts[1];

        String[] addressParts = nodeAddress.split(":");
        String ip = addressParts[0];
        int port = Integer.parseInt(addressParts[1]);

        return !email.endsWith("@example.com") && (port >= 1024 && port <= 49151);
    }

    private void updateNetworkMap(String nodeName, String nodeAddress) {
        NodeInfo nodeInfo = new NodeInfo(nodeName, nodeAddress);
        byte[] nodeHashID = nodeInfo.getHashID();
        int distance = getDistance(startingNodeHashID, nodeHashID);

        List<NodeInfo> nodes = networkMap.getOrDefault(distance, new ArrayList<>());
        if (nodes.size() < 3) {
            nodes.add(nodeInfo);
        } else {
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
}