package com.utils;

import main.file.system.AllocatedBlock;
import main.file.system.BlocksAllocatorImpl;
import main.peer.Link;
import reactor.core.publisher.Mono;

public class RemoteFakePeer extends Link {
    public RemoteFakePeer(Link link, FakePeerType fakePeerType) {
        super(link);

        this.receivePeerMessages()
                .getRequestMessageResponseFlux()
                .doOnNext(requestMessage -> {
                    switch (fakePeerType) {
                        case CLOSE_IN_FIRST_REQUEST:
                            closeConnection();
                            return;
                        case RESPOND_WITH_DELAY_100:
                            blockThread(100);
                            return;
                        case RESPOND_WITH_DELAY_3000:
                            //TODO: in some operating systems, the IO operations are extremely slow.
                            // for example the first use of randomAccessFile object. in linux all good.
                            // we need to remmber to change back 20->3.
                            blockThread(3 * 1000);
                            return;
                    }
                })
                .flatMap(requestMessage -> {
                    boolean doesFakePeerHaveThePiece = this.getPeerCurrentStatus()
                            .getPiecesStatus()
                            .get(requestMessage.getIndex());

                    if (!doesFakePeerHaveThePiece)
                        return Mono.empty();

                    switch (fakePeerType) {
                        case VALID:
                        case RESPOND_WITH_DELAY_100:
                        case RESPOND_WITH_DELAY_3000:
                            AllocatedBlock allocatedBlock = BlocksAllocatorImpl.getInstance()
                                    .allocate(0, requestMessage.getBlockLength())
                                    .block();
                            return this.sendMessages()
                                    .sendPieceMessage(requestMessage.getIndex(), requestMessage.getBegin(), allocatedBlock)
                                    .doOnEach(signal -> {
                                        // TODO: assert that we didn't miss any signal type or we will have a damn bug or a memory leak!
                                        if (signal.isOnError() || signal.isOnNext())
                                            BlocksAllocatorImpl.getInstance().free(allocatedBlock);
                                    });
                    }
                    // we will never be here...
                    return Mono.empty();
                }).publish()
                .autoConnect(0);
    }

    private void blockThread(int durationInMillis) {
        try {
            Thread.sleep(durationInMillis);
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }
    }
}
