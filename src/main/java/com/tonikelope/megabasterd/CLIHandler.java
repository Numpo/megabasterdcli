package com.tonikelope.megabasterd;

import java.util.logging.Logger;

import static com.tonikelope.megabasterd.MiscTools.extractMegaLinksFromString;

public class CLIHandler {
    private static final Logger logger = Logger.getLogger(CLIHandler.class.getName());

    public void handle(final MainPanel main_panel, final String[] args) {
        if (args.length < 2) {
            this.printHelp();
            return;
        }

        final String command = args[1];
        switch (command) {
            case "download":
                if (args.length < 4) {
                    logger.severe("Missing arguments for download");
                    this.printHelp();
                } else {
                    this.handleDownload(main_panel, args[2], args[3]);
                }
                break;
            case "upload":
                if (args.length < 4) {
                    logger.severe("Missing arguments for upload");
                    this.printHelp();
                } else {
                    this.handleUpload(args[2], args[3]);
                }
                break;
            default:
                logger.severe("Unknown command: " + command);
                this.printHelp();
        }
    }

    private void handleDownload(final MainPanel main_panel, final String link, final String destination) {
        final DownloadManager downloadManager = new DownloadManager(main_panel, null);
        final String url = extractMegaLinksFromString(link);
        downloadManager.download(url, destination);
    }

    private void handleUpload(final String filePath, final String destination) {
        // TODO
    }

    private void printHelp() {
        System.out.println("Usage: java -jar MegaBasterd.jar --cli <command> [options]");
        System.out.println("Commands:");
        System.out.println("  download <link> <destination> - Download file from Mega.nz");
        System.out.println("  upload <file> <destination>  - Upload file to Mega.nz");
    }
}

