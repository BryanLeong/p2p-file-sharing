import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

class UploadThread extends Thread {
    private DatagramSocket socket;
    private InetAddress address;
    private ConcurrentHashMap<String, byte[]> chunkMap;
    private String[] chunkIds;

    public UploadThread(ConcurrentHashMap<String, byte[]> chunkMap,
                        String address, String[] chunkIds) {
        this.chunkMap = chunkMap;
        this.chunkIds = chunkIds;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(1000);
            this.address = InetAddress.getByName(address);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        for (int i = 0; i < 10; i++) {
            for (String chunkId : chunkIds) {
                byte[] chunk = chunkMap.get(chunkId);
                byte[] chunkIdBytes = chunkId.getBytes();

                ByteBuffer bb = ByteBuffer.allocate(8 + chunk.length + chunkIdBytes.length);
                bb.putInt(chunkIdBytes.length);
                bb.putInt(chunk.length);
                bb.put(chunkIdBytes);
                bb.put(chunk);
                byte[] data = bb.array();

                DatagramPacket packet = new DatagramPacket(data, data.length, address, 8000);
                try {
                    socket.send(packet);
                    Thread.sleep(0 + i * 10);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }

            byte[] buf = new byte[2048];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
                if ((new String(packet.getData())).contains("ack")) {
                    break;
                }
            } catch (IOException e) {
                // timed-out and did not receive ack so we resend the whole batch
                Timestamp ts = new Timestamp((new Date()).getTime());
                System.out.printf("[" + ts.toString() + "] No ACK received from " + address.getHostAddress());
                if (i != 9) {
                    System.out.println(": Resending batch with interval " + (i + 1) * 10 + "...");
                } else {
                    System.out.println(": Max retries reached.");
                }
            }
        }

    }
}