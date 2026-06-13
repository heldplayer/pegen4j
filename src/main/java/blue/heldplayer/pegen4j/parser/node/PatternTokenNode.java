package blue.heldplayer.pegen4j.parser.node;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.MatchResult;

public final class PatternTokenNode extends StringTokenNode {

  public final @NotNull MatchResult match;

  public PatternTokenNode(@NotNull MatchResult match) {
    super(match.group());
    this.match = match;
  }

  public @Nullable String group(int group) {
    return this.match.group(group);
  }

}
