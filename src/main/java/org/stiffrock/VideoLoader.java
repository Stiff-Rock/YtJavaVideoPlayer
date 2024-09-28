package org.stiffrock;

import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

public class VideoLoader {
    public static String ytdlpPath;
    private static final Queue<String> videoUrls = new LinkedList<>();
    private static final Queue<SimpleEntry<String, String>> streamUrls = new LinkedList<>();

    public static Task<Void> loadVideoUrl(String videoUrl) {
        return new Task<>() {
            @Override
            protected Void call() {
                videoUrls.add(videoUrl);
                return null;
            }
        };
    }

    public static Task<Void> loadPlaylistUrls(String videoUrl) {
        return new Task<>() {
            @Override
            protected Void call() {
                try {
                    ProcessBuilder processBuilder = new ProcessBuilder(ytdlpPath, "--flat-playlist", "--get-url", videoUrl);
                    Process process = processBuilder.start();

                    StringBuilder urlsBuilder = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        urlsBuilder.append(line).append("\n");
                    }

                    process.waitFor();

                    String[] Urls = urlsBuilder.toString().split("\n");
                    Collections.addAll(videoUrls, Urls);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }

                System.out.println(videoUrls.size() + " URLs loaded.");

                return null;
            }
        };
    }

    public static Task<Void> loadStreamUrl() {
        return new Task<>() {
            @Override
            protected Void call() {
                if (videoUrls.isEmpty()) {
                    return null;
                }

                String url = videoUrls.poll();
                ProcessBuilder processBuilder = new ProcessBuilder(ytdlpPath, "-f", "best", "-g", url);
                StringBuilder videoUrlBuilder = new StringBuilder();

                try {
                    System.out.println("--------------------");
                    System.out.println("Loading StreamUrl...");
                    Process process = processBuilder.start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        videoUrlBuilder.append(line).append("\n");
                    }

                    reader.close();
                    int exitCode = process.waitFor();

                    if (exitCode != 0) {
                        throw new IOException("Error occurred while executing yt-dlp. Exit code: " + exitCode);
                    }

                    String streamUrl = videoUrlBuilder.toString().trim();
                    streamUrls.add(new SimpleEntry<>(streamUrl, getVideoTitle(url)));
                    System.out.println("Current queue length: " + streamUrls.size());
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }

                if (!videoUrls.isEmpty()) {
                    new Thread(loadStreamUrl()).start();
                } else {
                    System.out.println("--------------------");
                    System.out.println("Finished loading video/s");
                    System.out.println("--------------------");
                }
                return null;
            }
        };
    }

    public static String getVideoTitle(String videoUrl) {
        try {
            Process process = new ProcessBuilder(ytdlpPath, "--get-title", videoUrl).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            process.waitFor();

            String title = reader.readLine();
            System.out.println("Playing: " + title);

            return title;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static SimpleEntry<String, String> getStreamUrl() {
        return streamUrls.poll();
    }
}
