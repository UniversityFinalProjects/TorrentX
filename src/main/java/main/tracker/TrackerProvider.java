package main.tracker;

import main.tracker.requests.ConnectRequest;
import main.tracker.response.ConnectResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;

public class TrackerProvider {
    public static Flux<TrackerConnection> connectToTrackers(List<Tracker> trackers) {

        // if I get an errorSignal signal containing one of those errors,
        // then I will communicate with the next tracker in the tracker-list.
        Predicate<Throwable> communicationErrorsToIgnore = (Throwable error) ->
                error instanceof SocketTimeoutException ||
                        error instanceof BadResponseException;

        return Flux.fromIterable(trackers)
                .flatMap(tracker -> {
                    ConnectRequest connectRequest = new ConnectRequest(tracker.getTracker(), tracker.getPort(), 123456);
                    Function<ByteBuffer, ConnectResponse> createConnectResponse = (ByteBuffer response) ->
                            new ConnectResponse(tracker.getTracker(), tracker.getPort(), response.array());
                    return TrackerCommunication.communicate(connectRequest, createConnectResponse)
                            .onErrorResume(communicationErrorsToIgnore, error -> Mono.empty());
                })
                .log(null, Level.INFO, true, SignalType.ON_NEXT)
                .map(connectResponse -> new TrackerConnection(connectResponse));
    }
}