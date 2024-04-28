/*public class NetworkTest {

    public static void main(String[] args) {
        try {
            // Create a FullNode instance
            FullNode fullNode = new FullNode();

            // Start the FullNode and listen for incoming connections
            fullNode.listen("127.0.0.1", 4567);
            new Thread(() -> fullNode.handleIncomingConnections("StartingNodeName", "127.0.0.1:4567")).start();


            //Thread.sleep(10000);



            // Create a TemporaryNode instance
            TemporaryNode tempNode = new TemporaryNode();

            // Start the TemporaryNode and try to connect to the FullNode
            boolean connected = tempNode.start("StartingNodeName", "127.0.0.1:4567");


            if (connected) {
                new Thread(() -> fullNode.handleIncomingConnections("StartingNodeName", "127.0.0.1:4567")).start();

                System.out.println("Hi");
                // If connected successfully, test storing and retrieving a (key, value) pair
                String key = "TestKey";
                String value = "TestValue";

                // Store the (key, value) pair
                boolean storeResult = tempNode.store(key, value);
                if (storeResult) {
                    System.out.println("Stored successfully: " + key + " -> " + value);
                } else {
                    System.out.println("Failed to store: " + key + " -> " + value);
                }

                // Retrieve the value corresponding to the key
                String retrievedValue = tempNode.get(key);
                if (retrievedValue != null) {
                    System.out.println("Retrieved value for key " + key + ": " + retrievedValue);
                } else {
                    System.out.println("No value found for key " + key);
                }
            } else {
                System.out.println("Failed to connect to the network.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
*/