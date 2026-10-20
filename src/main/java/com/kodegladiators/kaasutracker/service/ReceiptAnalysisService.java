package com.kodegladiators.kaasutracker.service;

import com.kodegladiators.kaasutracker.dto.ExpenseAnalysisResponse;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReceiptAnalysisService {

    private static final Pattern DATA_URI_PATTERN =
            Pattern.compile("^data:(image/(?:png|jpe?g|gif|webp));base64,(.*)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final String SYSTEM_PROMPT = """
            Analyze the provided receipt image and extract its total amount paid, merchant, expense category,
            transaction date, and confidence score.
            Return only one valid JSON object matching the supplied ExpenseAnalysisResponse schema. Do not
            include markdown, code fences, explanations, or additional properties. Use a JSON number for amount,
            a string from 0.0 to 1.0 for confidenceScore as a number, and an ISO-8601 date for date.
            If a field cannot be read reliably, return null for that field. Do not guess missing values.
            """;

    private final ChatClient chatClient;
    private final BeanOutputConverter<ExpenseAnalysisResponse> outputConverter =
            new BeanOutputConverter<>(ExpenseAnalysisResponse.class);

    public ReceiptAnalysisService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public ExpenseAnalysisResponse analyzeReceipt(String base64Image) {
        if (base64Image == null || base64Image.isBlank()) {
            throw new IllegalArgumentException("Receipt image must not be blank");
        }

        DecodedImage image = decodeImage(base64Image);
        String response;
        try {
            response = chatClient.prompt()
                    .system(SYSTEM_PROMPT + "\nRequired JSON schema:\n" + outputConverter.getFormat())
                    .user(user -> user
                            .text("Extract the requested expense details from this receipt image.")
                            .media(image.mimeType(), new ByteArrayResource(image.bytes())))
                    .call()
                    .content();
        } catch (RuntimeException exception) {
            if (hasTimeoutCause(exception)) {
                throw new ResponseStatusException(
                        HttpStatus.GATEWAY_TIMEOUT,
                        "Receipt analysis timed out",
                        exception
                );
            }
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Receipt analysis model request failed",
                    exception
            );
        }

        if (response == null || response.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Receipt analysis model returned an empty response"
            );
        }

        ExpenseAnalysisResponse result;
        try {
            result = outputConverter.convert(response);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Receipt analysis model returned an invalid response",
                    exception
            );
        }

        validateResponse(result);
        return result;
    }

    private static void validateResponse(ExpenseAnalysisResponse response) {
        if (response == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Receipt analysis model returned no structured expense"
            );
        }
        if (response.amount() != null && response.amount().signum() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Receipt analysis model returned a non-positive amount"
            );
        }
        if (response.confidenceScore() != null
                && (response.confidenceScore() < 0.0 || response.confidenceScore() > 1.0)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Receipt analysis model returned an invalid confidence score"
            );
        }
    }

    private static boolean hasTimeoutCause(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof TimeoutException
                    || cause instanceof SocketTimeoutException
                    || cause instanceof HttpTimeoutException) {
                return true;
            }
            if (cause instanceof ResourceAccessException) {
                String message = cause.getMessage();
                if (message != null) {
                    String normalizedMessage = message.toLowerCase(Locale.ROOT);
                    if (normalizedMessage.contains("timeout") || normalizedMessage.contains("timed out")) {
                        return true;
                    }
                }
            }
            cause = cause.getCause();
        }
        return false;
    }

    private static DecodedImage decodeImage(String base64Image) {
        String input = base64Image.trim();
        String declaredMimeType = null;
        Matcher dataUriMatcher = DATA_URI_PATTERN.matcher(input);
        if (dataUriMatcher.matches()) {
            declaredMimeType = normalizeMimeType(dataUriMatcher.group(1));
            input = dataUriMatcher.group(2);
        } else if (input.startsWith("data:")) {
            throw new IllegalArgumentException("Receipt data URI must contain a supported base64 image");
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(input.replaceAll("\\s", ""));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Receipt image is not valid base64", exception);
        }
        if (bytes.length == 0) {
            throw new IllegalArgumentException("Receipt image must not be empty");
        }

        String detectedMimeType = detectMimeType(bytes);
        if (declaredMimeType != null && !declaredMimeType.equals(detectedMimeType)) {
            throw new IllegalArgumentException("Receipt data URI content type does not match the image data");
        }

        return new DecodedImage(bytes, MimeTypeUtils.parseMimeType(detectedMimeType));
    }

    private static String detectMimeType(byte[] bytes) {
        if (hasPrefix(bytes, 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)) {
            return "image/png";
        }
        if (hasPrefix(bytes, 0xff, 0xd8, 0xff)) {
            return "image/jpeg";
        }
        if (hasAsciiPrefix(bytes, "GIF87a") || hasAsciiPrefix(bytes, "GIF89a")) {
            return "image/gif";
        }
        if (hasAsciiPrefix(bytes, "RIFF") && hasAsciiAt(bytes, 8, "WEBP")) {
            return "image/webp";
        }
        throw new IllegalArgumentException("Receipt image must be PNG, JPEG, GIF, or WebP");
    }

    private static boolean hasPrefix(byte[] bytes, int... prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if ((bytes[index] & 0xff) != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasAsciiPrefix(byte[] bytes, String prefix) {
        return hasAsciiAt(bytes, 0, prefix);
    }

    private static boolean hasAsciiAt(byte[] bytes, int offset, String value) {
        if (bytes.length < offset + value.length()) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            if (bytes[offset + index] != value.charAt(index)) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeMimeType(String mimeType) {
        String normalized = mimeType.toLowerCase(Locale.ROOT);
        return normalized.equals("image/jpg") ? "image/jpeg" : normalized;
    }

    private record DecodedImage(byte[] bytes, MimeType mimeType) {
    }
}
