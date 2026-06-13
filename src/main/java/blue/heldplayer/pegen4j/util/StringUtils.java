package blue.heldplayer.pegen4j.util;

import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Collection;
import java.util.stream.Collectors;

@NotNullByDefault
public final class StringUtils {

  private StringUtils() {
  }

  @Contract(pure = true)
  public static String toJavaString(String value) {
    return "\"" + StringEscapeUtils.escapeJava(value) + "\"";
  }

  @Contract(pure = true)
  public static String toJavaStringList(Collection<String> values) {
    return values.stream().map(StringUtils::toJavaString).collect(Collectors.joining(", "));
  }

  @Contract(pure = true)
  public static String toPascalCase(String name) {
    var sb = new StringBuilder();
    for (var part : name.split("_")) {
      if (!part.isEmpty()) {
        sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
      }
    }
    assert !sb.isEmpty();
    return sb.toString();
  }

}
