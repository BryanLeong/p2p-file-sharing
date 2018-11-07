import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

class FileIOThread extends Thread {
    private ConcurrentHashMap<String, byte[]> chunkMap;
    private File folder;
    private List<File> fileList;
    private int chunkSize;

    public FileIOThread(int chunkSize,
                        ConcurrentHashMap<String, byte[]> chunkMap) {
        this.chunkSize = chunkSize;
        this.chunkMap = chunkMap;
        folder = new File("files");
        fileList = Arrays.asList(folder.listFiles());
        populateChunkMap();
    }

    private void populateChunkMap() {
        for (File file : fileList) {
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
                    if (!chunkMap.containsKey(chunkId)) {
                        chunkMap.put(chunkId, chunk);
                    }
                    start += chunkSize;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void run() {
        while (true) {
            // update chunkList if new files are added
            List<File> newFileList = Arrays.asList(folder.listFiles());
            if (!fileList.containsAll(newFileList)) {
                fileList = newFileList;
                populateChunkMap();
            }

            // if we have all chunks of a file, write it to disk
            // else store chunks in a temp file on disk (update populateChunkMap() to read these as chunks as well)

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
