package main.file.system;

import reactor.core.publisher.Mono;

import java.io.RandomAccessFile;

public class ActiveTorrentFile implements TorrentFile {
    private String filePath;
    private long from, to; // not closed range: [from,to).
    private RandomAccessFile randomAccessFile;

    public ActiveTorrentFile(String filePath, long from, long to) {
        this.filePath = filePath;
        this.from = from;
        this.to = to;
    }

    public long getFrom() {
        return from;
    }

    public long getTo() {
        return to;
    }

    // as implied here: https://stackoverflow.com/questions/45396252/concurrency-of-randomaccessfile-in-java/45490504
    // something can go wrong if multiple threads try to read/write concurrently.
    public synchronized Mono<ActiveTorrent> writeBlock(int begin, byte[] block) {
        return Mono.empty();
    }


    // as implied here: https://stackoverflow.com/questions/45396252/concurrency-of-randomaccessfile-in-java/45490504
    // something can go wrong if multiple threads try to read/write concurrently.
    public synchronized Mono<byte[]> readBlock(int begin, int blockLength) {
        //if (!havePiece(pieceIndex))
        //Mono.error(new IllegalStateException("requested block of pieced we don't have yet: " + pieceIndex));
        return Mono.empty();
    }

    @Override
    public String getFilePath() {
        return this.filePath;
    }

    @Override
    public long getLength() {
        return this.to - this.from;
    }
}
