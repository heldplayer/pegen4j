package blue.heldplayer.pegen4j.parser;

import java.net.URI;

// NOTE: make value type once this is supported in the JVM
public record SourceLocation(URI uri, int startPos, int endPos, int startLine, int startColumn, int endLine,
                             int endColumn) implements SourceLocationProvider {

  @Override
  public SourceLocation toSourceLocation() {
    return this;
  }

}
