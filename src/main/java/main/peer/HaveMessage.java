package main.peer;

import java.nio.ByteBuffer;

public class HaveMessage extends PeerMessage {
    private static final int length = 5;
    private static final byte messageId = 4;

    /**
     * The payload is the zero-based index
     * of a piece that has just been successfully
     * downloaded and verified via the hash.
     *
     * @param pieceIndex is the piece (not block, which is a piece inside a piece) we tell the other peers we have.
     */
    public HaveMessage(int pieceIndex) {
        super(length, messageId, ByteBuffer.allocate(4).putInt(pieceIndex).array());
    }
}
