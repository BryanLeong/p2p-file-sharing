import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;

class Common {
    public static void updatePeerWithChunkList(DatagramSocket socket,
                                               ConcurrentHashMap<String, byte[]> chunkMap,
                                               String peer) {
        String msg = "list," + String.join(",", chunkMap.keySet());
        byte[] msgBytes = msg.getBytes();
        try {
            InetAddress address = InetAddress.getByName(peer);
            DatagramPacket packet = new DatagramPacket(msgBytes, msgBytes.length, address, 8000);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
