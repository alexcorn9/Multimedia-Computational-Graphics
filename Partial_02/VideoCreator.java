import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;

public class VideoCreator {

    // Where everything will be saved
    private final Path outputFolder;

    // Video size (portrait mode)
    private final int width;
    private final int height;

    // How long each image stays on screen
    private final int secondsPerImage;

    public VideoCreator(Path outputFolder, int width, int height, int secondsPerImage) {
        this.outputFolder = outputFolder;
        this.width = width;
        this.height = height;
        this.secondsPerImage = secondsPerImage;
    }

    // Turns text into audio using the system voice
    public void generateAudio(String text, Path outputAiff) throws Exception {
        runCommand(List.of(
                "say",
                "-o", outputAiff.toString(),
                text
        ));
    }

    // Creates a simple cover if AI image fails
    public Path createFallbackCover(String title, String subtitle) throws Exception {
        Path output = outputFolder.resolve("cover_fallback.jpg");

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // Background gradient
        GradientPaint gp = new GradientPaint(0, 0, new Color(10, 10, 30), 0, height, new Color(70, 20, 90));
        g.setPaint(gp);
        g.fillRect(0, 0, width, height);

        // Title text
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 64));
        drawCenteredText(g, title, 500, 900, 80);

        // Subtitle text
        g.setFont(new Font("SansSerif", Font.PLAIN, 34));
        drawCenteredText(g, subtitle, 900, 900, 48);

        g.dispose();
        ImageIO.write(img, "jpg", output.toFile());

        return output;
    }

    // Creates a video from a single image + audio
    public Path createStillClip(Path imagePath, Path audioPath, String outputName, int seconds) throws Exception {
        Path output = outputFolder.resolve(outputName);

        runCommand(List.of(
                "ffmpeg", "-y",
                "-loop", "1",
                "-i", imagePath.toString(),
                "-i", audioPath.toString(),
                "-vf", "scale=" + width + ":" + height + ":force_original_aspect_ratio=increase,crop=" + width + ":" + height,
                "-t", String.valueOf(seconds),
                "-c:v", "libx264",
                "-pix_fmt", "yuv420p",
                "-c:a", "aac",
                "-shortest",
                output.toString()
        ));

        return output;
    }

    // Creates a clip for each image or video
    public Path createMediaClip(MediaItem item, int index) throws Exception {
        Path output = outputFolder.resolve("clip_" + index + ".mp4");

        // If it's an image, loop it like a video
        if (item.getType() == MediaItem.Type.IMAGE) {
            runCommand(List.of(
                    "ffmpeg", "-y",
                    "-loop", "1",
                    "-i", item.getPath().toString(),
                    "-i", item.getAudioPath().toString(),
                    "-vf", "scale=" + width + ":" + height + ":force_original_aspect_ratio=increase,crop=" + width + ":" + height,
                    "-t", String.valueOf(secondsPerImage),
                    "-c:v", "libx264",
                    "-pix_fmt", "yuv420p",
                    "-c:a", "aac",
                    "-shortest",
                    output.toString()
            ));
        } else {
            // If it's already a video, just resize and add audio
            runCommand(List.of(
                    "ffmpeg", "-y",
                    "-i", item.getPath().toString(),
                    "-i", item.getAudioPath().toString(),
                    "-vf", "scale=" + width + ":" + height + ":force_original_aspect_ratio=increase,crop=" + width + ":" + height,
                    "-map", "0:v:0",
                    "-map", "1:a:0",
                    "-c:v", "libx264",
                    "-pix_fmt", "yuv420p",
                    "-c:a", "aac",
                    "-shortest",
                    output.toString()
            ));
        }

        return output;
    }

    // Creates the final map image with start and end locations
    public Path createMapCard(MediaItem first, MediaItem last, String phrase) throws Exception {
        Path finalCard = outputFolder.resolve("map_card.jpg");

        BufferedImage map = ImageIO.read(new File("world_map.png"));
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = canvas.createGraphics();

        // Background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        int mapX = 50;
        int mapY = 100;
        int mapWidth = width - 100;
        int mapHeight = 900;

        // Draw map image
        g.drawImage(map, mapX, mapY, mapWidth, mapHeight, null);

        // Convert GPS to screen position
        int x1 = (int) ((first.getLongitude() + 180.0) * (mapWidth / 360.0)) + mapX;
        int y1 = (int) ((90.0 - first.getLatitude()) * (mapHeight / 180.0)) + mapY;

        int x2 = (int) ((last.getLongitude() + 180.0) * (mapWidth / 360.0)) + mapX;
        int y2 = (int) ((90.0 - last.getLatitude()) * (mapHeight / 180.0)) + mapY;

        // Draw line between points
        g.setStroke(new BasicStroke(4f));
        g.setColor(Color.YELLOW);
        g.drawLine(x1, y1, x2, y2);

        // Draw start point
        g.setColor(Color.BLUE);
        g.fillOval(x1 - 12, y1 - 12, 24, 24);

        // Draw end point
        g.setColor(Color.RED);
        g.fillOval(x2 - 12, y2 - 12, 24, 24);

        // Labels
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.drawString("START", x1 + 18, y1 - 8);
        g.drawString("END", x2 + 18, y2 - 8);

        // Main phrase
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 44));
        drawCenteredText(g, phrase, 1160, 900, 58);

        // Coordinates text
        g.setFont(new Font("SansSerif", Font.PLAIN, 24));
        drawCenteredText(
                g,
                "Start: " + gpsText(first) + "   |   End: " + gpsText(last),
                1720,
                900,
                34
        );

        g.dispose();
        ImageIO.write(canvas, "jpg", finalCard.toFile());

        return finalCard;
    }

    // Combines all clips into one final video
    public Path buildFinalVideo(List<Path> clips) throws Exception {
        Path concatList = outputFolder.resolve("concat_list.txt");
        Path finalVideo = outputFolder.resolve("final.mp4");

        BufferedWriter writer = new BufferedWriter(new FileWriter(concatList.toFile()));

        // Write each clip into a list file
        for (Path clip : clips) {
            writer.write("file '" + clip.toAbsolutePath() + "'");
            writer.newLine();
        }
        writer.close();

        // Use ffmpeg to merge everything
        runCommand(List.of(
                "ffmpeg", "-y",
                "-f", "concat",
                "-safe", "0",
                "-i", concatList.toString(),
                "-c", "copy",
                finalVideo.toString()
        ));

        return finalVideo;
    }

    // Formats GPS nicely
    private String gpsText(MediaItem item) {
        if (item == null || !item.hasGps()) {
            return "unknown";
        }
        return String.format("%.5f, %.5f", item.getLatitude(), item.getLongitude());
    }

    // Draws text centered and splits it into lines if needed
    private void drawCenteredText(Graphics2D g, String text, int startY, int maxWidth, int lineHeight) {
        FontMetrics fm = g.getFontMetrics();
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        int y = startY;

        for (String word : words) {
            String test = line.length() == 0 ? word : line + " " + word;

            if (fm.stringWidth(test) > maxWidth) {
                int x = (width - fm.stringWidth(line.toString())) / 2;
                g.drawString(line.toString(), x, y);
                line = new StringBuilder(word);
                y += lineHeight;
            } else {
                line = new StringBuilder(test);
            }
        }

        if (line.length() > 0) {
            int x = (width - fm.stringWidth(line.toString())) / 2;
            g.drawString(line.toString(), x, y);
        }
    }

    // Runs terminal commands like ffmpeg or say
    private void runCommand(List<String> command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        while (reader.readLine() != null) {
        }

        int exit = process.waitFor();

        // If something fails, throw error
        if (exit != 0) {
            throw new RuntimeException("Command failed: " + String.join(" ", command));
        }
    }
}