package org.rugdar.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class IdService {

    private static final long VERSION = 8L;              // 4 bits: custom UUIDv8
    private static final long VARIANT = 0b10L;           // 2 bits: RFC 4122 variant
    private static final long SEQUENCE_MASK = 0xFFFL;    // 12 bits
    private static final long NODE_MASK = 0x3FFL;        // 10 bits
    private static final long RANDOM_MASK = 0xFFFFFFFFFFFFFL; // 52 bits

    private final long nodeId;

    private long lastTimestamp = -1L;
    private int sequence;

    public IdService(@Value("${rugdar.id.node-id:0}") long nodeId) {
        this.nodeId = nodeId & NODE_MASK;
    }

    public synchronized UUID next() {
        long now = System.currentTimeMillis();
        if (now == lastTimestamp) {
            sequence = (sequence + 1) & (int) SEQUENCE_MASK;
            if (sequence == 0) {
                do {
                    now = System.currentTimeMillis();
                } while (now == lastTimestamp);
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = now;

        long randomBits = UUID.randomUUID().getLeastSignificantBits() & RANDOM_MASK;

        long mostSig = (now << 16) | (VERSION << 12) | sequence;
        long leastSig = (VARIANT << 62) | (nodeId << 52) | randomBits;

        return new UUID(mostSig, leastSig);
    }

    public long timestampOf(UUID id) {
        return id.getMostSignificantBits() >>> 16;
    }

    public int sequenceOf(UUID id) {
        return (int) (id.getMostSignificantBits() & SEQUENCE_MASK);
    }

    public long nodeOf(UUID id) {
        return (id.getLeastSignificantBits() >>> 52) & NODE_MASK;
    }
}
