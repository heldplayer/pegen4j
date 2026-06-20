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
    public final List<? extends StringTokenNode> values;

    public Keyword(List<? extends StringTokenNode> values) {
      this.values = values;
    }

  }

  public static final class Ignore extends Meta {
    public final List<? extends StringTokenNode> names;

    public Ignore(List<? extends StringTokenNode> names) {
      this.names = names;
    }

  }

  public static final class Option extends Meta {
    public final StringTokenNode name;
    public final List<? extends StringTokenNode> values;

    public Option(StringTokenNode name, List<? extends StringTokenNode> values) {
      this.name = name;
      this.values = values;
    }

  }

}
