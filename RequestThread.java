import java.net.DatagramSocket;

class RequestThread extends Thread {
    private DatagramSocket socket;

    public RequestThread() {
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (true) {
            if () {  // Check if list of peers is empty
                // Send 'query' message to broadcast address to get list of chunks from all peers
            } else {
                // If not currently downloading from any peer,
                // check which chunks peer has but we do not
                // create batch of chunks based on scarcest-first algo
                // send 'request' message to peer to initiate file transfer
            }
        }
    }
}