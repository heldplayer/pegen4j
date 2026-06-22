package blue.heldplayer.pegen4j.intellij;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

public class MessageBundle {

  private static final String BUNDLE = "messages.MessageBundle";
  private static final DynamicBundle INSTANCE = new DynamicBundle(MessageBundle.class, BUNDLE);

  public static @NonNls String message(@NonNls @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
    return INSTANCE.getMessage(key, params);
  }

  public static Supplier<@NonNls String> lazyMessage(@NonNls @PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
    return INSTANCE.getLazyMessage(key, params);
  }

}
