package dev.drme.rugdar.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.drme.rugdar.dto.Analysis;
import dev.drme.rugdar.repository.AnalysisRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class MarketAnalysisHandlerTest {

    private final AnalysisRepository repository = mock(AnalysisRepository.class);
    private final MarketAnalysisHandler handler = new MarketAnalysisHandler(
            new ObjectMapper().registerModule(new JavaTimeModule()), repository);

    @Test
    void connectionSendsHistoryFromDb() throws Exception {
        Analysis old = analysis("old analysis");
        when(repository.findLatest(20)).thenReturn(List.of(old));

        WebSocketSession session = session();
        handler.afterConnectionEstablished(session);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        assertThat(captor.getAllValues().stream().map(TextMessage::getPayload).toList())
                .anyMatch(p -> containsAll(p, "\"type\":\"analysis_history\"", "\"model\":\"model-x\"", "\"message\":\"old analysis\""));
    }

    @Test
    void broadcastsAnalysisToConnectedSessions() throws Exception {
        WebSocketSession session = session();
        handler.afterConnectionEstablished(session);

        handler.onAnalysis(analysis("bullish"));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        assertThat(captor.getAllValues().stream().map(TextMessage::getPayload).toList())
                .anyMatch(p -> containsAll(p, "\"type\":\"analysis\"", "\"message\":\"bullish\""));
    }

    @Test
    void disconnectedSessionReceivesNothing() throws Exception {
        WebSocketSession session = session();
        handler.afterConnectionEstablished(session);
        handler.afterConnectionClosed(session, org.springframework.web.socket.CloseStatus.NORMAL);

        handler.onAnalysis(analysis("bullish"));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        assertThat(captor.getAllValues().stream().map(TextMessage::getPayload).toList())
                .noneMatch(p -> p.contains("\"type\":\"analysis\""));
    }

    private static WebSocketSession session() {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws/analysis"));
        return session;
    }

    private static Analysis analysis(String message) {
        return new Analysis(UUID.randomUUID(), "model-x", Instant.now(), message);
    }

    private static boolean containsAll(String text, String... parts) {
        for (String part : parts) {
            if (!text.contains(part)) {
                return false;
            }
        }
        return true;
    }
}
