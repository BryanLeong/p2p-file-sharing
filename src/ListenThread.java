import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

class ListenThread extends Thread {
    private DatagramSocket socket;
    private ConcurrentHashMap<String, byte[]> chunkMap;
    private ConcurrentHashMap<String, Set<String>> peerMap;

    public ListenThread(InetAddress localAddress, ConcurrentHashMap<String, byte[]> chunkMap, ConcurrentHashMap<String, Set<String>> peerMap) {
        this.chunkMap = chunkMap;
        this.peerMap = peerMap;
        try {
            socket = new DatagramSocket(8000, localAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void storePeerChunkList(String peer, String data) {
        Set<String> peerChunkSet;
        if (!data.isEmpty()) {
            peerChunkSet = new HashSet<>(Arrays.asList(data.split(",")));
        } else {
            peerChunkSet = new HashSet<>();
        }
        if (peerMap.containsKey(peer)) {
            peerMap.replace(peer, peerChunkSet);
        } else {
            peerMap.put(peer, peerChunkSet);
        }
    }

    public void run() {
        String peer;
        String[] data;
        DatagramPacket packet;

        while (true) {
            byte[] buf = new byte[2048];
            packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
                if (packet.getAddress().equals(InetAddress.getLocalHost())) {
                    continue;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            peer = packet.getAddress().getHostAddress();
            data = (new String(packet.getData())).trim().split(",", 2);

            System.out.println("Received packet from: " + peer);
            System.out.println("Packet type: " + data[0]);
            System.out.println("Data: " + data[1]);

            switch (data[0]) {
                case "query":
                    // Store peer's list of chunks
                    storePeerChunkList(peer, data[1]);
                    // Reply with list of available chunks
                    String reply = "list," + chunkMap.keySet().toString().replaceAll("\\[|\\]", "");
                    byte[] replyBytes = reply.getBytes();
                    try {
                        InetAddress address = InetAddress.getByName(peer);
                        packet = new DatagramPacket(replyBytes, replyBytes.length, address, 8000);
                        socket.send(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case "request":
                    // Get batch of requested chunks and start new UploadThread to send chunks to requester
                    if (!data[1].isEmpty()) {
                        Thread uploadThread = new UploadThread(chunkMap, peer, data[1].split(","));
                        uploadThread.start();
                    }
                    break;
                case "list":
                    // Store peer's list of chunks
                    storePeerChunkList(peer, data[1]);
                    break;
                default:
                    break;
            }
        }
    }
}