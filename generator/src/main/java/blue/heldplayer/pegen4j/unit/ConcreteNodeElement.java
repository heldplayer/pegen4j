package blue.heldplayer.pegen4j.unit;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

@NotNullByDefault
public record ConcreteNodeElement(String name, String type, Shape shape, boolean terminal, @Nullable String ref) {

  public enum Shape {
    SCALAR, LIST, OPT
  }

}
