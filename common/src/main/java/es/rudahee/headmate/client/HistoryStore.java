package es.rudahee.headmate.client;

import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * "La última vez": quién jugó, dónde y cuándo, con su nota de salida.
 * Se guarda en este PC: .minecraft/headmate/history.json
 */
public final class HistoryStore {
    private static final int MAX_SESSIONS = 200;
    private static @Nullable HistoryStore instance;

    public static final class Session {
        /** "world:&lt;carpeta&gt;" o "server:&lt;dirección&gt;". */
        public String placeKey;
        public String placeName;
        public boolean server;
        public String alterId;
        public String alterName;
        public int alterColor;
        public long start;
        public long end;
        public @Nullable String dimension;
        public int x;
        public int y;
        public int z;
        public @Nullable String exitNote;
    }

    private final Path file;
    private final List<Session> sessions = new ArrayList<>();

    private HistoryStore(Path file) {
        this.file = file;
        List<Session> read = JsonFiles.read(file, new TypeToken<List<Session>>() { }.getType());
        if (read != null) {
            read.stream().filter(s -> s != null && s.placeKey != null && s.alterId != null).forEach(sessions::add);
        }
    }

    public static synchronized HistoryStore get() {
        if (instance == null) {
            instance = new HistoryStore(ClientPaths.root().resolve("history.json"));
        }
        return instance;
    }

    public void add(Session session) {
        sessions.add(session);
        sessions.sort((a, b) -> Long.compare(a.end, b.end));
        while (sessions.size() > MAX_SESSIONS) {
            sessions.remove(0);
        }
        JsonFiles.write(file, sessions);
    }

    /** Más recientes primero. */
    public List<Session> all() {
        List<Session> copy = new ArrayList<>(sessions);
        Collections.reverse(copy);
        return copy;
    }

    public @Nullable Session lastAt(String placeKey) {
        for (int i = sessions.size() - 1; i >= 0; i--) {
            if (sessions.get(i).placeKey.equals(placeKey)) {
                return sessions.get(i);
            }
        }
        return null;
    }

    public void replace(List<Session> imported) {
        sessions.clear();
        imported.stream().filter(s -> s != null && s.placeKey != null && s.alterId != null).forEach(sessions::add);
        sessions.sort((a, b) -> Long.compare(a.end, b.end));
        JsonFiles.write(file, sessions);
    }

    public void clear() {
        sessions.clear();
        JsonFiles.write(file, sessions);
    }

    /** JSON para exportar. */
    String toJson() {
        return JsonFiles.GSON.toJson(sessions);
    }

    static List<Session> parse(String json) {
        List<Session> read = JsonFiles.GSON.fromJson(json, new TypeToken<List<Session>>() { }.getType());
        return read == null ? List.of() : read;
    }
}
