package blue.heldplayer.pegen4j.unit;

import blue.heldplayer.pegen4j.peg.ast.RuleDefinition;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@NotNullByDefault
public final class RuleUnit {

  private final RuleDefinition node;
  private final String contextClassName;
  private final List<AltUnit> alts;

  @Nullable
  private String typeName;

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

  public void setTypeName(String typeName) {
    this.typeName = typeName;
  }

  public String getTypeName() {
    assert this.typeName != null : "type not resolved";
    return this.typeName;
  }

  public boolean hasCut() {
    return this.node.hasCut();
  }

}
