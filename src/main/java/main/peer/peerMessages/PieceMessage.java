package main.peer.peerMessages;

import main.peer.Peer;

import java.nio.ByteBuffer;

public class PieceMessage extends PeerMessage {
    private static final byte messageId=7;
    /**
     * The payload contains the following information: (by this order)
     *
     * @param index integer specifying the zero-based piece index
     * @param begin integer specifying the zero-based byte offset within the piece
     * @param block block of data, which is a subset of the piece specified by index.
     */
    public PieceMessage(Peer from, Peer to, int index, int begin, byte[] block) {
        super(to, from, 9 + block.length, messageId, ByteBuffer.allocate(4 + 4 + block.length)
                                                            .putInt(index)
                                                            .putInt(begin)
                                                            .put(block).array());
    }
    public PieceMessage(Peer from, Peer to,byte[] peerMessage) {
        super(to, peerMessage, from);
    }
    @Override
    public String toString() {
        return "PieceMessage{} " + super.toString();
    }
}