package Parcial_01;

import java.io.IOException;
import java.util.Scanner;

/* This class contains the main method of the program. It handles menu and inputs 
 * and calls the corresponding methods from the ImageEditor class */
public class ImageEditorApp {

    public static void main(String[] args) {

        // Scanner is used to read input
        Scanner sc = new Scanner(System.in);

        // This object will handle all image operations
        ImageEditor editor;
        try {
            // Create a new ImageEditor object
            // The constructor loads the image from the given path
            editor = new ImageEditor("Parcial_01/Interstellar.jpg");
            System.out.println("Image loaded successfully.");
            System.out.println("Resolution: " + editor.getWidth() + " x " + editor.getHeight());
        } catch (IOException e) {
            // If the image cannot be loaded, show error and exit
            System.out.println("Image not loaded");
            sc.close();
            return;
        }

        // This boolean controls the execution of the menu loop
        boolean running = true;

        // MAIN PROGRAM LOOP: It continues until the user selects Save and exit which makes the boolean false
        while (running) {

            // Display menu options
            System.out.println("\n1) Crop");
            System.out.println("2) Invert region");
            System.out.println("3) Rotate region");
            System.out.println("4) Save and exit");
            System.out.print("Option: ");

            // Reads option
            int option = sc.nextInt();

            switch (option) {
                case 1 -> {
                    // Read rectangular region coordinates
                    int[] r = readRegion(sc);

                    // Calls the crop method from ImageEditor
                    editor.crop(r[0], r[1], r[2], r[3]);
                }
                case 2 -> {
                    // Read region coordinates
                    int[] r = readRegion(sc);

                    // Calls the invert method from ImageEditor
                    editor.invert(r[0], r[1], r[2], r[3]);
                }
                case 3 -> {
                    // Read region coordinates
                    int[] r = readRegion(sc);

                    // Ask user for rotation angle
                    System.out.print("Angle (90,180,270): ");
                    int angle = sc.nextInt();

                    // Calls the rotate method from ImageEditor
                    editor.rotate(r[0], r[1], r[2], r[3], angle);
                }

                case 4 -> {
                    sc.nextLine();

                    // Asks the user for the output file name
                    System.out.print("Output name with .jpg at the end: ");
                    String name = sc.nextLine();

                    try {
                        // Save modified image in the Parcial_01 folder
                        editor.save("Parcial_01/" + name);
                        System.out.println("Saved");
                    } catch (IOException e) {
                        System.out.println("Error saving");
                    }
                    // Stops the loop and exits the program
                    running = false;
                }
                // If the user doesn't choose a valid number
                default -> System.out.println("Invalid option");
            }
        }
        sc.close();
    }

    // Method that reads x1, y1, x2, y2 from the user and returns an int array containing those four values.
    private static int[] readRegion(Scanner sc) {
        System.out.print("x1: ");
        int x1 = sc.nextInt();

        System.out.print("y1: ");
        int y1 = sc.nextInt();

        System.out.print("x2: ");
        int x2 = sc.nextInt();

        System.out.print("y2: ");
        int y2 = sc.nextInt();

        return new int[]{x1, y1, x2, y2};
    }
}