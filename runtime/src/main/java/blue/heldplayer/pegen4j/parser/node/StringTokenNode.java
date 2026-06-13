package blue.heldplayer.pegen4j.parser.node;

import org.jetbrains.annotations.NotNull;

public sealed class StringTokenNode extends ParseNode permits PatternTokenNode {

  private final @NotNull String string;

  public StringTokenNode(@NotNull String string) {
    this.string = string;
  }

  public @NotNull String string() {
    return this.string;
  }

  public @NotNull String unquoted() {
    assert this.string.length() >= 2;

    char first = this.string.charAt(0);
    char last = this.string.charAt(this.string.length() - 1);
    assert first == last;
    return this.string.substring(1, this.string.length() - 1);
  }

  @Override
  public String toString() {
    return this.string;
  }

}
