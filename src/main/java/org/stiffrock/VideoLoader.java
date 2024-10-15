package org.stiffrock;

import javafx.concurrent.Task;
import javafx.scene.media.Media;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

public class VideoLoader {
    public static String ytdlpPath;
    public static String ffmpegPath;
    private static final Queue<String> videoRequests = new LinkedList<>();
    private static final LinkedList<SimpleEntry<Media, String[]>> streamUrlQueue = new LinkedList<>();
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
    public static Task<Void> getMedia() {
        return new Task<>() {
            @Override
            protected Void call() {
                if (videoRequests.isEmpty()) {
                    System.err.println("Error: Url queue is empty");
                    return null;
                }

                String url = videoRequests.poll();

                System.out.println("--------------------");

                Media video = ffmpegConvert(urlProcess(url, "bestvideo"), urlProcess(url, "bestaudio"));
                String[] videoInfo = getVideoInfo(url);

                streamUrlQueue.add(new SimpleEntry<>(video, videoInfo));
                notifyQueueUpdate(true);

                System.out.println("Current queue length: " + streamUrlQueue.size());
                return null;
            }
        };
    }

    private static String urlProcess(String url, String format) {
        System.out.print("Loading Stream Url... ");
        ProcessBuilder processBuilder = new ProcessBuilder(ytdlpPath, "-f", format, "-g", url);

        StringBuilder videoUrlBuilder = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();

        String streamUrl = "";
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
                throw new IOException("Error retrieving stream Url - Exit code: " + exitCode + "\n" + errorOutput.toString().trim());
            }

            streamUrl = videoUrlBuilder.toString().trim();

        } catch (IOException | InterruptedException e) {
            System.err.println(e.getMessage());
        }
        System.out.println("Done");
        return streamUrl;
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

    private static Media ffmpegConvert(String videoUrl, String audioUrl) {
        String outputVideo = "tempVideoFiles/video_" + System.currentTimeMillis() + ".mp4";
        Media video;

        String[] command = {ffmpegPath, "-i", videoUrl, "-i", audioUrl, "-c:v", "copy", "-c:a", "aac", "-shortest", outputVideo};
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        StringBuilder logOutput = new StringBuilder();

        try {
            Process process = processBuilder.start();
            System.out.print("Executing ffmpeg conversion... ");

            try (BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = inputReader.readLine()) != null) {
                    logOutput.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.err.println("FFmpeg process exited with code: " + exitCode);
                System.err.println(logOutput);
                throw new RuntimeException("Error in FFMpeg process: " + logOutput);
            }

            Path videoPath = Paths.get(outputVideo);
            if (Files.exists(videoPath)) {
                video = new Media(videoPath.toUri().toString());
                System.out.println("Done");
            } else {
                throw new RuntimeException("Output video file was not created: " + outputVideo);
            }

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error in FFMpeg: " + e.getMessage(), e);
        }

        return video;
    }

    public static SimpleEntry<Media, String[]> pollMedia() {
        notifyQueueUpdate(false);
        return streamUrlQueue.poll();
    }

    public static SimpleEntry<Media, String[]> peekVideoFromQueue(int index) {
        return streamUrlQueue.get(index);
    }

    public static void removeVideoFromQueue(int queueIndex) {
        streamUrlQueue.remove(queueIndex);
    }

    public static SimpleEntry<Media, String[]> pollVideoByIndex(int queueIndex) {
        return streamUrlQueue.remove(queueIndex);
    }

    public static void changeVideoPositionInQueue(int queueIndex, int desiredIndex) {
        streamUrlQueue.add(desiredIndex, streamUrlQueue.remove(queueIndex));

        for (SimpleEntry<Media, String[]> entry : streamUrlQueue) {
            String[] value = entry.getValue();

            System.out.println("Value: " + Arrays.toString(value));
        }
        System.out.println();
    }

    public static boolean isQueueEmpty() {
        return streamUrlQueue.isEmpty();
    }

    public static int getQueueSize() {
        return streamUrlQueue.size();
    }

    public static int getVideoRequestsSize() {
        return videoRequests.size();
    }

}
