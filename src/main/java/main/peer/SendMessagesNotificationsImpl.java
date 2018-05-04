package main.peer;

import main.TorrentInfo;
import main.file.system.AllocatedBlock;
import main.peer.peerMessages.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.io.DataOutputStream;
import java.util.BitSet;

class SendMessagesNotificationsImpl implements SendMessagesNotifications {
    private TorrentInfo torrentInfo;
    private Peer me;
    private Peer peer;
    private Flux<PeerMessage> sentPeerMessagesFlux;
    private FluxSink<PeerMessage> sentMessagesFluxSink;
    private PeerCurrentStatus peerCurrentStatus;
    private SendMessages sendMessages;

    SendMessagesNotificationsImpl(TorrentInfo torrentInfo,
                                  Peer me, Peer peer,
                                  PeerCurrentStatus peerCurrentStatus,
                                  Runnable closeConnectionMethod,
                                  DataOutputStream peerDataOutputStream) {
        this.torrentInfo = torrentInfo;
        this.me = me;
        this.peer = peer;
        this.peerCurrentStatus = peerCurrentStatus;
        this.sentPeerMessagesFlux = Flux.create((FluxSink<PeerMessage> sink) -> this.sentMessagesFluxSink = sink);
        this.sendMessages = new SendMessages(peerDataOutputStream, closeConnectionMethod);
    }

    private Mono<SendMessagesNotifications> send(PeerMessage peerMessage) {
        return peerMessage.sendMessage(this.sendMessages)
                .map(sendMessages -> (SendMessagesNotifications) this)
                .doOnNext(peersCommunicator -> {
                    if (this.sentMessagesFluxSink != null)
                        this.sentMessagesFluxSink.next(peerMessage);
                });
    }

    @Override
    public Mono<SendMessagesNotifications> sendPieceMessage(int index, int begin,
                                                            AllocatedBlock allocatedBlock) {
        PieceMessage pieceMessage = new PieceMessage(this.getMe(), this.getPeer(),
                index, begin, allocatedBlock, this.torrentInfo.getPieceLength(index));
        return send(pieceMessage)
                .doOnNext(sendPeerMessages -> this.peerCurrentStatus.updatePiecesStatus(index));
    }

    @Override
    public Mono<SendMessagesNotifications> sendBitFieldMessage(BitSet completedPieces) {
        return send(new BitFieldMessage(this.getMe(), this.getPeer(), completedPieces))
                .doOnNext(sendPeerMessages -> this.peerCurrentStatus.updatePiecesStatus(completedPieces));
    }

    @Override
    public Mono<SendMessagesNotifications> sendCancelMessage(int index, int begin, int length) {
        return send(new CancelMessage(this.getMe(), this.getPeer(), index, begin, length));
    }

    @Override
    public Mono<SendMessagesNotifications> sendChokeMessage() {
        return send(new ChokeMessage(this.getMe(), this.getPeer()))
                .doOnNext(__ -> this.peerCurrentStatus.setAmIChokingHim(true));
    }

    @Override
    public Mono<SendMessagesNotifications> sendHaveMessage(int pieceIndex) {
        return send(new HaveMessage(this.getMe(), this.getPeer(), pieceIndex))
                .doOnNext(sendPeerMessages -> this.peerCurrentStatus.updatePiecesStatus(pieceIndex));
    }

    @Override
    public Mono<SendMessagesNotifications> sendInterestedMessage() {
        return send(new InterestedMessage(this.getMe(), this.getPeer()))
                .doOnNext(__ -> this.peerCurrentStatus.setAmIInterestedInHim(true));
    }

    @Override
    public Mono<SendMessagesNotifications> sendKeepAliveMessage() {
        return send(new KeepAliveMessage(this.getMe(), this.getPeer()));
    }

    @Override
    public Mono<SendMessagesNotifications> sendNotInterestedMessage() {
        return send(new NotInterestedMessage(this.getMe(), this.getPeer()))
                .doOnNext(__ -> this.peerCurrentStatus.setAmIInterestedInHim(false));
    }

    @Override
    public Mono<SendMessagesNotifications> sendPortMessage(short listenPort) {
        return send(new PortMessage(this.getMe(), this.getPeer(), listenPort));
    }

    @Override
    public Mono<SendMessagesNotifications> sendRequestMessage(int index, int begin, int length) {
        RequestMessage requestMessage = new RequestMessage(this.getMe(), this.getPeer(), index, begin, length);
        return send(RequestMessage.fixRequestMessage(requestMessage, this.torrentInfo.getPieceLength(requestMessage.getIndex())));
    }

    @Override
    public Mono<SendMessagesNotifications> sendRequestMessage(RequestMessage requestMessage) {
        return send(requestMessage);
    }

    @Override
    public Mono<SendMessagesNotifications> sendUnchokeMessage() {
        return send(new UnchokeMessage(this.getMe(), this.getPeer()))
                .doOnNext(__ -> this.peerCurrentStatus.setAmIChokingHim(false));
    }

    @Override
    public Flux<PeerMessage> sentPeerMessagesFlux() {
        return this.sentPeerMessagesFlux;
    }

    private Peer getPeer() {
        return peer;
    }

    private Peer getMe() {
        return me;
    }
}
