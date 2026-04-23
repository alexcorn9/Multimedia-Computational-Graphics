import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetadataUtils {

    // This method decides if the file is an image or video
    // and calls the correct method
    public static void fillMetadata(MediaItem item) {
        try {
            if (item.getType() == MediaItem.Type.IMAGE) {
                fillImageMetadata(item);
            } else {
                fillVideoMetadata(item);
            }
        } catch (Exception e) {
            // If something goes wrong, we just show a warning
            System.out.println("Metadata warning for " + item.getPath().getFileName() + ": " + e.getMessage());
        }
    }

    // This gets metadata from images using exiftool
    private static void fillImageMetadata(MediaItem item) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "exiftool",
                "-DateTimeOriginal",
                "-GPSLatitude",
                "-GPSLongitude",
                "-n",
                item.getPath().toString()
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;

        // Read each line returned by exiftool
        while ((line = reader.readLine()) != null) {
            line = line.trim();

            // If we find the date, we save it
            if (line.startsWith("Date/Time Original")) {
                String value = valueAfterColon(line);
                if (value != null && !value.isBlank()) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
                    item.setDateTime(LocalDateTime.parse(value, formatter));
                }

            // If we find latitude, we save it
            } else if (line.startsWith("GPS Latitude")) {
                String value = valueAfterColon(line);
                if (value != null && !value.isBlank()) {
                    item.setLatitude(Double.parseDouble(value));
                }

            // If we find longitude, we save it
            } else if (line.startsWith("GPS Longitude")) {
                String value = valueAfterColon(line);
                if (value != null && !value.isBlank()) {
                    item.setLongitude(Double.parseDouble(value));
                }
            }
        }

        process.waitFor();
    }

    // This gets metadata from videos using ffprobe
    private static void fillVideoMetadata(MediaItem item) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "ffprobe",
                "-v", "quiet",
                "-print_format", "json",
                "-show_format",
                "-show_streams",
                item.getPath().toString()
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder json = new StringBuilder();
        String line;

        // Read full JSON response from ffprobe
        while ((line = reader.readLine()) != null) {
            json.append(line).append("\n");
        }

        process.waitFor();

        String raw = json.toString();

        // Try to get the creation date
        String creationTime = extractFirst(raw, "\"creation_time\"\\s*:\\s*\"([^\"]+)\"");
        if (creationTime != null) {
            creationTime = creationTime.replace("Z", "");
            if (creationTime.length() >= 19) {
                item.setDateTime(LocalDateTime.parse(creationTime.substring(0, 19)));
            }
        }

        // Try to get GPS location
        String location = extractFirst(raw, "\"location\"\\s*:\\s*\"([^\"]+)\"");
        if (location != null) {
            parseIso6709(location, item);
        }
    }

    // This converts the GPS string into latitude and longitude
    private static void parseIso6709(String location, MediaItem item) {
        try {
            String cleaned = location.replace("/", "").trim();

            // This pattern finds numbers like +19.1234-99.1234
            Matcher m = Pattern.compile("([+-]\\d+\\.\\d+)([+-]\\d+\\.\\d+)").matcher(cleaned);

            if (m.find()) {
                item.setLatitude(Double.parseDouble(m.group(1)));
                item.setLongitude(Double.parseDouble(m.group(2)));
            }
        } catch (Exception ignored) {
            // If parsing fails, we just ignore it
        }
    }

    // Gets the text after ":" in a line
    private static String valueAfterColon(String line) {
        int index = line.indexOf(":");
        if (index == -1) {
            return null;
        }
        return line.substring(index + 1).trim();
    }

    // Finds a value inside text using a pattern
    private static String extractFirst(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        return m.find() ? m.group(1) : null;
    }
}