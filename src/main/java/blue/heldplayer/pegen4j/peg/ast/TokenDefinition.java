package blue.heldplayer.pegen4j.peg.ast;

import blue.heldplayer.pegen4j.parser.node.StringTokenNode;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public final class TokenDefinition extends GrammarElement {
  public final StringTokenNode name;
  public final StringTokenNode pattern;

  public TokenDefinition(StringTokenNode name, StringTokenNode pattern) {
    this.name = name;
    this.pattern = pattern;
  }

  public String regex() {
    String patternString = this.pattern.string();
    assert patternString.length() >= 2 && patternString.startsWith("/") && patternString.endsWith("/");
    return patternString.substring(1, patternString.length() - 1);
  }

}
