package Parcial_01;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

//This class stores the current image, applies the different methods on it and gives out the final image.
 public class ImageEditor {

    // Declares the variable where the image is gonna be stored, called "img"
    private BufferedImage img;

    /* This is the constructor method that receives the image path in the parenthesis, 
    creates a File object that represents the image,
    and uses ImageIO.read() to load it as a BufferedImage. IOException is used if the image can't be found. */
    public ImageEditor(String path) throws IOException {
        img = ImageIO.read(new File(path));
    }
    // Returns the width of the image to edit
    public int getWidth() {
        return img.getWidth();
    }

    // Returns the height of the image to edit
    public int getHeight() {
        return img.getHeight();
    }
    // THE CROP METHOD: creates a new image with the selected rectangular region.
    public void crop(int x1, int y1, int x2, int y2) {

        // Given the two x and y coordinates the left, right, top and bottom sides of the rectangle are defined
        int left = Math.min(x1, x2);
        int right = Math.max(x1, x2);
        int top = Math.min(y1, y2);
        int bottom = Math.max(y1, y2);

        // Given the sides, the width and height are formulated
        int width = right - left;
        int height = bottom - top;

        // The operation can't be done if the width or height is negative, it doesn't exist
        if (width <= 0 || height <= 0) {
            System.out.println("Invalid region");
            return;
        }
        // Creates a new image for the cropped region
        BufferedImage cropped = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Copies pixels from original image to new image
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                cropped.setRGB(x, y, img.getRGB(left + x, top + y));
            }
        }

        // Replaces the original image with cropped version
        img = cropped;
        System.out.println("Cropped");
    }

    // THE INVERTED METHOD: inverts the colors of a given region, the new color == 255 - originalColor
    public void invert(int x1, int y1, int x2, int y2) {

        // Same procedure for rectangle as before
        int left = Math.min(x1, x2);
        int right = Math.max(x1, x2);
        int top = Math.min(y1, y2);
        int bottom = Math.max(y1, y2);

        int wid = right - left;
        int hei = bottom - top;
    
        if (wid <= 0 || hei <= 0) {
            System.out.println("Invalid region");
            return;
        }

        // For every pixel in the selected region, the color is inverted
        for (int x = left; x < right; x++) {
            for (int y = top; y < bottom; y++) {

                // Gets the RGB pixel value that is stored as a 32-bit integer AAAAAAAA RRRRRRRR GGGGGGGG BBBBBBBB
                int pixel = img.getRGB(x, y);

                /* Extract the color components using bit shifting
                We move bits to the right to isolate each color,
                then use & 0xFF to keep only the last 8 bits which correspond to the color we want*/

                int r = (pixel >> 16) & 0xFF;  // 8 bits for red
                int g = (pixel >> 8) & 0xFF;   // 8 bits for red
                int b = pixel & 0xFF;          // 8 bits for red

                // Each color is inverted using the formula: newColor = 255 - originalColor
                r = 255 - r;
                g = 255 - g;
                b = 255 - b;

                /* Now that we have the value for the inverted colors, we convert them back into the 32-bit integer
                and combine them using the OR operator (|) */

                int newPixel = (r << 16) | (g << 8) | b;

                // Replaces original pixel with inverted pixel
                img.setRGB(x, y, newPixel);
            }
        }
        System.out.println("Inverted");
    }

    // THE ROTATE METHOD: rotates the selected region by 90°, 180°, or 270°.
    public void rotate(int x1, int y1, int x2, int y2, int angle) {

        // Same rectangle method as before
        int left = Math.min(x1, x2);
        int right = Math.max(x1, x2);
        int top = Math.min(y1, y2);
        int bottom = Math.max(y1, y2);
    
        int w = right - left;
        int h = bottom - top;
    
        if (w <= 0 || h <= 0) {
            System.out.println("Invalid region");
            return;
        }
    
        /* Creates a temporary image to store a copy of the original region so it can read pixels from the copy 
        and implement them into the original image but rotated */
    
        // Temporary region image:
        BufferedImage region = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    
        // Copies every pixel from the original image into the temporary region image
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                region.setRGB(x, y, img.getRGB(left + x, top + y));
            }
        }

        // Image that will store the rotated version of the region
        BufferedImage rotated;
    
        // ROTATION CASES:
        // For a 180° rotation: the width and height stay the same
        if (angle == 180) {
            rotated = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    
            // Each pixel is mirrored horizontally and vertically: (x, y) becomes (w-1-x, h-1-y)
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    rotated.setRGB(w - 1 - x, h - 1 - y, region.getRGB(x, y));
                }
            }
        } 
        // For a 90° rotation, the width and height swap
        else if (angle == 90) {
            rotated = new BufferedImage(h, w, BufferedImage.TYPE_INT_RGB);
    
            // Rotation formula: (x, y) becomes (h-1-y, x)
            // This rotates the image clockwise
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    rotated.setRGB(h - 1 - y, x, region.getRGB(x, y));
                }
            }
        }
        // It is a 270° rotation: The width and height also swap.
        else if (angle == 270){
            rotated = new BufferedImage(h, w, BufferedImage.TYPE_INT_RGB);
    
            // Rotation formula: (x, y) becomes (y, w-1-x)
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    rotated.setRGB(y, w - 1 - x, region.getRGB(x, y));
                }
            }
        }
        else {
            System.out.println("Invalid rotation angle. Only 90, 180, or 270 allowed.");
            return;
        }
        /* FILL THE SELECTED RECTANGLE AREA IN GRAY: We first fill the original selected area with gray 
        so that the empty areas remain visible */
        int gray = 0x808080; // This is the gray color in the 32-bit format, in RGB it is (128,128,128)
        for (int x = left; x < right; x++) {
            for (int y = top; y < bottom; y++) {
                img.setRGB(x, y, gray);
            }
        }
    
        // DRAW ROTATED IMAGE: We then draw the rotated image on top of the original region
        for (int x = 0; x < rotated.getWidth(); x++) {
            for (int y = 0; y < rotated.getHeight(); y++) {
                // If the pixel we're analyzing is inside the region, we change it
                if (left + x < img.getWidth() && top + y < img.getHeight()) {
                    img.setRGB(left + x, top + y, rotated.getRGB(x, y));
                }
            }
        }
        System.out.println("Rotated");
    }
    // SAVE THE CREATED IMAGE:
    public void save(String path) throws IOException {
        ImageIO.write(img, "jpg", new File(path));
    }
}
