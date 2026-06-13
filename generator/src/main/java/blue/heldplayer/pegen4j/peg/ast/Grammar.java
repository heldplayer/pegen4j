package blue.heldplayer.pegen4j.peg.ast;

import blue.heldplayer.pegen4j.parser.node.CustomNode;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;

@NotNullByDefault
public final class Grammar extends CustomNode {
  public final List<GrammarElement> elements;

  public Grammar(List<GrammarElement> elements) {
    this.elements = elements;
  }

}
