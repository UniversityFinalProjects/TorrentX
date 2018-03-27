package main.file.system;

import christophedetroyer.torrent.TorrentFile;
import main.TorrentInfo;
import main.peer.peerMessages.PieceMessage;
import main.peer.peerMessages.RequestMessage;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;

public class ActiveTorrent extends TorrentInfo {

    private final List<ActiveTorrentFile> activeTorrentFileList;
    private final BitSet piecesStatus;
    private final long[] piecesPartialStatus;
    private final String downloadPath;

    public ActiveTorrent(TorrentInfo torrentInfo, String downloadPath) throws FileNotFoundException {
        super(torrentInfo);
        this.downloadPath = downloadPath;
        this.piecesStatus = new BitSet(getPieces().size());
        this.piecesPartialStatus = new long[getPieces().size()];
        this.activeTorrentFileList = createActiveTorrentFileList(torrentInfo, downloadPath);
    }

    public List<? extends main.file.system.TorrentFile> getTorrentFiles() {
        return this.activeTorrentFileList;
    }

    public BitSet getPiecesStatus() {
        return this.piecesStatus;
    }

    public Mono<ActiveTorrent> writeBlock(PieceMessage pieceMessage) {
        return Mono.<ActiveTorrent>create(sink -> {
            long from = pieceMessage.getIndex() * this.getPieceLength() + pieceMessage.getBegin();
            long to = pieceMessage.getIndex() * this.getPieceLength()
                    + pieceMessage.getBegin() + pieceMessage.getBlock().length;
            int arrayIndexFrom = 0; // where ActiveTorrentFile object needs to write from in the array.

            for (ActiveTorrentFile activeTorrentFile : this.activeTorrentFileList)
                if (activeTorrentFile.getFrom() <= from && from <= activeTorrentFile.getTo()) {
                    int howMuchToWriteFromArray = (int) (Math.min(to, activeTorrentFile.getTo()) - from);
                    try {
                        activeTorrentFile.writeBlock(from, pieceMessage.getBlock(), arrayIndexFrom, howMuchToWriteFromArray);
                    } catch (IOException e) {
                        sink.error(e);
                        return;
                    }
                    // increase 'from' because next time we will write to different position.
                    from += howMuchToWriteFromArray;
                    arrayIndexFrom += howMuchToWriteFromArray;
                    if (from == to)
                        break;
                }

            // update pieces partial status array:
            // WARNING: this line *only* must be synchronized among multiple threads!
            this.piecesPartialStatus[pieceMessage.getIndex()] += pieceMessage.getBlock().length;

            // update pieces status:
            // there maybe multiple writes of the same pieceRequest during one execution...
            if (this.piecesPartialStatus[pieceMessage.getIndex()] >= this.getPieceLength()) {
                this.piecesStatus.set(pieceMessage.getIndex());
                System.out.println("piece completed: " + pieceMessage.getIndex());
            }
            sink.success(this);
        }).subscribeOn(Schedulers.single());
    }

    public Mono<byte[]> readBlock(RequestMessage requestMessage) {
        return Mono.<byte[]>create(sink -> {
            if (!havePiece(requestMessage.getIndex())) {
                sink.error(new PieceNotFoundException(requestMessage.getIndex()));
                return;
            }
            byte[] result = new byte[requestMessage.getBlockLength()];
            int freeIndexInResultArray = 0;

            long from = requestMessage.getIndex() * this.getPieceLength() + requestMessage.getBegin();
            long to = requestMessage.getIndex() * this.getPieceLength()
                    + requestMessage.getBegin() + requestMessage.getBlockLength();

            for (ActiveTorrentFile activeTorrentFile : this.activeTorrentFileList) {
                if (activeTorrentFile.getFrom() <= from && from <= activeTorrentFile.getTo()) {
                    int howMuchToReadFromThisFile = (int) Math.min(requestMessage.getBlockLength(), (from - to));
                    byte[] tempResult = new byte[0];
                    try {
                        tempResult = activeTorrentFile.readBlock(from, howMuchToReadFromThisFile);
                    } catch (IOException e) {
                        sink.error(e);
                        return;
                    }
                    for (int i = 0; i < tempResult.length; i++)
                        result[freeIndexInResultArray++] = tempResult[i];
                    from += howMuchToReadFromThisFile;
                    if (from == to) {
                        sink.success(result);
                        return;
                    }
                }
            }
        }).subscribeOn(Schedulers.elastic());
    }

    public boolean havePiece(int pieceIndex) {
        return this.piecesStatus.get(pieceIndex);
    }

    public Mono<ActiveTorrent> updatePieceAsCompleted(int pieceIndex) {
        return Mono.<ActiveTorrent>create(sink -> {
            this.piecesStatus.set(pieceIndex);
            Mono.just(this);
        }).doOnSuccess(activeTorrent -> {
            // update in mongo db
        });
    }

    private List<ActiveTorrentFile> createActiveTorrentFileList(TorrentInfo torrentInfo, String downloadPath) throws FileNotFoundException {
        String mainFolder = !torrentInfo.isSingleFileTorrent() ?
                downloadPath + "/" + torrentInfo.getName() + "/" :
                downloadPath + "/";

        // create activeTorrentFile list
        long position = 0;
        List<ActiveTorrentFile> activeTorrentFileList = new ArrayList<>();
        for (TorrentFile torrentFile : torrentInfo.getFileList()) {
            String filePath = torrentFile
                    .getFileDirs()
                    .stream()
                    .collect(Collectors.joining("/", mainFolder, ""));
            ActiveTorrentFile activeTorrentFile =
                    new ActiveTorrentFile(filePath, position, position + torrentFile.getFileLength());
            activeTorrentFileList.add(activeTorrentFile);
            position += torrentFile.getFileLength();
        }
        return activeTorrentFileList;
    }

    public String getDownloadPath() {
        return downloadPath;
    }
}
