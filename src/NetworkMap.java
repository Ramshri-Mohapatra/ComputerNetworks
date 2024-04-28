import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class NetworkMap {
    private Map<byte[], String> map;

    public NetworkMap() {
        this.map = new HashMap<>();
    }

    public void addNode(byte[] keyHash, String address) {
        map.put(keyHash, address);
    }

    public void removeNode(byte[] keyHash) {
        map.remove(keyHash);
    }

    public String getAddress(byte[] keyHash) {
        return map.get(keyHash);
    }

    public Iterable<byte[]> getKeyHashes() {
        return map.keySet();
    }
}