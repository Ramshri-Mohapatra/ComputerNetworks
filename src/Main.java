import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter your email address:");
        String email = scanner.nextLine();

        System.out.println("Enter a unique identifier for your node:");
        String identifier = scanner.nextLine();

        String nodeName = email + ":" + identifier;

        System.out.println("Do you want to start a FullNode or a TemporaryNode? (F/T)");
        String nodeType = scanner.nextLine().toUpperCase();

        if (nodeType.equals("F")) {
            System.out.println("Enter the IP address and port number for the FullNode (e.g., 127.0.0.1:4567):");
            String address = scanner.nextLine();

            FullNode fullNode = new FullNode();
            fullNode.setNodeName(nodeName, address);
            boolean success = fullNode.listen(address.split(":")[0], Integer.parseInt(address.split(":")[1]));

            if (success) {
                System.out.println("FullNode is listening on " + address);
                fullNode.handleIncomingConnections(nodeName, address);
            } else {
                System.out.println("Failed to start the FullNode.");
            }
        } else if (nodeType.equals("T")) {
            System.out.println("Enter the address of the starting FullNode (e.g., 127.0.0.1:4567):");
            String startingNodeAddress = scanner.nextLine();

            TemporaryNode tempNode = new TemporaryNode();
            tempNode.setNodeName(nodeName);
            boolean connected = tempNode.start(nodeName, startingNodeAddress);

            if (connected) {
                System.out.println("TemporaryNode connected to the network.");
                handleTemporaryNodeCommands(tempNode, scanner);
            } else {
                System.out.println("Failed to connect TemporaryNode to the network.");
            }
        } else {
            System.out.println("Invalid node type.");
        }

        scanner.close();
    }

    private static void handleTemporaryNodeCommands(TemporaryNode tempNode, Scanner scanner) {
        while (true) {
            System.out.println("Enter a command (PUT, GET, or END):");
            String command = scanner.nextLine().toUpperCase();

            if (command.equals("PUT")) {
                System.out.println("Enter the number of lines for the key:");
                int keyLines = Integer.parseInt(scanner.nextLine());
                System.out.println("Enter the key:");
                StringBuilder keyBuilder = new StringBuilder();
                for (int i = 0; i < keyLines; i++) {
                    keyBuilder.append(scanner.nextLine()).append("\n");
                }
                String key = keyBuilder.toString();

                System.out.println("Enter the number of lines for the value:");
                int valueLines = Integer.parseInt(scanner.nextLine());
                System.out.println("Enter the value:");
                StringBuilder valueBuilder = new StringBuilder();
                for (int i = 0; i < valueLines; i++) {
                    valueBuilder.append(scanner.nextLine()).append("\n");
                }
                String value = valueBuilder.toString();

                boolean success = tempNode.store(key, value);
                if (success) {
                    System.out.println("Value stored successfully.");
                } else {
                    System.out.println("Failed to store the value.");
                }
            } else if (command.equals("GET")) {
                System.out.println("Enter the key:");
                String key = scanner.nextLine();

                String value = tempNode.get(key);
                if (value != null) {
                    System.out.println("Value: " + value);
                } else {
                    System.out.println("Value not found.");
                }
            } else if (command.equals("END")) {
                break;
            } else {
                System.out.println("Invalid command.");
            }
        }
    }
}