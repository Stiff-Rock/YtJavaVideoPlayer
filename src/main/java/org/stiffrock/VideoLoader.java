package org.stiffrock;

import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

public class VideoLoader {
    public static String ytdlpPath;
    private static final Queue<String> videoRequests = new LinkedList<>();
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
                videoRequests.add(videoUrl);
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
                    System.out.println(Urls.length + " URLs loaded.");
                    Collections.addAll(videoRequests, Urls);

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
                if (videoRequests.isEmpty()) {
                    System.err.println("Error: Url queue is empty");
                    return null;
                }

                String url = videoRequests.poll();

                boolean correctStreamUrl = false;

                System.out.println("--------------------");
                System.out.println("Loading Stream Url...");

                do {
                    ProcessBuilder processBuilder = new ProcessBuilder(ytdlpPath, "-f b", "-g", url);

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
                            errorOutput.append(line);
                        }

                        reader.close();
                        errorReader.close();

                        int exitCode = process.waitFor();

                        if (exitCode == 1) {
                            throw new IOException("Retrying request...");
                        } else if (exitCode != 0) {
                            throw new IOException("Error retrieving stream Url - Exit code: " + exitCode + "\n" + errorOutput.toString().trim());
                        } else {
                            correctStreamUrl = true;
                        }

                        String streamUrl = videoUrlBuilder.toString().trim();

                        String[] retrivedInfo = getVideoInfo(url);
                        if (retrivedInfo == null)
                            throw new IOException("Could not retrieve video info.");

                        String[] videoInfo = {retrivedInfo[0], retrivedInfo[1], retrivedInfo[2], streamUrl};
                        streamUrlQueue.add(new SimpleEntry<>(streamUrl, videoInfo));

                        notifyQueueUpdate(true);

                        System.out.println("Current queue length: " + streamUrlQueue.size());
                    } catch (IOException | InterruptedException e) {
                        System.err.println(e.getMessage());
                    }
                } while (!correctStreamUrl);

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

            System.out.println("Loaded: " + title + " (" + duration + ")");

            return new String[]{title, thumbnailUrl, duration};
        } catch (IOException | InterruptedException e) {
            System.err.println("Error getting video info: " + e.getMessage());
        }

        return null;
    }

    public static SimpleEntry<String, String[]> pollStreamUrl() {
        SimpleEntry<String, String[]> entry = streamUrlQueue.poll();
        if (entry != null) {
            System.out.println("Polled: " + entry.getValue()[0]);
        } //TODO gestionar esto
        notifyQueueUpdate(false);
        printStreamQueue();
        return entry;
    }

    public static SimpleEntry<String, String[]> peekVideoFromQueue(int index) {
        return streamUrlQueue.get(index);
    }

    public static void removeVideoFromQueue(int queueIndex) {
        SimpleEntry<String, String[]> entry = streamUrlQueue.remove(queueIndex);
        System.out.println("Deleted: " + entry.getValue()[0]);
        notifyQueueUpdate(false);
        printStreamQueue();
    }

    public static SimpleEntry<String, String[]> pollVideoByIndex(int queueIndex) {
        SimpleEntry<String, String[]> entry = streamUrlQueue.remove(queueIndex);
        System.out.println("Index " + queueIndex + " polled: " + entry.getValue()[0]);
        notifyQueueUpdate(false);
        return entry;
    }

    public static void changeVideoPositionInQueue(int queueIndex, int desiredIndex) {
        SimpleEntry<String, String[]> entry = streamUrlQueue.remove(queueIndex);
        streamUrlQueue.add(desiredIndex, entry);
        System.out.println("Changed index " + queueIndex + " to " + desiredIndex + " " + entry.getValue()[0]);
        notifyQueueUpdate(false);
        printStreamQueue();
    }

    public static boolean isQueueEmpty() {
        return streamUrlQueue.isEmpty();
    }

    public static LinkedList<SimpleEntry<String, String[]>> getQueue() {
        return streamUrlQueue;
    }

    public static int getQueueSize() {
        return streamUrlQueue.size();
    }

    public static int getVideoRequestsSize() {
        return videoRequests.size();
    }

    public static void checkYtDlpUpdates() {
        try {
            Process process = new ProcessBuilder(ytdlpPath, "-U").start();

            StringBuilder errorOutput = new StringBuilder();
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
            errorReader.close();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Exit code: " + exitCode + "\n" + errorOutput.toString().trim());
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error Updating yt-dlp: " + e.getMessage());
        }
    }

    public static void printStreamQueue() {
        System.out.println("--Current Queue Order--");
        int counter = 0;
        for (SimpleEntry<String, String[]> entry : streamUrlQueue) {
            String title = entry.getValue()[0];
            System.out.println(counter + " - " + title);
            counter++;
        }
        System.out.println("-----------------------");
    }
}
