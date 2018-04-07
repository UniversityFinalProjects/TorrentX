package main.peer;

import main.App;
import main.peer.peerMessages.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.io.DataInputStream;
import java.io.IOException;

class ReceivePeerMessagesImpl implements ReceivePeerMessages {
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

    public ReceivePeerMessagesImpl(Peer me, Peer peer,
                                   PeerCurrentStatus peerCurrentStatus, DataInputStream dataInputStream) {
        this.peerCurrentStatus = peerCurrentStatus;

        Flux<PeerMessage> peerMessageResponseFlux = Flux.create((FluxSink<PeerMessage> sink) -> listenForPeerMessages(sink, me, peer, dataInputStream))
                .subscribeOn(App.MyScheduler)
                // it is important to publish from source on different thread then the
                // subscription to this source's thread every time because:
                // if not and we subscribe to this specific source multiple times then only the
                // first subscription will be activated and the source will never end
                .onErrorResume(PeerExceptions.communicationErrors, throwable -> Mono.empty())
                // there are multiple subscribers to this source (every specific peer-message flux).
                // all of them must get the same message and ***not activate this source more then once***.
                .publish()
                .autoConnect();

        this.bitFieldMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof BitFieldMessage)
                .cast(BitFieldMessage.class)
                .doOnNext(bitFieldMessage -> this.peerCurrentStatus.updatePiecesStatus(bitFieldMessage.getPiecesStatus()));

        this.cancelMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof CancelMessage)
                .cast(CancelMessage.class);

        this.chokeMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof ChokeMessage)
                .cast(ChokeMessage.class)
                .doOnNext(__ -> this.peerCurrentStatus.setIsHeChokingMe(true));

        this.extendedMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof ExtendedMessage)
                .cast(ExtendedMessage.class);

        this.haveMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof HaveMessage)
                .cast(HaveMessage.class);

        this.interestedMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof InterestedMessage)
                .cast(InterestedMessage.class)
                .doOnNext(__ -> this.peerCurrentStatus.setIsHeInterestedInMe(true));

        this.keepMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof KeepAliveMessage)
                .cast(KeepAliveMessage.class);

        this.notInterestedMessageResponseFlux = peerMessageResponseFlux
                .filter(peerMessage -> peerMessage instanceof NotInterestedMessage)
                .cast(NotInterestedMessage.class)
                .doOnNext(__ -> this.peerCurrentStatus.setIsHeInterestedInMe(false));

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
                .cast(UnchokeMessage.class)
                .doOnNext(__ -> this.peerCurrentStatus.setIsHeChokingMe(false));

        this.peerMessageResponseFlux = Flux.merge(
                this.unchokeMessageResponseFlux,
                this.requestMessageResponseFlux,
                this.portMessageResponseFlux,
                this.pieceMessageResponseFlux,
                this.notInterestedMessageResponseFlux,
                this.keepMessageResponseFlux,
                this.interestedMessageResponseFlux,
                this.haveMessageResponseFlux,
                this.extendedMessageResponseFlux,
                this.chokeMessageResponseFlux,
                this.cancelMessageResponseFlux,
                this.bitFieldMessageResponseFlux)
                .doOnNext(x -> System.out.println(x))
                .publish()
                .autoConnect();
    }

    private void listenForPeerMessages(FluxSink<PeerMessage> sink, Peer me, Peer peer, DataInputStream dataInputStream) {
        while (!sink.isCancelled()) {
            try {
                PeerMessage peerMessage = PeerMessageFactory.create(peer, me, dataInputStream);
                sink.next(peerMessage);
            } catch (IOException e) {
                try {
                    dataInputStream.close();
                } catch (IOException e1) {
                    // TODO: do something better... it's a fatal problem with my design!!!
                    e1.printStackTrace();
                }
                if (!sink.isCancelled())
                    sink.error(e);
                return;
            }
        }
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
