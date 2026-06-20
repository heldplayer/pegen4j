package blue.heldplayer.pegen4j.parser;

import blue.heldplayer.pegen4j.parser.node.BaseNode;
import blue.heldplayer.pegen4j.parser.node.PatternTokenNode;
import blue.heldplayer.pegen4j.parser.node.StringTokenNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AbstractParser {

  protected final URI sourceUri;
  protected final CharSequence sourceString;
  private final int length;
  private final TokenType[] tokens;
  private final TokenType[] ignoredTokens;
  private final Set<String> keywordLiterals;
  private final Set<String> softKeywordLiterals;
  private final boolean caseInsensitive;

  private final List<DiagnosticMessage> parseErrors = new ArrayList<>();

  private int position = 0;
  private int line = 1;
  private int column = 1;

  public AbstractParser(URI sourceUri, @NotNull CharSequence sourceString, @NotNull TokenType @NotNull [] tokens, @NotNull TokenType @NotNull [] ignoredTokens, @NotNull String @NotNull [] keywords, @NotNull String @NotNull [] softKeywords, boolean caseInsensitive) {
    this.sourceUri = sourceUri;
    this.sourceString = sourceString;
    this.length = sourceString.length();
    this.tokens = tokens;
    this.ignoredTokens = ignoredTokens;
    if (caseInsensitive) {
      this.keywordLiterals = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
      this.keywordLiterals.addAll(List.of(keywords));
      this.softKeywordLiterals = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
      this.softKeywordLiterals.addAll(List.of(softKeywords));
    } else {
      this.keywordLiterals = Set.of(keywords);
      this.softKeywordLiterals = Set.of(softKeywords);
    }
    this.caseInsensitive = caseInsensitive;

    this.keywordEndGuardMatcher = KEYWORD_END_GUARD_PATTERN.matcher(this.sourceString);
  }


  public static final class State {
    private final int position;
    private final int line;
    private final int column;

    private State(int position, int line, int column) {
      this.position = position;
      this.line = line;
      this.column = column;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      if (obj == null || obj.getClass() != this.getClass()) return false;
      var that = (State) obj;
      return this.position == that.position &&
        this.line == that.line &&
        this.column == that.column;
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.position, this.line, this.column);
    }
  }

  protected final @NotNull State mark() {
    return new State(this.position, this.line, this.column);
  }

  protected final void reset(@NotNull State state) {
    this.position = state.position;
    this.line = state.line;
    this.column = state.column;
  }

  protected final void finish(@NotNull BaseNode node, @NotNull State start) {
    node.setLocation(this.sourceUri, start.position, this.position, start.line, start.column, this.line, this.column);
  }

  // region Error handling
  private int furthestPosition = -1;
  private int furthestLine = 1;
  private int furthestColumn = 1;
  private final Set<String> expectedTokens = new LinkedHashSet<>();

  private void recordFailure(String expectation) {
    if (this.position > this.furthestPosition) {
      this.furthestPosition = this.position;
      this.furthestLine = this.line;
      this.furthestColumn = this.column;
      this.expectedTokens.clear();
    }
    if (this.position == this.furthestPosition) {
      this.expectedTokens.add(expectation);
    }
  }

  public @Nullable DiagnosticMessage getFurthestFailure() {
    if (this.furthestPosition < 0) {
      return null;
    }
    var location = new SourceLocation(this.sourceUri, this.furthestPosition, this.furthestPosition, this.furthestLine, this.furthestColumn, this.furthestLine, this.furthestColumn);
    return new DiagnosticMessage(DiagnosticMessage.Severity.ERROR, location, "expected " + String.join(" or ", this.expectedTokens));
  }

  public List<DiagnosticMessage> getErrors() {
    return List.copyOf(this.parseErrors);
  }

  // region Error recovery
  protected boolean pendingRecovery = false;

  protected final void recover(@NotNull String @NotNull [] syncTokens) {
    DiagnosticMessage error = this.getFurthestFailure();
    if (error != null) {
      this.parseErrors.add(error);
    }
    this.furthestPosition = -1;
    this.furthestLine = 1;
    this.furthestColumn = 1;
    this.expectedTokens.clear();
    this.synchronize(syncTokens);
    this.pendingRecovery = true;
  }

  protected final void synchronize(@NotNull String @NotNull [] syncTokens) {
    while (this.position < this.length) {
      this.skipIgnoredTokens();
      if (this.position >= this.length) {
        return;
      }
      for (String sync : syncTokens) {
        int end = this.position + sync.length();
        if (end <= this.length && sync.contentEquals(this.sourceString.subSequence(this.position, end))) {
          this.textProcessed(sync.length());
          return;
        }
      }
      this.advanceOneToken();
    }
  }

  private void advanceOneToken() {
    for (TokenType token : this.tokens) {
      var matcher = this.getMatcher(token.pattern()).region(this.position, this.length);
      if (matcher.lookingAt() && matcher.end() > 0) {
        this.textProcessed(matcher);
        return;
      }
    }
    this.textProcessed(1);
  }
  // endregion
  // endregion

  // region Parsing
  private void skipIgnoredTokens() {
    boolean skipped;
    do {
      skipped = false;

      for (var ignored : this.ignoredTokens) {
        var matcher = this.getMatcher(ignored.pattern()).region(this.position, this.length);
        if (matcher.lookingAt()) {
          this.textProcessed(matcher);
          skipped = true;
          break;
        }
      }
    } while (skipped);
  }

  @Nullable
  protected final PatternTokenNode expect(@NotNull TokenType token) {
    this.skipIgnoredTokens();
    var matcher = this.getMatcher(token.pattern()).region(this.position, this.length);
    if (matcher.lookingAt()) {
      var tokenText = matcher.group();
      if (this.keywordLiterals.contains(tokenText)) {
        this.recordFailure(token.name());
        return null;
      }

      var startPos = this.position;
      var startLine = this.line;
      var startColumn = this.column;
      this.textProcessed(matcher);
      var endPos = this.position;
      var endLine = this.line;
      var endColumn = this.column;

      var result = new PatternTokenNode(matcher.toMatchResult());
      result.setLocation(this.sourceUri, startPos, endPos, startLine, startColumn, endLine, endColumn);
      return result;
    }
    this.recordFailure(token.name());
    return null;
  }

  private static final Pattern KEYWORD_END_GUARD_PATTERN = Pattern.compile("\\w\\w");
  private final Matcher keywordEndGuardMatcher;

  @Nullable
  protected final StringTokenNode expect(@NotNull String str) {
    this.skipIgnoredTokens();
    int strEndPos = this.position + str.length();
    if (strEndPos <= this.length) {
      if (this.matchesAt(this.position, str) && (
        // Check that our string match does not accidentally match a partial word
        // e.g. expecting "in" should not match "index"
        strEndPos + 1 > this.length
          || !this.keywordEndGuardMatcher.region(strEndPos - 1, strEndPos + 1).matches()
      )) {
        var startPos = this.position;
        var startLine = this.line;
        var startColumn = this.column;
        this.textProcessed(str.length());
        var endPos = this.position;
        var endLine = this.line;
        var endColumn = this.column;

        var result = new StringTokenNode(this.sourceString.subSequence(this.position, strEndPos).toString());
        result.setLocation(this.sourceUri, startPos, endPos, startLine, startColumn, endLine, endColumn);
        return result;
      }
    }
    this.recordFailure("\"" + str + "\"");
    return null;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  protected final boolean expectEof() {
    this.skipIgnoredTokens();
    if (this.position >= this.length) {
      return true;
    }
    this.recordFailure("<EOF>");
    return false;
  }
  // endregion

  // region Parsing helpers
  private void textProcessed(int length) {
    var position = this.position + length;
    var line = this.line;
    var column = this.column;

    for (int i = 0; i < length; i++) {
      if (this.sourceString.charAt(this.position + i) == '\n') {
        column = 1;
        ++line;
      } else {
        ++column;
      }
    }

    this.position = position;
    this.line = line;
    this.column = column;
  }

  private void textProcessed(Matcher matcher) {
    assert matcher.start() == matcher.regionStart();
    this.textProcessed(matcher.end() - matcher.start());
  }

  private final Map<Pattern, Matcher> matchers = new IdentityHashMap<>();

  private Matcher getMatcher(Pattern pattern) {
    return this.matchers.computeIfAbsent(pattern, p -> p.matcher(this.sourceString));
  }

  private boolean matchesAt(int startPos, @NotNull String str) {
    for (var i = 0; i < str.length(); i++) {
      var a = str.charAt(i);
      var b = this.sourceString.charAt(startPos + i);
      if (a == b) {
        continue;
      }
      if (!this.caseInsensitive) {
        return false;
      }
      if (
        (a = Character.toUpperCase(a)) == (b = Character.toUpperCase(b))
          || Character.toLowerCase(a) == Character.toLowerCase(b)
      ) {
        continue;
      }
      return false;
    }
    return true;
  }
  // endregion

  // region Memoization
  private final Map<CacheKey, CacheEntry> memoizationCache = new HashMap<>();

  private record CacheKey(int position, String ruleName) {
  }

  private record CacheEntry(@Nullable Object value, @NotNull State endState) {
  }

  protected final <T> @Nullable T memoized(@NotNull String ruleName, @NotNull Supplier<@Nullable T> rule) {
    var mark = this.mark();
    var key = new CacheKey(mark.position, ruleName);
    var hit = this.memoizationCache.get(key);
    if (hit != null) {
      this.reset(hit.endState());
      @SuppressWarnings("unchecked")
      T tree = (T) hit.value();
      return tree;
    }
    T tree = rule.get();
    this.memoizationCache.put(key, new CacheEntry(tree, this.mark()));
    return tree;
  }

  @SuppressWarnings("unused") // not used by our own parser, but should be kept
  protected final <T> @Nullable T memoizedLeftRecursive(@NotNull String ruleName, @NotNull Supplier<@Nullable T> rule) {
    var mark = this.mark();
    var key = new CacheKey(mark.position, ruleName);
    var hit = this.memoizationCache.get(key);
    if (hit != null) {
      this.reset(hit.endState());
      @SuppressWarnings("unchecked")
      T tree = (T) hit.value();
      return tree;
    }

    // Prime the cache with a failure, then grow the parse.
    this.memoizationCache.put(key, new CacheEntry(null, mark));
    Object lastResult = null;
    State lastMark = mark;
    while (true) {
      this.reset(mark);
      Object result = rule.get();
      State endMark = this.mark();
      if (result == null) {
        break;
      }
      if (endMark.position <= lastMark.position) {
        // No further progress; keep the previous (longer) parse.
        break;
      }
      lastResult = result;
      lastMark = endMark;
      this.memoizationCache.put(key, new CacheEntry(lastResult, lastMark));
    }

    this.reset(lastMark);
    State endMark = (lastResult != null) ? this.mark() : mark;
    if (lastResult == null) {
      this.reset(mark);
    }
    this.memoizationCache.put(key, new CacheEntry(lastResult, endMark));
    @SuppressWarnings("unchecked")
    T tree = (T) lastResult;
    return tree;
  }

  @SuppressWarnings("unused") // not used by our own parser, but should be kept
  protected final <T> @Nullable T logged(@NotNull String ruleName, @NotNull Supplier<@Nullable T> rule) {
    return rule.get();
  }
  // endregion

}
