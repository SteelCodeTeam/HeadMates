package es.rudahee.headmate.client;

import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Tablón del sistema: notas que todos los alters pueden leer. .minecraft/headmate/board.json */
public final class BoardStore {
    public static final int MAX_NOTE = 500;
    private static final int MAX_NOTES = 300;
    private static @Nullable BoardStore instance;

    public static final class Note {
        public String id;
        public String authorId;
        public String authorName;
        public int authorColor;
        public String text;
        public long createdAt;
    }

    private final Path file;
    private final List<Note> notes = new ArrayList<>();

    private BoardStore(Path file) {
        this.file = file;
        List<Note> read = JsonFiles.read(file, new TypeToken<List<Note>>() { }.getType());
        if (read != null) {
            read.stream().filter(n -> n != null && n.id != null && n.text != null).forEach(notes::add);
        }
    }

    public static synchronized BoardStore get() {
        if (instance == null) {
            instance = new BoardStore(ClientPaths.root().resolve("board.json"));
        }
        return instance;
    }

    /** Más recientes primero. */
    public List<Note> all() {
        List<Note> copy = new ArrayList<>(notes);
        copy.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
        return Collections.unmodifiableList(copy);
    }

    public Note add(AlterProfile author, String text) {
        Note note = new Note();
        note.id = UUID.randomUUID().toString();
        note.authorId = author.id;
        note.authorName = author.name;
        note.authorColor = author.color;
        note.text = text.strip().substring(0, Math.min(text.strip().length(), MAX_NOTE));
        note.createdAt = System.currentTimeMillis();
        notes.add(note);
        while (notes.size() > MAX_NOTES) {
            notes.stream().min((a, b) -> Long.compare(a.createdAt, b.createdAt)).ifPresent(notes::remove);
        }
        save();
        return note;
    }

    public void remove(Note note) {
        notes.removeIf(n -> n.id.equals(note.id));
        save();
    }

    /** Añade notas importadas que no estén ya (por id). */
    public void merge(List<Note> imported) {
        for (Note note : imported) {
            if (note != null && note.id != null && note.text != null && notes.stream().noneMatch(n -> n.id.equals(note.id))) {
                notes.add(note);
            }
        }
        save();
    }

    public void replace(List<Note> imported) {
        notes.clear();
        merge(imported);
    }

    public void clear() {
        notes.clear();
        save();
    }

    String toJson() {
        return JsonFiles.GSON.toJson(notes);
    }

    static List<Note> parse(String json) {
        List<Note> read = JsonFiles.GSON.fromJson(json, new TypeToken<List<Note>>() { }.getType());
        return read == null ? List.of() : read;
    }

    private void save() {
        JsonFiles.write(file, notes);
    }
}
