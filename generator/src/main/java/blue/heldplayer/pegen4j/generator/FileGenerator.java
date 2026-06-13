package blue.heldplayer.pegen4j.generator;

import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public final class FileGenerator {

  private final StringBuilder out = new StringBuilder();
  private int indent;

  public IndenterHelper withIndentation() {
    return new IndenterHelper();
  }

  public void line() {
    this.out.append("\n");
  }

  public void line(String text) {
    this.out.repeat("  ", this.indent).append(text).append("\n");
  }

  public String getOutput() {
    return this.out.toString();
  }

  public final class IndenterHelper implements AutoCloseable {
    {
      ++FileGenerator.this.indent;
    }

    @Override
    public void close() {
      --FileGenerator.this.indent;
    }
  }

}
