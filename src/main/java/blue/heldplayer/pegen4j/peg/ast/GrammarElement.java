package blue.heldplayer.pegen4j.peg.ast;

import blue.heldplayer.pegen4j.parser.node.CustomNode;

public sealed abstract class GrammarElement extends CustomNode permits Meta, RuleDefinition, TokenDefinition {
}
