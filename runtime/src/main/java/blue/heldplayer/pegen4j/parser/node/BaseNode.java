package blue.heldplayer.pegen4j.parser.node;

import blue.heldplayer.pegen4j.parser.SourceLocation;
import blue.heldplayer.pegen4j.parser.SourceLocationProvider;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

public sealed abstract class BaseNode implements SourceLocationProvider permits ParseNode, CustomNode {

  private @Nullable SourceLocation location;

  public void setLocation(URI uri, int startPos, int endPos, int startLine, int startColumn, int endLine, int endColumn) {
    this.location = new SourceLocation(uri, startPos, endPos, startLine, startColumn, endLine, endColumn);
  }

  public static <T extends BaseNode> T copyLocation(T into, BaseNode from) {
    ((BaseNode) into).location = from.location;
    return into;
  }

  @Override
  public @Nullable SourceLocation toSourceLocation() {
    return this.location;
  }

}
