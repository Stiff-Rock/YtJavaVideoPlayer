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
import java.util.function.Consumer;

public class VideoLoader {
    public static String ytdlpPath;
    private static final Queue<String> videoUrls = new LinkedList<>();
    private static final LinkedList<SimpleEntry<String, String[]>> streamUrlQueue = new LinkedList<>();
    private static Consumer<Boolean> onQueueUpdate;

    public static void setOnQueueUpdateListener(Consumer<Boolean> listener) {
        onQueueUpdate = listener;
    }

    private static void notifyQueueUpdate(boolean newAddedToQueue) {
        if (onQueueUpdate != null) {
            onQueueUpdate.accept(newAddedToQueue);  // Pass the boolean to the listener
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

    // Gets each video Url form the playlist
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
                    System.out.println(Urls.length+ " URLs loaded.");
                    Collections.addAll(videoUrls, Urls);
                } catch (IOException | InterruptedException e) {
                    System.err.println("Error getting video Url. " + e.getMessage());
                }

                return null;
            }
        };
    }

    // Goes through the videoUrls list to get every stream url from the video Urls
    public static Task<Void> retrieveStreamUrl() {
        return new Task<>() {
            @Override
            protected Void call() {
                if (videoUrls.isEmpty()) {
                    System.err.println("Error: Url queue is empty");
                    return null;
                }

                String url = videoUrls.poll();

                boolean correctStreamUrl = false;
                int tries = 0;

                System.out.println("--------------------");
                System.out.println("Loading Stream Url...");

                do {
                    ProcessBuilder processBuilder = new ProcessBuilder(ytdlpPath, "-g", url);

                    StringBuilder videoUrlBuilder = new StringBuilder();
                    StringBuilder errorOutput = new StringBuilder();

                    try {
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
                            throw new IOException("Exit code: " + exitCode + "\n" + errorOutput.toString().trim());
                        }

                        String streamUrl = videoUrlBuilder.toString().trim();
                        String[] urlArray = streamUrl.split("\n");

                        if (urlArray.length == 1) {
                            correctStreamUrl = true;
                            System.out.println("Correctly loaded streamUrl");
                        } else if (urlArray.length == 2) {
                            tries++;
                            throw new InterruptedException("Two separate stream URLs detected, retrying... (Iteration nº" + tries + ")");
                        }

                        String[] videoInfo = getVideoInfo(url);
                        streamUrlQueue.add(new SimpleEntry<>(streamUrl, videoInfo));

                        notifyQueueUpdate(true);

                        System.out.println("Current queue length: " + streamUrlQueue.size());
                    } catch (IOException | InterruptedException e) {
                        System.err.println("Error retrieving stream Url: " + e.getMessage());
                    }
                } while (!correctStreamUrl && tries != 3);

                if (tries == 3) {
                    System.err.println("Video could not be loaded.");
                    return null;
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
                throw new IOException("Exit code: " + exitCode + "\n" + errorOutput.toString().trim());
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
        notifyQueueUpdate(false);
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
                notifyQueueUpdate(false);
                iterator.remove();
                break;
            }
        }
    }

    public static SimpleEntry<String, String[]> pollVideoFromQueueByUrl(String key) {
        Iterator<SimpleEntry<String, String[]>> iterator = streamUrlQueue.iterator();
        while (iterator.hasNext()) {
            SimpleEntry<String, String[]> entry = iterator.next();
            if (entry.getKey().equals(key)) {
                iterator.remove();
                return entry;
            }
        }
        return null;
    }

    public static boolean isQueueEmpty() {
        return streamUrlQueue.isEmpty();
    }

    public static int getQueueSize() {
        return streamUrlQueue.size();
    }
}
