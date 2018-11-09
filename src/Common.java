import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class Common {
    private static void sendMessage(DatagramSocket socket, String peer, String type, List<String> data) {
        String msg = type + ",";
        if (data != null) {
            msg += String.join(",", data);
        }
        byte[] msgBytes = msg.getBytes();
        try {
            InetAddress address = InetAddress.getByName(peer);
            DatagramPacket packet = new DatagramPacket(msgBytes, msgBytes.length, address, 8000);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void sendQuery(DatagramSocket socket, String peer) {
        sendMessage(socket, peer, "query", null);
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
}