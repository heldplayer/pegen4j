package blue.heldplayer.pegen4j.peg.ast;

import blue.heldplayer.pegen4j.parser.node.CustomNode;
import blue.heldplayer.pegen4j.parser.node.StringTokenNode;

import java.util.function.Predicate;

public abstract sealed class Item extends CustomNode {

  public String boundName() {
    return null;
  }

  public Item target() {
    return this;
  }

  public abstract String describe();

  public abstract boolean isNullable(Predicate<String> ruleIsNullable);

  public abstract boolean hasCut();

  public static final class Cut extends Item {
    @Override
    public String describe() {
      return "~";
    }

    @Override
    public boolean isNullable(Predicate<String> ruleIsNullable) {
      return false;
    }

    @Override
    public boolean hasCut() {
      return true;
    }
  }

  public static final class EofMarker extends Item {
    @Override
    public String describe() {
      return "$";
    }

    @Override
    public boolean isNullable(Predicate<String> ruleIsNullable) {
      return false;
    }

    @Override
    public boolean hasCut() {
      return false;
    }
  }

  public static final class Forced extends Item {
    public final Atom item;

    public Forced(Atom item) {
      this.item = item;
    }

    @Override
    public String describe() {
      return "&&" + this.item.describe();
    }

    @Override
    public boolean isNullable(Predicate<String> ruleIsNullable) {
      return true;
    }

    @Override
    public boolean hasCut() {
      return false;
    }
  }

  public static final class Gather extends Item {
    public final Atom item;
    public final Atom separator;

    public Gather(Atom item, Atom separator) {
      this.item = item;
      this.separator = separator;
    }

    @Override
    public String describe() {
      return this.separator.describe() + "." + this.item.describe() + "+";
    }

    @Override
    public boolean isNullable(Predicate<String> ruleIsNullable) {
      return false;
    }

    @Override
    public boolean hasCut() {
      return false;
    }
  }

  public static final class Named extends Item {
    public final StringTokenNode name;
    public final Item item;

    public Named(StringTokenNode name, Item item) {
      this.name = name;
      this.item = item;
    }

    @Override
    public String boundName() {
      return this.name.string();
    }

    @Override
    public Item target() {
      return this.item;
    }

    @Override
    public String describe() {
      return this.name.string() + "=" + this.item.describe();
    }

    @Override
    public boolean isNullable(Predicate<String> ruleIsNullable) {
      return this.item.isNullable(ruleIsNullable);
    }

    @Override
    public boolean hasCut() {
      return this.item.hasCut();
    }
  }

  public static final class NegativeLookahead extends Item {
    public final Atom item;

    public NegativeLookahead(Atom item) {
      this.item = item;
    }

    @Override
    public String describe() {
      return "!" + this.item.describe();
    }

    @Override
    public boolean isNullable(Predicate<String> ruleIsNullable) {
      return true;
    }

    @Override
    public boolean hasCut() {
      return false;
    }
  }

  public static final class Opt extends Item {
    public final Atom item;

    public Opt(Atom item) {
      this.item = item;
    }

    @Override
    public String describe() {
      return this.item.describe() + "?";
    }

    @Override
    public boolean isNullable(Predicate<String> ruleIsNullable) {
      return true;
    }

    @Override
    public boolean hasCut() {
      return false;
    }
  }

  public static final class PositiveLookahead extends Item {
    public final Atom item;

    public PositiveLookahead(Atom item) {
      this.item = item;
    }

    @Override
    public String describe() {
      return "&" + this.item.describe();
    }

    @Override
    public boolean isNullable(Predicate<String> ruleIsNullable) {
      return true;
    }

    @Override
    public boolean hasCut() {
      return false;
    }
  }

  public static final class Repeat0 extends Item {
    public final Atom item;

    public Repeat0(Atom item) {
      this.item = item;
    }

    @Override
    public String describe() {
      return this.item.describe() + "*";
    }

    @Override
    public boolean isNullable(Predicate<String> ruleIsNullable) {
      return true;
    }

    @Override
    public boolean hasCut() {
      return false;
    }
  }

  public static final class Repeat1 extends Item {
    public final Atom item;

    public Repeat1(Atom item) {
      this.item = item;
    }

    @Override
    public String describe() {
      return this.item.describe() + "+";
    }

    @Override
    public boolean isNullable(Predicate<String> ruleIsNullable) {
      return false;
    }

    @Override
    public boolean hasCut() {
      return false;
    }
  }

  public static abstract sealed class Atom extends Item permits NameLeaf, StringLeaf {
  }

  public static final class NameLeaf extends Atom {
    public final StringTokenNode name;

    public NameLeaf(StringTokenNode name) {
      this.name = name;
    }

    @Override
    public String describe() {
      return this.name.string();
    }

    @Override
    public boolean isNullable(Predicate<String> ruleIsNullable) {
      return ruleIsNullable.test(this.name.string());
    }

    @Override
    public boolean hasCut() {
      return false;
    }
  }

  public static final class StringLeaf extends Atom {
    public final StringTokenNode string;

    public StringLeaf(StringTokenNode string) {
      this.string = string;
    }

    @Override
    public String describe() {
      return this.string.string();
    }

    @Override
    public boolean isNullable(Predicate<String> ruleIsNullable) {
      return this.string.unquoted().isEmpty();
    }

    @Override
    public boolean hasCut() {
      return false;
    }
  }

}
