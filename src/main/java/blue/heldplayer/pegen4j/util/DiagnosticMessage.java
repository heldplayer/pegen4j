package blue.heldplayer.pegen4j.util;

import blue.heldplayer.pegen4j.parser.SourceLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NotNullByDefault
public final class DiagnosticMessage {

  private static final String COLOR_RED = "\033[91m";
  private static final String COLOR_GREEN = "\033[92m";
  private static final String COLOR_YELLOW = "\033[93m";
  private static final String COLOR_BLUE = "\033[94m";
  private static final String COLOR_RESET = "\033[0m";

  public enum Severity {
    ERROR(COLOR_RED),
    WARNING(COLOR_YELLOW),
    NOTE(COLOR_BLUE);

    private final String color;

    Severity(String color) {
      this.color = color;
    }
  }

  private final Severity severity;
  private final String message;
  private final @Nullable SourceLocation location;
  private final List<DiagnosticMessage> notes = new ArrayList<>();

  public DiagnosticMessage(Severity severity, @Nullable SourceLocation location, String message) {
    this.severity = severity;
    this.location = location;
    this.message = message;
  }

  public DiagnosticMessage(Severity severity, String message) {
    this(severity, null, message);
  }

  public DiagnosticMessage addNote(@Nullable SourceLocation location, String message) {
    this.notes.add(new DiagnosticMessage(Severity.NOTE, location, message));
    return this;
  }

  @Contract(pure = true)
  public String getMessage() {
    return this.message;
  }

  @Contract(pure = true)
  public @Nullable SourceLocation getLocation() {
    return this.location;
  }

  @Contract(pure = true)
  public List<DiagnosticMessage> getNotes() {
    return Collections.unmodifiableList(this.notes);
  }

  @Contract(pure = true)
  public static String render(Collection<DiagnosticMessage> messages, CharSequence source, boolean outputColor) {
    return messages
      .stream()
      .flatMap(m -> Stream.concat(Stream.of(m), m.getNotes().stream()))
      .map(m -> m.render(source, outputColor))
      .collect(Collectors.joining("\n"));
  }

  private String render(CharSequence source, boolean outputColor) {
    var label = this.severity.name().toLowerCase(Locale.ROOT);
    var severityText = outputColor ? this.severity.color + label + ":" + COLOR_RESET : label + ":";

    if (this.location == null) {
      return severityText + " " + this.message + "\n";
    }

    var slice = lineAt(source, this.location.startPos());
    return """
      %s:%d:%d: %s %s
       %s
      %s%s
      """.formatted(
      this.location.uri() != null ? this.location.uri().toString() : "<input>",
      this.location.startLine(),
      slice.caretColumn() + 1,
      severityText,
      this.message,
      slice.text(),
      " ".repeat(slice.caretColumn() + 1),
      outputColor ? COLOR_GREEN + "^" + COLOR_RESET : "^"
    );
  }

  private record LineSlice(String text, int caretColumn) {
  }

  private static LineSlice lineAt(CharSequence source, int position) {
    var len = source.length();
    var p = Math.clamp(position, 0, len);
    var start = p;
    while (start > 0 && source.charAt(start - 1) != '\n') {
      start--;
    }
    var end = p;
    while (end < len && source.charAt(end) != '\n') {
      end++;
    }
    var displayEnd = (end > start && source.charAt(end - 1) == '\r') ? end - 1 : end;
    return new LineSlice(source.subSequence(start, displayEnd).toString(), p - start);
  }

}
