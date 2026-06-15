package blue.heldplayer.pegen4j.parser;

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

    var sourceLocation = this.location.uri() != null ? this.location.uri().toString() : "<input>";
    var slices = getLines(source, this.location);

    return sourceLocation + ":" + this.location.startLine() + ":" + this.location.startColumn() + ": " + severityText
      + " " + this.message + "\n" +
      slices.stream().map(slice -> {
        var startPos = slice.columnStart() == -1 ? 1 : slice.columnStart();
        var startChar = slice.columnStart() == -1 ? "~" : "^";
        var colorPre = outputColor ? COLOR_GREEN : "";
        var colorPost = outputColor ? COLOR_RESET : "";
        return " " + slice.text() + "\n " + " ".repeat(startPos)
          + colorPre + startChar + "~".repeat(slice.columnEnd() - startPos) + colorPost;
      }).collect(Collectors.joining("\n"));
  }

  private record LineSlice(String text, int columnStart, int columnEnd) {
  }

  private static List<LineSlice> getLines(CharSequence source, SourceLocation location) {
    var len = source.length();
    var p = Math.clamp(location.startPos(), 0, len);
    var result = new ArrayList<LineSlice>();
    while (p <= location.endPos() && p < len) {
      var lineStart = p;
      while (lineStart > 0 && source.charAt(lineStart - 1) != '\n') {
        lineStart--;
      }
      var lineEnd = p;
      while (lineEnd < len && source.charAt(lineEnd) != '\n') {
        lineEnd++;
      }
      var nextP = lineEnd + 1;
      var line = source.subSequence(lineStart, lineEnd).toString().stripTrailing();
      result.add(new LineSlice(line, result.isEmpty() ? p - lineStart : -1, nextP < location.endPos() ? line.length() : location.endPos() - lineStart));
      p = nextP;
    }
    return result;
  }

}
