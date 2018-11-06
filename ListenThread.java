import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

class ListenThread extends Thread {
    private DatagramSocket socket;

    public ListenThread() {
        try {
            socket = new DatagramSocket(8000);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (true) {
            byte[] buf = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String address = packet.getAddress().toString();
            // Add origin address of packet to shared list of known peers

            String[] msg = (new String(packet.getData())).trim().split(",");

            if (msg[0].equals("query")) {
                // Reply with list of available chunks
            } else if (msg[0].equals("request")) {
                // Get batch of requested chunks
                // Start new UploadThread to send chunks to requester
            } else if (msg[0].equals("list")) {
                // Store peer's list of chunks together with ip address
            }
        }
    }
}