import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class Common {
    private static void sendMessage(DatagramSocket socket, String peer, String type, List<String> data) {
        sendMessage(socket, peer, 8001, type, data);
    }

    private static void sendMessage(DatagramSocket socket, String peer, int port, String type, List<String> data) {
        String msg = type + ",";
        if (data != null) {
            msg += String.join(",", data);
        }
        byte[] msgBytes = msg.getBytes();
        try {
            InetAddress address = InetAddress.getByName(peer);
            DatagramPacket packet = new DatagramPacket(msgBytes, msgBytes.length, address, port);
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

    static void sendChunkList(ConcurrentHashMap<String, Set<String>> peerMap, String peer, Set<String> chunkSet) {
        List<String> chunkList = new ArrayList<>(chunkSet);
        sendChunkList(peerMap, peer, chunkList);
    }

    static void sendChunkList(ConcurrentHashMap<String, Set<String>> peerMap, String peer, List<String> chunkList) {
        DatagramSocket socket;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(1000);

            int batchSize = 10;
            int totalMessages = (int) Math.ceil(1.0 * chunkList.size() / batchSize);
            int start = 0, end;
            for (int i = 0; i < totalMessages; i++) {
                if (start + batchSize < chunkList.size()) {
                    end = start + batchSize;
                } else {
                    end = chunkList.size();
                }
                while (true) {
                    sendMessage(socket, peer, "list", chunkList.subList(start, end));
                    byte[] buf = new byte[2048];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    try {
                        socket.receive(packet);
                        if ((new String(packet.getData())).contains("ack")) {
                            break;
                        }
                    } catch (IOException e) {
                        // timed-out and did not receive ack
                        if (!peerMap.containsKey(peer)) {
                            break;
                        }
                    }
                }
                start += batchSize;
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    static void sendRequest(DatagramSocket socket, String peer, Set<String> chunkSet) {
        sendMessage(socket, peer, "request", new ArrayList<>(chunkSet));
    }

    static void sendAck(DatagramSocket socket, String peer, int port) {
        sendMessage(socket, peer, port, "ack", null);
    }

    // Returns null if filename is wrong
    // Assuming data is in the form "filename/no_of_chunks/chunks"
    //  e.g. "file.txt/12/1"
    //      "file.txt/12/2"
    //      "file.txt/12/10"
    static String unpackChunk(String data, String fileName) {
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
    static Set<String> unpackChunks(Set<String> data, String fileName) {
        Set<String> chunkSet = new HashSet<>();
        for (String file : data) {
            String[] parts = file.split("/");
            if (parts.length != 3)
                continue;
            chunkSet.add(parts[2]);
        }
        return chunkSet;
    }

    static Set<String> removeRepeats(Set<String> originalChunks, Set<String> inputs) {
        Set<String> origChunks = new HashSet<>(originalChunks);
        origChunks.removeAll(inputs);
        return origChunks;
    }

}