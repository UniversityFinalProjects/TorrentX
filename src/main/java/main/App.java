package main;

import christophedetroyer.torrent.TorrentParser;
import main.downloader.TorrentDownloader;
import main.downloader.TorrentDownloaders;
import main.peer.PeersCommunicator;
import main.peer.ReceiveMessages;
import reactor.core.publisher.Hooks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;

public class App {
    public static Scheduler MyScheduler = Schedulers.elastic();
    private static String downloadPath = System.getProperty("user.dir") + "/" + "torrents-test/";

    private static void f4() throws IOException {
        TorrentDownloader torrentDownloader = TorrentDownloaders
                .createDefaultTorrentDownloader(getTorrentInfo(), downloadPath);

        torrentDownloader.getPeersCommunicatorFlux()
                .map(PeersCommunicator::receivePeerMessages)
                .flatMap(ReceiveMessages::getPeerMessageResponseFlux)
                .subscribe(System.out::println, Throwable::printStackTrace, System.out::println);

        torrentDownloader.getTorrentStatusController().startDownload();
        torrentDownloader.getTorrentStatusController().startUpload();
    }

    public static void main(String[] args) throws Exception {
        Hooks.onOperatorDebug();
        f4();
        Thread.sleep(1000 * 1000);
    }

    private static TorrentInfo getTorrentInfo() throws IOException {
        String torrentFilePath = "src/main/resources/torrents/torrent-file-example3.torrent";
        return new TorrentInfo(torrentFilePath, TorrentParser.parseTorrent(torrentFilePath));
    }
}