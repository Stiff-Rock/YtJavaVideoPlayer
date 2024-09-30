package org.stiffrock;

import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class VideoLoader {
    public static String ytdlpPath;
    private static final Queue<String> videoUrls = new LinkedList<>();
    private static final LinkedList<SimpleEntry<String, String[]>> streamUrlQueue = new LinkedList<>();
    private static Runnable onQueueUpdate;

    public static void setOnQueueUpdateListener(Runnable listener) {
        onQueueUpdate = listener;
    }

    private static void notifyQueueUpdate() {
        if (onQueueUpdate != null) {
            onQueueUpdate.run();
        }
    }

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
                    StringBuilder errorOutput = new StringBuilder();

                    String line;
                    while ((line = reader.readLine()) != null) {
                        urlsBuilder.append(line).append("\n");
                    }


                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    while ((line = errorReader.readLine()) != null) {
                        errorOutput.append(line).append("\n");
                    }

                    reader.close();
                    errorReader.close();

                    int exitCode = process.waitFor();
                    if (exitCode != 0) {
                        throw new IOException("Exit code: " + exitCode + "\nError: " + errorOutput.toString().trim());
                    }

                    String[] Urls = urlsBuilder.toString().split("\n");
                    Collections.addAll(videoUrls, Urls);
                } catch (IOException | InterruptedException e) {
                    System.err.println("Error getting video Url. " + e.getMessage());
                }

                System.out.println(videoUrls.size() + " URLs loaded.");

                return null;
            }
        };
    }

    public static Task<Void> retrieveStreamUrl() {
        return new Task<>() {
            @Override
            protected Void call() {
                if (videoUrls.isEmpty()) {
                    return null;
                }

                String url = videoUrls.poll();
                ProcessBuilder processBuilder = new ProcessBuilder(ytdlpPath, "-f", "best", "-g", url);

                StringBuilder videoUrlBuilder = new StringBuilder();
                StringBuilder errorOutput = new StringBuilder();

                try {
                    System.out.println("--------------------");
                    System.out.println("Loading Stream Url...");

                    Process process = processBuilder.start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        videoUrlBuilder.append(line).append("\n");
                    }

                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    while ((line = errorReader.readLine()) != null) {
                        errorOutput.append(line).append("\n");
                    }

                    reader.close();
                    errorReader.close();

                    int exitCode = process.waitFor();
                    if (exitCode != 0) {
                        throw new IOException("Exit code: " + exitCode + "\nError: " + errorOutput.toString().trim());
                    }

                    String streamUrl = videoUrlBuilder.toString().trim();
                    String[] videoInfo = getVideoInfo(url);
                    streamUrlQueue.add(new SimpleEntry<>(streamUrl, videoInfo));

                    notifyQueueUpdate();

                    System.out.println("Current queue length: " + streamUrlQueue.size());
                } catch (IOException | InterruptedException e) {
                    System.err.println("Error retrieving stream Url. " + e.getMessage());
                }

                if (!videoUrls.isEmpty()) {
                    new Thread(retrieveStreamUrl()).start();
                } else {
                    System.out.println("--------------------");
                    System.out.println("Finished loading video/s");
                    System.out.println("--------------------");
                }
                return null;
            }
        };
    }

    public static String[] getVideoInfo(String videoUrl) {
        try {
            Process process = new ProcessBuilder(ytdlpPath, "--get-title", "--get-thumbnail", "--get-duration", videoUrl).start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder errorOutput = new StringBuilder();

            String line;
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }

            errorReader.close();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Exit code: " + exitCode + "\nError: " + errorOutput.toString().trim());
            }

            String title = reader.readLine();
            String thumbnailUrl = reader.readLine();
            String duration = reader.readLine();
            reader.close();

            System.out.println("Playing: " + title + " (" + duration + ")");

            return new String[]{title, thumbnailUrl, duration};
        } catch (IOException | InterruptedException e) {
            System.err.println("Error getting video info: " + e.getMessage());
        }

        return null;
    }


    public static SimpleEntry<String, String[]> pollStreamUrl() {
        return streamUrlQueue.poll();
    }

    public static SimpleEntry<String, String[]> peekVideoFromQueue(int index) {
        return streamUrlQueue.get(index);
    }

    public static void removeVideoFromQueue(String key) {
        Iterator<SimpleEntry<String, String[]>> iterator = streamUrlQueue.iterator();
        while (iterator.hasNext()) {
            SimpleEntry<String, String[]> entry = iterator.next();
            if (entry.getKey().equals(key)) {
                iterator.remove();
                break;
            }
        }
    }

    public static boolean isQueueEmpty() {
        return streamUrlQueue.isEmpty();
    }

    public static int getQueueSize() {
        return streamUrlQueue.size();
    }
}
