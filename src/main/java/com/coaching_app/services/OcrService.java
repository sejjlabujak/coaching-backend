package com.coaching_app.services;

import com.coaching_app.dto.OcrConfirmDTO;
import com.coaching_app.dto.OcrUploadResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;

@Service
public class OcrService {
    @Value("${openrouter.api.key}")
    private String openRouterApiKey;

    private static final boolean USE_OPENROUTER = false;

    @Value("${tesseract.data.path}")
    private String tessDataPath;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final ExecutorService executor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() - 1)
    );

    private final ObjectMapper objectMapper = new ObjectMapper();


    public void processBytes(byte[] bytes, String contentType, String jobId) {
        try {
            String rawText;

            if ("application/pdf".equals(contentType)) {
                try (PDDocument document = PDDocument.load(bytes)) {
                    rawText = extractTextFromPdfParallel(document);
                }
            } else {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                rawText = buildTesseract().doOCR(image);
            }

            List<OcrConfirmDTO> drills = extractAllDrillsWithGemini(rawText);

            // Store drills for review
            OcrJobStore.putDrills(jobId, drills);

            // Store summary response so status endpoint knows we're done
            OcrUploadResponseDTO summary = new OcrUploadResponseDTO(
                    "Extraction Complete",
                    drills.size() + " drills extracted",
                    1.0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            OcrJobStore.put(jobId, summary);

        } catch (Exception e) {
            // Store error so status endpoint returns something
            OcrUploadResponseDTO error = new OcrUploadResponseDTO(
                    "Error",
                    "OCR processing failed: " + e.getMessage(),
                    0.0,
                    null, null, null, null, null, null
            );
            OcrJobStore.put(jobId, error);
            throw new RuntimeException("OCR processing failed: " + e.getMessage(), e);
        }
    }

    // Parallel PDF extraction

    private String extractTextFromPdfParallel(PDDocument document)
            throws InterruptedException, ExecutionException {

        PDFRenderer renderer = new PDFRenderer(document);
        int pageCount = document.getNumberOfPages();
        List<Future<String>> futures = new ArrayList<>(pageCount);

        for (int i = 0; i < pageCount; i++) {
            final int pageIndex = i;
            futures.add(executor.submit(() -> {
                Tesseract tesseract = buildTesseract();
                BufferedImage image;
                synchronized (renderer) {
                    image = renderer.renderImageWithDPI(pageIndex, 150);
                }
                return tesseract.doOCR(image);
            }));
        }

        StringBuilder fullText = new StringBuilder();
        for (int i = 0; i < futures.size(); i++) {
            fullText.append("\n--- PAGE ").append(i + 1).append(" ---\n");
            fullText.append(futures.get(i).get());
            System.out.println("✓ Page " + (i + 1) + "/" + futures.size() + " collected");
        }

        return fullText.toString();
    }

    // Extract all drills from chunked text

    private List<OcrConfirmDTO> extractAllDrillsWithGemini(String rawText) throws Exception {

        List<String> chunks = splitIntoChunks(rawText, 20000, 1000);
        List<OcrConfirmDTO> allDrills = new ArrayList<>();
        Set<String> seenTitles = new HashSet<>();

        System.out.println(">>> Sending " + chunks.size() + " chunks to " + (USE_OPENROUTER ? "OpenRouter" : "Gemini") + "...");

        for (int i = 0; i < chunks.size(); i++) {
            System.out.println(">>> Processing chunk " + (i + 1) + "/" + chunks.size());

            String prompt = """
                You are a basketball coaching assistant. Analyze this OCR-extracted text chunk from a basketball drill book.
                Find ALL basketball drills in this text chunk and extract their details.
                Return ONLY a valid JSON array, no explanation, no markdown, no backticks.
                If no drills are found in this chunk, return an empty array: []
                
                For each drill extract:
                - title: the drill name (string)
                - description: what the drill involves (string, max 500 chars)
                - focus: ONE of exactly: SHOOTING, DEFENSE, OFFENSE, CONDITIONING, RECOVERY, REBOUNDING, TEAM_BUILDING
                - intensity: ONE of exactly: LOW, MEDIUM, HIGH
                - ageGroup: ONE of exactly: U10, U12, U14, U16, U18, SENIOR (null if unknown)
                - duration: estimated minutes as integer (null if unknown)
                - equipment: comma-separated equipment list (null if none mentioned)
                - level: ONE of exactly: Beginner, Intermediate, Advanced
                
                OCR Text chunk:
                """ + chunks.get(i) + """
                
                Respond with only a JSON array:
                [
                  {
                    "title": "...",
                    "description": "...",
                    "focus": "...",
                    "intensity": "...",
                    "ageGroup": null,
                    "duration": null,
                    "equipment": null,
                    "level": "..."
                  }
                ]
                """;

            String requestBody;
            HttpRequest request;
            HttpClient client = HttpClient.newHttpClient();

            if (USE_OPENROUTER) {
                requestBody = objectMapper.writeValueAsString(new java.util.HashMap<>() {{
                    put("model", "meta-llama/llama-3.3-70b-instruct:free");
                    put("messages", List.of(new java.util.HashMap<>() {{
                        put("role", "user");
                        put("content", prompt);
                    }}));
                }});

                request = HttpRequest.newBuilder()
                        .uri(URI.create("https://openrouter.ai/api/v1/chat/completions"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + openRouterApiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();
            } else {
                requestBody = objectMapper.writeValueAsString(new java.util.HashMap<>() {{
                    put("contents", List.of(new java.util.HashMap<>() {{
                        put("parts", List.of(new java.util.HashMap<>() {{
                            put("text", prompt);
                        }}));
                    }}));
                }});

                request = HttpRequest.newBuilder()
                        .uri(URI.create(
                                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
                                        + geminiApiKey))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();
            }

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429) {
                int waitSeconds = 60;
                try {
                    JsonNode errorRoot = objectMapper.readTree(response.body());
                    JsonNode details = errorRoot.path("error").path("details");
                    for (JsonNode detail : details) {
                        if (detail.has("retryDelay")) {
                            String retryDelay = detail.path("retryDelay").asText();
                            waitSeconds = Integer.parseInt(retryDelay.replace("s", "")) + 10;
                        }
                    }
                } catch (Exception ignored) {}

                System.out.println(">>> Rate limited on chunk " + (i + 1) + ", waiting " + waitSeconds + " seconds...");
                Thread.sleep(waitSeconds * 1000L);
                i--;
                continue;
            }

            if (response.statusCode() != 200) {
                System.out.println(">>> Chunk " + (i + 1) + " failed: " + response.body());
                continue;
            }

            String content;
            if (USE_OPENROUTER) {
                JsonNode root = objectMapper.readTree(response.body());
                content = root
                        .path("choices").get(0)
                        .path("message")
                        .path("content")
                        .asText();
            } else {
                JsonNode root = objectMapper.readTree(response.body());
                content = root
                        .path("candidates").get(0)
                        .path("content")
                        .path("parts").get(0)
                        .path("text")
                        .asText();
            }

            content = content.replaceAll("```json|```", "").trim();

            JsonNode drillsArray = objectMapper.readTree(content);
            if (drillsArray.isArray()) {
                for (JsonNode drillNode : drillsArray) {
                    String title = nullableText(drillNode, "title");
                    if (title == null || seenTitles.contains(title.toLowerCase())) {
                        continue;
                    }
                    seenTitles.add(title.toLowerCase());

                    OcrConfirmDTO dto = new OcrConfirmDTO();
                    dto.setTitle(title);
                    dto.setDescription(nullableText(drillNode, "description"));
                    dto.setFocus(nullableText(drillNode, "focus"));
                    dto.setIntensity(nullableText(drillNode, "intensity"));
                    dto.setAgeGroup(nullableText(drillNode, "ageGroup"));
                    dto.setLevel(nullableText(drillNode, "level"));
                    dto.setEquipment(nullableText(drillNode, "equipment"));
                    allDrills.add(dto);
                }
            }

            System.out.println(">>> Drills found so far: " + allDrills.size());
            Thread.sleep(8000);
        }

        System.out.println(">>> Total drills extracted: " + allDrills.size());
        return allDrills;
    }

    private List<String> splitIntoChunks(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            start += chunkSize - overlap;
        }
        return chunks;
    }

    private String nullableText(JsonNode node, String field) {
        JsonNode n = node.path(field);
        return (n.isNull() || n.isMissingNode()) ? null : n.asText();
    }

    // Tesseract factory

    private Tesseract buildTesseract() {
        Tesseract t = new Tesseract();
        t.setDatapath("C:/Program Files/Tesseract-OCR/tessdata");
        t.setLanguage("eng");
        t.setPageSegMode(6);
        t.setOcrEngineMode(1);
        t.setVariable("tessedit_char_whitelist",
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,;:!?'-/() \n");
        return t;
    }
}