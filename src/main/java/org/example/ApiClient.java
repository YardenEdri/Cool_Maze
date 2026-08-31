package org.example;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

/**
 * The two HTTP calls used by the app.
 *
 * Both methods throw {@link ApiException} on any failure (network error,
 * non-2xx status, unreadable body) so the UI layer can show a message dialog
 * instead of the error being swallowed here.
 */
public class ApiClient {

    private static final String BASE_URL = "https://shaitest-production-3066.up.railway.app/fm1";
    private static final String CONFIG_URL = BASE_URL + "/get-render-config";
    private static final String IMAGE_URL = BASE_URL + "/get-maze-image";

    /** Thrown when either call fails; carries a human-readable message for a dialog. */
    public static class ApiException extends RuntimeException {
        public ApiException(String message, Throwable cause) {
            super(message, cause);
        }
        public ApiException(String message) {
            super(message);
        }
    }

    /** GET /fm1/get-render-config -> RenderConfig. */
    public static RenderConfig getRenderConfig() {
        try {
            HttpResponse<String> response = Unirest.get(CONFIG_URL).asString();
            if (!response.isSuccess()) {
                throw new ApiException("Config request failed: HTTP " + response.getStatus());
            }
            JSONObject json = new JSONObject(response.getBody());
            return new RenderConfig(json);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Could not load render config: " + e.getMessage(), e);
        }
    }

    /** GET /fm1/get-maze-image?width={width}&height={height} -> decoded PNG. */
    public static BufferedImage getMazeImage(int width, int height) {
        try {
            HttpResponse<byte[]> response = Unirest.get(IMAGE_URL)
                    .queryString("width", width)
                    .queryString("height", height)
                    .asBytes();
            if (!response.isSuccess()) {
                throw new ApiException("Maze image request failed: HTTP " + response.getStatus());
            }
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.getBody()));
            if (image == null) {
                throw new ApiException("Server did not return a readable image");
            }
            return image;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Could not load maze image: " + e.getMessage(), e);
        }
    }

    /** Quick manual test for Step 3 — hits the live server. */
    public static void main(String[] args) {
        RenderConfig cfg = getRenderConfig();
        System.out.println("Config: " + cfg);

        int w = 10, h = 8;
        BufferedImage img = getMazeImage(w, h);
        int scaleX = img.getWidth() / w;
        int scaleY = img.getHeight() / h;
        System.out.println("Maze image: " + img.getWidth() + "x" + img.getHeight()
                + " (requested " + w + "x" + h + ", so " + scaleX + "x" + scaleY
                + " pixels per cell)");

        // The image is a grid of width x height cells, each drawn as a square
        // block of pixels (currently 16x16 on the live server). Step 7 derives
        // the block size as image size / requested size (see spec section 2.2).
        boolean ok = img.getWidth() % w == 0 && img.getHeight() % h == 0
                && scaleX == scaleY && scaleX >= 1;
        System.out.println(ok ? "STEP 3 OK" : "STEP 3 FAILED");
        Unirest.shutDown();
    }
}
