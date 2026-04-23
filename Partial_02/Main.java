import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        try {
            // Get the API key from the environment variables
            String apiKey = System.getenv("GeminiToken");

            // If the key is missing, show an error and stop the program
            if (apiKey == null || apiKey.isEmpty()) {
                System.out.println("Error: GeminiToken environment variable not found.");
                System.out.println("Run: export GeminiToken=\"YOUR_API_KEY\"");
                return;
            }

            // Create the main controller of the project
            // input_media = where the user puts images/videos
            // output = where the program saves everything
            // 1080x1920 = vertical video format
            // 4 = seconds per image
            ProjectRunner runner = new ProjectRunner(
                    apiKey,
                    Paths.get("input_media"),
                    Paths.get("output"),
                    1080,
                    1920,
                    4
            );

            // Start the whole process (this runs everything)
            runner.run();

            // Message when everything finishes
            System.out.println("Project finished");

        } catch (Exception e) {
            // If something breaks, print the error to understand what happened
            e.printStackTrace();
        }
    }
}