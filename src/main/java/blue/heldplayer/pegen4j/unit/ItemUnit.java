package blue.heldplayer.pegen4j.unit;

import blue.heldplayer.pegen4j.peg.ast.Item;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

@NotNullByDefault
public record ItemUnit(Item item, @Nullable ConcreteNodeElement field) {

  @Contract(pure = true)
  public boolean isStored() {
    return this.field != null;
  }

  @Contract(pure = true)
  public ConcreteNodeElement assertField() {
    assert this.isStored();
    return this.field;
  }

}
