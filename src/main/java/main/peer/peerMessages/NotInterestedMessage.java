package main.peer.peerMessages;

import main.peer.Peer;
import main.peer.PeersCommunicator;

import java.nio.ByteBuffer;

public class NotInterestedMessage extends PeerMessage {
    private static final int length=1;
    private static final byte messageId=3;
    /**
     * The not interested message is fixed-length and has no payload.
     */
    public NotInterestedMessage(PeersCommunicator peersCommunicator, Peer from, Peer to) {
        super(peersCommunicator, to, from, length,messageId, ByteBuffer.allocate(0).array());
    }
    public NotInterestedMessage(PeersCommunicator peersCommunicator,Peer from, Peer to,byte[] peerMessage) {
        super(peersCommunicator, to, peerMessage, from);
    }
    @Override
    public String toString() {
        return "NotInterestedMessage{} " + super.toString();
    }
}