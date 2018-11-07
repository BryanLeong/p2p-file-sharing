import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

class FileIOThread extends Thread {
    private ConcurrentHashMap<String, byte[]> chunkMap;
    private int chunkSize;

    public FileIOThread(int chunkSize,
                        ConcurrentHashMap<String, byte[]> chunkMap) {
        this.chunkSize = chunkSize;
        this.chunkMap = chunkMap;
    }

    private void populateChunkMap() {
        File folder = new File("files");
        for (File file : folder.listFiles()) {
            String fileName = file.getName();
            try {
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                int totalChunks = (int) Math.ceil(1.0 * fileBytes.length / chunkSize);
                int start = 0, end;
                for (int i = 1; i <= totalChunks; i++) {
                    if (start + chunkSize < fileBytes.length) {
                        end = start + chunkSize;
                    } else {
                        end = fileBytes.length;
                    }
                    String chunkId = String.format("%s/%d/%d", fileName, totalChunks, i);
                    byte[] chunk = Arrays.copyOfRange(fileBytes, start, end);
                    chunkMap.put(chunkId, chunk);
                    start += chunkSize;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void run() {
        populateChunkMap();
    }
}
