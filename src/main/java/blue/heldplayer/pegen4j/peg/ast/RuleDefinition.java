package blue.heldplayer.pegen4j.peg.ast;

import blue.heldplayer.pegen4j.parser.node.StringTokenNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

@NotNullByDefault
public final class RuleDefinition extends GrammarElement {

  public final StringTokenNode name;
  public final List<Alt> alts;
  public final @Nullable StringTokenNode type;

  public RuleDefinition(StringTokenNode name, List<Alt> alts, @Nullable StringTokenNode type) {
    this.name = name;
    this.alts = alts;
    this.type = type;
  }

  @Contract(pure = true)
  @Nullable
  public String typeName() {
    if (this.type == null) {
      return null;
    }
    var typeString = this.type.string();
    assert typeString.length() >= 2 && typeString.startsWith("[") && typeString.endsWith("]");
    return typeString.substring(1, typeString.length() - 1).trim();
  }

  @Contract(pure = true)
  public boolean isNullable(Predicate<String> ruleIsNullable) {
    return this.alts.stream().anyMatch(alt -> alt.items.stream().allMatch(item -> item.isNullable(ruleIsNullable)));
  }

  @Contract(pure = true)
  public boolean hasCut() {
    return this.alts.stream().anyMatch(Alt::hasCut);
  }

}
