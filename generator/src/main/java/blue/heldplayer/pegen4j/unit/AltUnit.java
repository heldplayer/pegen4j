package blue.heldplayer.pegen4j.unit;

import blue.heldplayer.pegen4j.peg.ast.Alt;
import blue.heldplayer.pegen4j.peg.ast.Item;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@NotNullByDefault
public final class AltUnit {

  private final Alt node;
  private final String simpleContextClassName;
  private final String qualifiedContextClassName;
  private final List<ItemUnit> items;

  public AltUnit(Alt node, String simpleContextClassName, String qualifiedContextClassName, List<ItemUnit> items) {
    this.node = node;
    this.simpleContextClassName = simpleContextClassName;
    this.qualifiedContextClassName = qualifiedContextClassName;
    this.items = items;
  }

  public Alt getNode() {
    return this.node;
  }

  public String getSimpleContextClassName() {
    return this.simpleContextClassName;
  }

  public String getQualifiedContextClassName() {
    return this.qualifiedContextClassName;
  }

  public List<ItemUnit> getItems() {
    return this.items;
  }

  public List<ConcreteNodeElement> getContextFields() {
    return this.items.stream().filter(ItemUnit::isStored).map(ItemUnit::assertField).toList();
  }

  public @Nullable String getLabelName() {
    return this.node.labelName();
  }

  public @Nullable String getAction() {
    return this.node.action();
  }

  public boolean hasCut() {
    return this.node.hasCut();
  }

  public boolean alwaysReturns() {
    for (var itemUnit : this.items) {
      switch (itemUnit.item().target()) {
        case Item.Cut _, Item.Repeat0 _, Item.Opt _ -> {
        }
        default -> {
          return false;
        }
      }
    }
    return true;
  }

}
