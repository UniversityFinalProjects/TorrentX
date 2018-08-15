package main.peer;

import main.TorrentInfo;
import main.downloader.TorrentDownloaders;
import main.file.system.allocator.AllocatorStore;
import main.peer.peerMessages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

import java.io.DataInputStream;
import java.util.AbstractMap;

class ReceiveMessagesNotificationsImpl implements ReceiveMessagesNotifications {
    private static Logger logger = LoggerFactory.getLogger(ReceiveMessagesNotificationsImpl.class);

    private Flux<? extends PeerMessage> peerMessageResponseFlux;

    private Flux<BitFieldMessage> bitFieldMessageResponseFlux;
    private Flux<CancelMessage> cancelMessageResponseFlux;
    private Flux<ChokeMessage> chokeMessageResponseFlux;
    private Flux<ExtendedMessage> extendedMessageResponseFlux;
    private Flux<HaveMessage> haveMessageResponseFlux;
    private Flux<InterestedMessage> interestedMessageResponseFlux;
    private Flux<KeepAliveMessage> keepMessageResponseFlux;
    private Flux<NotInterestedMessage> notInterestedMessageResponseFlux;
    private Flux<PieceMessage> pieceMessageResponseFlux;
    private Flux<PortMessage> portMessageResponseFlux;
    private Flux<RequestMessage> requestMessageResponseFlux;
    private Flux<UnchokeMessage> unchokeMessageResponseFlux;

    private PeerCurrentStatus peerCurrentStatus;

    public ReceiveMessagesNotificationsImpl(AllocatorStore allocatorStore,
                                            TorrentInfo torrentInfo, Peer me, Peer peer,
                                            PeerCurrentStatus peerCurrentStatus, DataInputStream dataInputStream,
                                            String identifier,
                                            Link link,
                                            UnicastProcessor<AbstractMap.SimpleEntry<Link,PeerMessage>> incomingPeerMessages$,
                                            FluxSink<AbstractMap.SimpleEntry<Link, PeerMessage>> emitIncomingPeerMessages) {
        this.peerCurrentStatus = peerCurrentStatus;


        Scheduler scheduler = Schedulers.newSingle(identifier + " - RECEIVE-PEER-MESSAGES");


        this.peerMessageResponseFlux = Flux.generate(synchronousSink -> synchronousSink.next(0))
                .publishOn(scheduler)
                .concatMap(__ -> PeerMessageFactory.waitForMessage(allocatorStore, scheduler, torrentInfo, peer, me, dataInputStream, identifier)
                        .onErrorResume(PeerExceptions.communicationErrors, throwable -> Mono.empty()))
                .doOnNext(peerMessage -> logger.debug(identifier + " - received new message: " + peerMessage))
                //.onErrorResume(PeerExceptions.communicationErrors, throwable -> Mono.empty())
                // there are multiple subscribers to this source (every specific peer-message flux).
                // all of them must get the same message and ***not activate this source more then once***.
                .doOnNext(peerMessage -> {
                    PeerMessageId peerMessageId = PeerMessageId.fromValue(peerMessage.getMessageId());
                    switch (peerMessageId) {
                        case bitFieldMessage:
                            this.peerCurrentStatus.updatePiecesStatus(((BitFieldMessage) peerMessage).getPiecesStatus());
                            break;
                        case haveMessage:
                            this.peerCurrentStatus.updatePiecesStatus(((HaveMessage) peerMessage).getPieceIndex());
                            break;
                        case interestedMessage:
                            this.peerCurrentStatus.setIsHeInterestedInMe(true);
                            break;
                        case notInterestedMessage:
                            this.peerCurrentStatus.setIsHeInterestedInMe(false);
                            break;
                        case chokeMessage:
                            this.peerCurrentStatus.setIsHeChokingMe(true);
                            break;
                        case unchokeMessage:
                            this.peerCurrentStatus.setIsHeChokingMe(false);
                            break;
                    }
                })
                .doOnNext(peerMessage -> emitIncomingPeerMessages.next(new AbstractMap.SimpleEntry<>(link, peerMessage)))
                .doOnError(throwable -> emitIncomingPeerMessages.error(throwable))
                .publish()
                .autoConnect(0);

        this.bitFieldMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof BitFieldMessage)
                .cast(BitFieldMessage.class);

        this.cancelMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof CancelMessage)
                .cast(CancelMessage.class);

        this.chokeMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof ChokeMessage)
                .cast(ChokeMessage.class);

        this.extendedMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof ExtendedMessage)
                .cast(ExtendedMessage.class);

        this.haveMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof HaveMessage)
                .cast(HaveMessage.class);

        this.interestedMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof InterestedMessage)
                .cast(InterestedMessage.class);

        this.keepMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof KeepAliveMessage)
                .cast(KeepAliveMessage.class);

        this.notInterestedMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof NotInterestedMessage)
                .cast(NotInterestedMessage.class);

        this.pieceMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof PieceMessage)
                .cast(PieceMessage.class);

        this.portMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof PortMessage)
                .cast(PortMessage.class);

        this.requestMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof RequestMessage)
                .cast(RequestMessage.class);

        this.unchokeMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof UnchokeMessage)
                .cast(UnchokeMessage.class);
    }

    @Override
    public Flux<? extends PeerMessage> getPeerMessageResponseFlux() {
        return this.peerMessageResponseFlux;
    }

    @Override
    public Flux<BitFieldMessage> getBitFieldMessageResponseFlux() {
        return bitFieldMessageResponseFlux;
    }

    @Override
    public Flux<CancelMessage> getCancelMessageResponseFlux() {
        return cancelMessageResponseFlux;
    }

    @Override
    public Flux<ChokeMessage> getChokeMessageResponseFlux() {
        return chokeMessageResponseFlux;
    }

    @Override
    public Flux<ExtendedMessage> getExtendedMessageResponseFlux() {
        return extendedMessageResponseFlux;
    }

    @Override
    public Flux<HaveMessage> getHaveMessageResponseFlux() {
        return haveMessageResponseFlux;
    }

    @Override
    public Flux<InterestedMessage> getInterestedMessageResponseFlux() {
        return interestedMessageResponseFlux;
    }

    @Override
    public Flux<KeepAliveMessage> getKeepMessageResponseFlux() {
        return keepMessageResponseFlux;
    }

    @Override
    public Flux<NotInterestedMessage> getNotInterestedMessageResponseFlux() {
        return notInterestedMessageResponseFlux;
    }

    @Override
    public Flux<PieceMessage> getPieceMessageResponseFlux() {
        return pieceMessageResponseFlux;
    }

    @Override
    public Flux<PortMessage> getPortMessageResponseFlux() {
        return portMessageResponseFlux;
    }

    @Override
    public Flux<RequestMessage> getRequestMessageResponseFlux() {
        return requestMessageResponseFlux;
    }

    @Override
    public Flux<UnchokeMessage> getUnchokeMessageResponseFlux() {
        return unchokeMessageResponseFlux;
    }

}
