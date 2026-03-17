package Classwork.Classwork_04;

// Imports needed for files and running system commands
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class FileTranslator {

    public static void main(String[] args) {

        // Scanner to read user input from the terminal
        Scanner sc = new Scanner(System.in);

        try {
            // Asks the user for the path of the text file to translate
            System.out.print("Enter path to .txt file: ");
            String path = sc.nextLine();

            // Ask the user for the target language, for example: English
            System.out.print("Translate to language: ");
            String language = sc.nextLine();

            // Read the content of the text file
            String content = new String(Files.readAllBytes(Paths.get(path)));

            // The prompt that will be sent to the OpenAI API
            String prompt = "Translate the following text to " + language + ":\n" + content;

            // Escapes special characters so that the text can be safely inserted into JSON
            prompt = prompt
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "");

            // Get the API key from the environment variable OpenAIToken
            String token = System.getenv("OpenAIToken");

            // Check if the environment variable exists
            if (token == null || token.isEmpty()) {
                System.out.println("Error: OpenAIToken environment variable not found.");
                return;
            }

            // Build the JSON body that will be sent to the OpenAI API
            String json = "{"
                    + "\"model\":\"gpt-4o-mini\","
                    + "\"messages\":["
                    + "{\"role\":\"user\",\"content\":\"" + prompt + "\"}"
                    + "]"
                    + "}";

            // Use ProcessBuilder to execute the curl command from Java
            ProcessBuilder pb = new ProcessBuilder(
                    "curl",
                    "https://api.openai.com/v1/chat/completions",
                    "-H", "Content-Type: application/json",
                    "-H", "Authorization: Bearer " + token,
                    "-d", json
            );

            // Merge error stream with the normal output stream
            pb.redirectErrorStream(true);

            // Start the curl process
            Process process = pb.start();

            // Read the response returned by the API
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );
            String line;
            StringBuilder response = new StringBuilder();

            // Read the API response line by line
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }

            // Save the translation into a new file
            FileWriter writer = new FileWriter("translated.txt");
            writer.write(response.toString());
            writer.close();

            System.out.println("Translation saved to translated.txt");

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            sc.close();
        }
    }
}