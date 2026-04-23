import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ProjectRunner {

    // Folder where the user puts images/videos
    private final Path inputFolder;

    // Folder where all results will be saved
    private final Path outputFolder;

    // Handles AI requests (descriptions, phrases, etc.)
    private final AIService aiService;

    // Handles video creation, audio, and images
    private final VideoCreator videoCreator;

    // Constructor: sets everything up
    public ProjectRunner(String apiKey, Path inputFolder, Path outputFolder, int width, int height, int secondsPerImage) {
        this.inputFolder = inputFolder;
        this.outputFolder = outputFolder;
        this.aiService = new AIService(apiKey);
        this.videoCreator = new VideoCreator(outputFolder, width, height, secondsPerImage);
    }

    // This runs the whole program
    public void run() throws Exception {

        // Make sure output folder exists
        ensureFolder(outputFolder);

        // Load all files from input_media
        List<MediaItem> items = scanMedia(inputFolder);

        // If nothing is found, stop
        if (items.isEmpty()) {
            throw new IllegalArgumentException("No media found inside input_media.");
        }

        // Fill metadata (date, GPS) for each file
        for (MediaItem item : items) {
            MetadataUtils.fillMetadata(item);

            // If no date was found, use current time
            if (item.getDateTime() == null) {
                item.setDateTime(LocalDateTime.now());
            }
        }

        // Sort items from oldest to newest
        items.sort(Comparator.comparing(MediaItem::getDateTime));

        // Try to generate AI image for intro
        Path coverImage;
        try {
            coverImage = outputFolder.resolve("cover_ai.png");
            aiService.generateIntroImageFromMedia(items, coverImage);
        } catch (Exception e) {
            // If AI fails, use fallback image
            coverImage = videoCreator.createFallbackCover(
                    "A Journey Through Memories",
                    "Generated from places, dates, and moments"
            );
        }

        // Create audio for intro
        Path introAudio = outputFolder.resolve("intro.aiff");
        videoCreator.generateAudio("A journey through memories, places, and experiences.", introAudio);

        // List to store all video clips
        List<Path> clips = new ArrayList<>();

        // Create first clip (intro image + audio)
        clips.add(videoCreator.createStillClip(coverImage, introAudio, "intro.mp4", 4));

        // Process each image/video
        for (int i = 0; i < items.size(); i++) {
            MediaItem item = items.get(i);

            String description;

            // Try to get AI description
            try {
                description = aiService.generateDescription(item);
            } catch (Exception e) {
                // If it fails, use default text
                description = "A memorable moment captured during the journey.";
            }

            // Extra safety check
            if (description == null || description.isBlank()) {
                description = "A memorable moment captured during the journey.";
            }

            // Save description
            item.setDescription(description);

            // Create audio from description
            Path audio = outputFolder.resolve("audio_" + i + ".aiff");
            videoCreator.generateAudio(description, audio);
            item.setAudioPath(audio);

            // Create video clip (image/video + audio)
            Path clip = videoCreator.createMediaClip(item, i);
            item.setClipPath(clip);
            clips.add(clip);

            // Wait a bit to avoid API limits
            Thread.sleep(5000);
        }

        // Get first and last items with GPS
        MediaItem first = firstWithGps(items);
        MediaItem last = lastWithGps(items);

        // Only create map if GPS exists
        if (first != null && last != null) {

            String phrase;

            // Try to generate final phrase
            try {
                phrase = aiService.generateEndingPhrase(first, last);
            } catch (Exception e) {
                phrase = "Every place changed the journey, and every memory gave it meaning.";
            }

            if (phrase == null || phrase.isBlank()) {
                phrase = "Every place changed the journey, and every memory gave it meaning.";
            }

            // Create map image
            Path mapCard = videoCreator.createMapCard(first, last, phrase);

            // Create audio for ending
            Path finalAudio = outputFolder.resolve("final_audio.aiff");
            videoCreator.generateAudio(phrase, finalAudio);

            // Create final clip
            Path endClip = videoCreator.createStillClip(mapCard, finalAudio, "end.mp4", 5);
            clips.add(endClip);
        }

        // Combine all clips into one video
        Path finalVideo = videoCreator.buildFinalVideo(clips);

        System.out.println("Final video created at: " + finalVideo.toAbsolutePath());
    }

    // Reads all files from input folder
    private List<MediaItem> scanMedia(Path folder) throws Exception {
        List<MediaItem> items = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
            for (Path path : stream) {

                // Skip folders
                if (Files.isDirectory(path)) {
                    continue;
                }

                String name = path.getFileName().toString().toLowerCase();

                // Check if image
                if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")) {
                    items.add(new MediaItem(path, MediaItem.Type.IMAGE));

                // Check if video
                } else if (name.endsWith(".mp4") || name.endsWith(".mov") || name.endsWith(".m4v")) {
                    items.add(new MediaItem(path, MediaItem.Type.VIDEO));
                }
            }
        }

        return items;
    }

    // Creates folder if it does not exist
    private void ensureFolder(Path folder) throws Exception {
        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
        }
    }

    // Finds first item with GPS
    private MediaItem firstWithGps(List<MediaItem> items) {
        for (MediaItem item : items) {
            if (item.hasGps()) {
                return item;
            }
        }
        return null;
    }

    // Finds last item with GPS
    private MediaItem lastWithGps(List<MediaItem> items) {
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).hasGps()) {
                return items.get(i);
            }
        }
        return null;
    }
}