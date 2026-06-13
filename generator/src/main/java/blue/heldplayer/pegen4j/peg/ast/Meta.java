package blue.heldplayer.pegen4j.peg.ast;

import blue.heldplayer.pegen4j.parser.node.PatternTokenNode;
import blue.heldplayer.pegen4j.parser.node.StringTokenNode;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;

@NotNullByDefault
public sealed abstract class Meta extends GrammarElement {

  public static final class Recover extends Meta {
    // STRING tokens, so the elements are PatternTokenNode (a StringTokenNode).
    public final List<PatternTokenNode> tokens;

    public Recover(List<PatternTokenNode> tokens) {
      this.tokens = tokens;
    }

  }

  public static final class Keyword extends Meta {
    public final StringTokenNode value;

    public Keyword(StringTokenNode value) {
      this.value = value;
    }

  }

  public static final class Ignore extends Meta {
    public final StringTokenNode name;

    public Ignore(StringTokenNode name) {
      this.name = name;
    }

  }

  public static final class Option extends Meta {
    public final StringTokenNode name;
    public final StringTokenNode value;

    public Option(StringTokenNode name, StringTokenNode value) {
      this.name = name;
      this.value = value;
    }

  }

}
