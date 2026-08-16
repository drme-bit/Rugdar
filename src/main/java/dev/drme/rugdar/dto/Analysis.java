package dev.drme.rugdar.dto;

import java.time.Instant;
import java.util.UUID;

public record Analysis(
        UUID aid,
        String model,
        Instant timestamp,
        String message
) {
}
