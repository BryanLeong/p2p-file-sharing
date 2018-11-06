import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

class ListenThread extends Thread {
    private DatagramSocket socket;
    private ConcurrentHashMap<String, byte[]> chunkMap;
    private ConcurrentHashMap<String, Set<String>> peerMap;

    public ListenThread(ConcurrentHashMap<String, byte[]> chunkMap, ConcurrentHashMap<String, Set<String>> peerMap) {
        this.chunkMap = chunkMap;
        this.peerMap = peerMap;
        try {
            socket = new DatagramSocket(8000);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private void storePeerChunkList(String peer, String data) {
        Set<String> peerChunkSet = new HashSet<String>(Arrays.asList(data.split(",")));
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
            byte[] buf = new byte[1024];
            packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            peer = packet.getAddress().toString();
            data = (new String(packet.getData())).trim().split(",", 2);

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
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "request":
                // Get batch of requested chunks
                List<byte[]> chunks = new ArrayList<byte[]>();
                byte[] chunk;
                for (String chunkNum : data[1].split(",")) {
                    chunk = chunkMap.get(chunkNum);
                    chunks.add(chunk);
                }
                // Start new UploadThread to send chunks to requester
                Thread uploadThread = new UploadThread(peer, chunks);
                uploadThread.start();
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