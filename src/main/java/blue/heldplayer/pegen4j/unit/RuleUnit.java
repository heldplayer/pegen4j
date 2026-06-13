package blue.heldplayer.pegen4j.unit;

import blue.heldplayer.pegen4j.peg.ast.RuleDefinition;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;

@NotNullByDefault
public final class RuleUnit {

  private final RuleDefinition node;
  private final String contextClassName;
  private final List<AltUnit> alts;

  public RuleUnit(RuleDefinition node, String contextClassName, List<AltUnit> alts) {
    this.node = node;
    this.contextClassName = contextClassName;
    this.alts = alts;
  }

  public RuleDefinition getNode() {
    return this.node;
  }

  public String getName() {
    return this.node.name.string();
  }

  public String getContextClassName() {
    return this.contextClassName;
  }

  public List<AltUnit> getAlts() {
    return this.alts;
  }

  public String getTypeName() {
    var typeName = this.node.typeName();
    if (typeName != null) {
      return typeName;
    }
    String inferred = null;
    for (var alt : this.alts) {
      var fields = alt.getContextFields();
      var produced = alt.getAction() == null && fields.size() == 1 ? fields.getFirst().type() : "Object";
      if (inferred == null) {
        inferred = produced;
      } else if (!inferred.equals(produced)) {
        return "Object";
      }
    }
    if (inferred == null) {
      return "Object";
    }
    return inferred;
  }

  public boolean hasCut() {
    return this.node.hasCut();
  }

}
