package blue.heldplayer.pegen4j.parser.node;

import java.util.List;

/**
 * Node type for the concrete syntax tree, part of the parsing process and implemented by Pegen4J.
 */
public abstract sealed class ParseNode extends BaseNode permits RuleContext, StringTokenNode {

  /**
   * @return Children of the node, if any, in order of occurrence.
   */
  public List<ParseNode> children() {
    return List.of();
  }

}
