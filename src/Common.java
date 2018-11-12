import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;

class Common {
    private static void sendMessage(DatagramSocket socket, String peer, String type, List<String> data) {
        String msg = type + ",";
        if (data != null) {
            msg += String.join(",", data);
        }
        byte[] msgBytes = msg.getBytes();
        try {
            InetAddress address = InetAddress.getByName(peer);
            DatagramPacket packet = new DatagramPacket(msgBytes, msgBytes.length, address, 8001);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void sendQuery(DatagramSocket socket, String peer) {
        sendMessage(socket, peer, "query", null);
    }

    static void replyQuery(DatagramSocket socket, String peer) {
        sendMessage(socket, peer, "hello", null);
    }

    static void sendChunkList(DatagramSocket socket, String peer, Set<String> chunkSet) {
        List<String> chunkList = new ArrayList<>(chunkSet);
        sendChunkList(socket, peer, chunkList);
    }

    static void sendChunkList(DatagramSocket socket, String peer, List<String> chunkList) {
        int batchSize = 10;
        int totalMessages = (int) Math.ceil(1.0 * chunkList.size() / batchSize);
        int start = 0, end;
        for (int i = 0; i < totalMessages; i++) {
            if (start + batchSize < chunkList.size()) {
                end = start + batchSize;
            } else {
                end = chunkList.size();
            }
            sendMessage(socket, peer, "list", chunkList.subList(start, end));
            start += batchSize;
        }
    }

    static void sendRequest(DatagramSocket socket, String peer, Set<String> chunkSet) {
        sendMessage(socket, peer, "request", new ArrayList<>(chunkSet));
    }

    // Returns null if filename is wrong
    // Assuming data is in the form "filename/no_of_chunks/chunks"
    //  e.g. "file.txt/12/1"
    //      "file.txt/12/2"
    //      "file.txt/12/10"
    static String unpackChunk (String data, String fileName) {
        List<String> chunkList = new ArrayList<>();
        String[] parts = data.split("/");
        if (parts.length != 3)
            return null;
        return parts[2];
    }

    // Should be used more often than the one on top
    // Assuming data is in the form "filename/no_of_chunks/chunks"
    //  e.g. "file.txt/12/1"
    //      "file.txt/12/2"
    //      "file.txt/12/10"
    static Set<String> unpackChunks (Set<String> data, String fileName) {
        Set<String> chunkSet = new HashSet<>();
        for (String file : data){
            String[] parts = file.split("/");
            if (parts.length != 3)
                continue;
            chunkSet.add(parts[2]);
        }
        return chunkSet;
    }

}