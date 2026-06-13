package blue.heldplayer.pegen4j.peg.ast;

import blue.heldplayer.pegen4j.parser.node.CustomNode;
import blue.heldplayer.pegen4j.parser.node.StringTokenNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

@NotNullByDefault
public final class Alt extends CustomNode {
  public final List<Item> items;
  public final @Nullable StringTokenNode actionBlock;
  public final @Nullable StringTokenNode contextName;

  public Alt(List<Item> items, @Nullable StringTokenNode actionBlock) {
    this(items, actionBlock, null);
  }

  public Alt(List<Item> items, @Nullable StringTokenNode actionBlock, @Nullable StringTokenNode contextName) {
    this.items = items;
    this.actionBlock = actionBlock;
    this.contextName = contextName;
  }

  @Contract(pure = true)
  public @Nullable String action() {
    if (this.actionBlock == null) {
      return null;
    }
    var blockString = this.actionBlock.string();
    assert blockString.length() >= 2 && blockString.startsWith("{") && blockString.endsWith("}");
    return blockString.substring(1, blockString.length() - 1).trim();
  }

  @Contract(pure = true)
  public @Nullable String labelName() {
    if (this.contextName == null) {
      return null;
    }
    return this.contextName.string();
  }

  @Contract(pure = true)
  public String describe() {
    return this.items.stream().map(Item::describe).collect(Collectors.joining(" "))
      + (action() instanceof String action ? " { " + action + " }" : "");
  }

  @Contract(pure = true)
  public boolean hasCut() {
    return this.items.stream().anyMatch(Item::hasCut);
  }

}
