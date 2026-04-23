import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AIService {

    private final String apiKey;

    // This just saves the API key so we can use it later
    public AIService(String apiKey) {
        this.apiKey = apiKey;
    }

    // This asks the AI to give a short sentence for each image or video
    public String generateDescription(MediaItem item) throws Exception {

        // Simple instruction so the AI does not give something too long
        String prompt = "Write one short cinematic sentence describing a travel moment. "
                + "Keep it under 15 words. "
                + "Return only the sentence.";
    
        // We build the JSON text that will be sent to the API
        String json = "{"
                + "\"model\":\"gemini-2.5-flash\","
                + "\"contents\":[{"
                + "\"parts\":[{\"text\":\"" + escapeJson(prompt) + "\"}]"
                + "}]"
                + "}";
    
        // We use curl from Java to call the API
        ProcessBuilder pb = new ProcessBuilder(
                "curl",
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey,
                "-H", "Content-Type: application/json",
                "-d", json
        );
    
        // This makes sure we can read everything (even errors)
        pb.redirectErrorStream(true);
        Process process = pb.start();
    
        // Read the response from the API
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
        );
    
        StringBuilder response = new StringBuilder();
        String line;
    
        while ((line = reader.readLine()) != null) {
            response.append(line).append("\n");
        }
    
        process.waitFor();
    
        // Try to find the sentence inside the response
        String text = extractFirst(response.toString(), "\"text\"\\s*:\\s*\"([^\"]+)\"");
    
        // If something went wrong, return a default sentence
        if (text == null || text.isBlank()) {
            return "A unique moment from the journey.";
        }
    
        return cleanJsonText(text);
    }

    // This creates the final phrase for the ending of the video
    public String generateEndingPhrase(MediaItem first, MediaItem last) throws Exception {
        String prompt = "Write one inspirational and emotional phrase for the ending of a travel video. "
                + "Keep it under 24 words. "
                + "First location: " + gpsText(first) + ". "
                + "Last location: " + gpsText(last) + ". "
                + "Return only the phrase.";

        String json = "{"
                + "\"contents\":[{"
                + "\"parts\":[{\"text\":\"" + escapeJson(prompt) + "\"}]"
                + "}]"
                + "}";

        // Call the API using the helper method
        String response = runCurl(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey,
                json
        );

        String text = extractFirst(response, "\"text\"\\s*:\\s*\"([^\"]+)\"");

        // If the AI does not return anything, use a default phrase
        if (text == null || text.isBlank()) {
            return "Every place changed the journey, and every memory gave it meaning.";
        }

        return cleanJsonText(text);
    }

    // This tries to generate the first image using AI
    public void generateIntroImageFromMedia(List<MediaItem> items, Path outputImage) throws Exception {
        StringBuilder mediaSummary = new StringBuilder();

        // Build a simple description of all files
        for (MediaItem item : items) {
            mediaSummary.append("Type: ").append(item.getType()).append(". ");
            mediaSummary.append("Date: ").append(item.getDateTime() == null ? "unknown" : item.getDateTime()).append(". ");
            mediaSummary.append("Coordinates: ").append(gpsText(item)).append(". ");
            mediaSummary.append("File: ").append(item.getPath().getFileName()).append(". ");
        }

        String prompt = "Create one cinematic poster-style image that captures the essence of a journey represented by these media items. "
                + "The image must feel realistic and visually connected. "
                + "Do not add any text inside the image. "
                + "Media summary: " + mediaSummary;

        String json = "{"
                + "\"contents\":[{"
                + "\"parts\":[{\"text\":\"" + escapeJson(prompt) + "\"}]"
                + "}],"
                + "\"generationConfig\":{"
                + "\"responseModalities\":[\"TEXT\",\"IMAGE\"]"
                + "}"
                + "}";

        String response = runCurl(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image:generateContent?key=" + apiKey,
                json
        );

        // Try to get the image data from the response
        String base64 = extractFirst(response, "\"data\"\\s*:\\s*\"([^\"]+)\"");

        if (base64 == null || base64.isBlank()) {
            throw new RuntimeException("Could not get image from API.");
        }

        // Convert the text into real image bytes and save it
        byte[] imageBytes = Base64.getDecoder().decode(base64);
        FileOutputStream fos = new FileOutputStream(outputImage.toFile());
        fos.write(imageBytes);
        fos.close();
    }

    // This method runs curl and returns the response as text
    private String runCurl(String url, String json) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "curl",
                url,
                "-H", "Content-Type: application/json",
                "-d", json
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
        );

        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line).append("\n");
        }

        process.waitFor();
        return response.toString();
    }

    // Turns GPS into a simple text
    private String gpsText(MediaItem item) {
        if (item == null || !item.hasGps()) {
            return "unknown";
        }
        return item.getLatitude() + ", " + item.getLongitude();
    }

    // Looks for a piece of text using a pattern
    private String extractFirst(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        return m.find() ? m.group(1) : null;
    }

    // Fix small formatting issues in the response
    private String cleanJsonText(String text) {
        return text
                .replace("\\n", " ")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .trim();
    }

    // Prepares text so it does not break the JSON format
    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}