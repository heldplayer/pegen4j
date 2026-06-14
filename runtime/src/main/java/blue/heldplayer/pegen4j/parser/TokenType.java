package blue.heldplayer.pegen4j.parser;

import org.intellij.lang.annotations.Language;

import java.util.regex.Pattern;

// Class can NOT be a record due to the constructor difference, thanks IntelliJ
@SuppressWarnings("ClassCanBeRecord")
public final class TokenType {

  private final String name;
  private final Pattern pattern;

  public TokenType(String name, @Language("RegExp") String regex, Boolean caseInsensitive) {
    this.name = name;
    this.pattern = Pattern.compile(regex, caseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
  }

  public String name() {
    return this.name;
  }

  public Pattern pattern() {
    return this.pattern;
  }

  @Override
  public String toString() {
    return this.name;
  }

}
