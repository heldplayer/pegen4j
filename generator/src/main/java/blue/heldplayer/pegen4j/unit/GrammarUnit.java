package blue.heldplayer.pegen4j.unit;

import blue.heldplayer.pegen4j.generator.GrammarException;
import blue.heldplayer.pegen4j.parser.DiagnosticMessage;
import blue.heldplayer.pegen4j.peg.ast.*;
import blue.heldplayer.pegen4j.util.SccUtils;
import blue.heldplayer.pegen4j.util.StringUtils;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

@NotNullByDefault
public final class GrammarUnit {

  private static final Pattern KEYWORD_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

  private final Grammar node;
  private final Set<String> ruleNames = new HashSet<>();

  public final Map<String, RuleUnit> ruleByName = new LinkedHashMap<>();
  public final Set<String> leftRecursiveRules = new HashSet<>();
  public final Set<String> leadingRules = new HashSet<>();
  public final Set<String> nullableRules = new HashSet<>();

  public final LinkedHashMap<String, String> tokenPatternByName = new LinkedHashMap<>();
  public final List<String> ignoredTokenNames = new ArrayList<>();
  public final Set<String> keywords = new LinkedHashSet<>();
  public final Set<String> softKeywords = new LinkedHashSet<>();

  public final List<String> recoverTokens = new ArrayList<>();

  public final Map<String, List<String>> optionValuesByName = new HashMap<>();

  private final List<DiagnosticMessage> problems = new ArrayList<>();

  private GrammarUnit(Grammar node) {
    this.node = node;
  }

  public static GrammarUnit from(Grammar grammar) {
    var unit = new GrammarUnit(grammar);
    unit.build();
    unit.computeNullables();
    unit.computeLeftRecursion();
    return unit;
  }

  public Grammar getNode() {
    return this.node;
  }

  public List<DiagnosticMessage> getProblems() {
    return Collections.unmodifiableList(this.problems);
  }

  public boolean hasErrors() {
    return !this.problems.isEmpty();
  }

  private void build() {
    for (var element : this.node.elements) {
      switch (element) {
        case TokenDefinition token -> this.tokenPatternByName.put(token.name.string(), token.regex());
        case RuleDefinition rule -> this.ruleNames.add(rule.name.string());
        default -> {
        }
      }
    }

    for (var element : this.node.elements) {
      switch (element) {
        case TokenDefinition _ -> {
        }
        case Meta.Ignore ignore -> this.ignoredTokenNames.add(ignore.name.string());
        case Meta.Keyword keyword -> this.keywords.add(keyword.value.unquoted());
        case Meta.Recover recover -> {
          for (var token : recover.tokens) {
            this.recoverTokens.add(token.unquoted());
          }
        }
        case Meta.Option option -> this.optionValuesByName
          .computeIfAbsent(option.name.string(), _ -> new ArrayList<>())
          .add(option.value.unquoted());
        case RuleDefinition rule -> {
          this.ruleByName.put(rule.name.string(), buildRule(rule));
          for (Alt alt : rule.alts) {
            for (Item item : alt.items) {
              collectKeywords(item);
            }
          }
        }
      }
    }

    resolveTypes();
  }

  private void resolveTypes() {
    for (var rule : this.ruleByName.values()) {
      rule.setTypeName(resolveType(rule, new HashSet<>()));
    }
  }

  private String resolveType(RuleUnit rule, Set<String> visiting) {
    var explicit = rule.getNode().typeName();
    if (explicit != null) {
      return explicit;
    }
    if (!visiting.add(rule.getName())) {
      return "Object";
    }
    try {
      String inferred = null;
      for (var alt : rule.getAlts()) {
        var fields = alt.getContextFields();
        var produced = alt.getAction() == null && fields.size() == 1 ? astTypeOf(fields.getFirst(), visiting) : "Object";
        if (inferred == null) {
          inferred = produced;
        } else if (!inferred.equals(produced)) {
          return "Object";
        }
      }
      return inferred == null ? "Object" : inferred;
    } finally {
      visiting.remove(rule.getName());
    }
  }

  private String astTypeOf(ConcreteNodeElement field, Set<String> visiting) {
    if (field.terminal()) {
      return field.type();
    }
    var ref = this.ruleByName.get(field.ref());
    var element = ref != null ? resolveType(ref, visiting) : "Object";
    return field.shape() == ConcreteNodeElement.Shape.LIST ? "List<" + element + ">" : element;
  }

  private RuleUnit buildRule(RuleDefinition rule) {
    var contextClassName = StringUtils.toPascalCase(rule.name.string()) + "Context";
    var alts = new ArrayList<AltUnit>();

    if (rule.alts.size() == 1 && rule.alts.getFirst().labelName() == null) {
      var alt = rule.alts.getFirst();
      alts.add(new AltUnit(alt, contextClassName, contextClassName, deriveItems(alt)));
    } else {
      var seenLabels = new HashSet<String>();
      var labeledCount = 0;
      for (var i = 0; i < rule.alts.size(); i++) {
        var alt = rule.alts.get(i);

        var altContextName = alt.labelName() != null ? alt.labelName() : "Alt" + (i + 1);
        alts.add(new AltUnit(alt, altContextName, contextClassName + "." + altContextName, deriveItems(alt)));

        var label = alt.labelName();
        if (label != null) {
          labeledCount++;
          if (!seenLabels.add(label)) {
            var sourceLocation = alt.contextName != null ? alt.contextName.toSourceLocation() : null;
            this.problems.add(new DiagnosticMessage(DiagnosticMessage.Severity.ERROR, sourceLocation, "rule '" + rule.name.string() + "': duplicate alternative label '" + label + "'"));
          }
        }
      }
      if (labeledCount > 0 && labeledCount != rule.alts.size()) {
        this.problems.add(new DiagnosticMessage(DiagnosticMessage.Severity.ERROR, rule.name.toSourceLocation(), "rule '" + rule.name.string() + "': label all alternatives or none"));
      }
    }

    return new RuleUnit(rule, contextClassName, alts);
  }

  private void collectKeywords(Item item) {
    switch (item) {
      case Item.StringLeaf leaf -> {
        var raw = leaf.string.string();
        var value = leaf.string.unquoted();
        if (KEYWORD_PATTERN.matcher(value).matches()) {
          if (raw.startsWith("'")) {
            this.keywords.add(value);
          } else if (raw.startsWith("\"")) {
            this.softKeywords.add(value);
          }
        }
      }
      case Item.Named named -> collectKeywords(named.item);
      case Item.Opt opt -> collectKeywords(opt.item);
      case Item.Repeat0 repeat -> collectKeywords(repeat.item);
      case Item.Repeat1 repeat -> collectKeywords(repeat.item);
      case Item.PositiveLookahead lookahead -> collectKeywords(lookahead.item);
      case Item.NegativeLookahead lookahead -> collectKeywords(lookahead.item);
      case Item.Forced forced -> collectKeywords(forced.item);
      case Item.Gather gather -> {
        collectKeywords(gather.item);
        collectKeywords(gather.separator);
      }
      case Item.NameLeaf _, Item.Cut _, Item.EofMarker _ -> {
      }
    }
  }

  public boolean recoveryEnabled() {
    return !this.recoverTokens.isEmpty();
  }

  private static @Nullable String defaultItemName(Item item) {
    return switch (item) {
      case Item.NameLeaf leaf -> leaf.name.string();
      case Item.Repeat0 r -> defaultItemName(r.item);
      case Item.Repeat1 r -> defaultItemName(r.item);
      case Item.Gather g -> defaultItemName(g.item);
      case Item.Opt o -> defaultItemName(o.item);
      case Item.Forced f -> defaultItemName(f.item);
      default -> null;
    };
  }

  private static String uniqueName(String name, Set<String> used) {
    if (used.add(name)) {
      return name;
    }
    int i = 1;
    while (!used.add(name + "_" + i)) {
      i++;
    }
    return name + "_" + i;
  }

  private List<ItemUnit> deriveItems(Alt alt) {
    var items = new ArrayList<ItemUnit>();
    var used = new HashSet<String>();
    for (Item item : alt.items) {
      var target = item.target();
      var label = item.boundName() != null ? item.boundName() : defaultItemName(target);
      ConcreteNodeElement field = switch (target) {
        case Item.NameLeaf _ -> scalarField(target, label, used);
        case Item.StringLeaf _ -> label != null ? scalarField(target, label, used) : null;
        case Item.Repeat0 r -> listField(r.item, label, used);
        case Item.Repeat1 r -> listField(r.item, label, used);
        case Item.Gather g -> listField(g.item, label, used);
        case Item.Opt o -> optField(o.item, label, used);
        case Item.Forced _ -> throw new GrammarException("forced (&&) is not yet supported");
        case Item.Cut _, Item.EofMarker _, Item.PositiveLookahead _, Item.NegativeLookahead _ -> null;
        default -> throw new GrammarException("unsupported item: " + target.getClass().getSimpleName());
      };
      items.add(new ItemUnit(item, field));
    }
    return items;
  }

  private ConcreteNodeElement scalarField(Item atomItem, @Nullable String label, Set<String> used) {
    return field(atomItem, label, used, ConcreteNodeElement.Shape.SCALAR);
  }

  private ConcreteNodeElement listField(Item element, @Nullable String label, Set<String> used) {
    return field(element, label, used, ConcreteNodeElement.Shape.LIST);
  }

  private ConcreteNodeElement optField(Item inner, @Nullable String label, Set<String> used) {
    return field(inner, label, used, ConcreteNodeElement.Shape.OPT);
  }

  private ConcreteNodeElement field(Item atomItem, @Nullable String label, Set<String> used, ConcreteNodeElement.Shape shape) {
    var atom = classifyAtom(atomItem);
    var name = uniqueName(label != null ? label : "e", used);
    var type = shape == ConcreteNodeElement.Shape.LIST ? "List<" + atom.type() + ">" : atom.type();
    return new ConcreteNodeElement(name, type, shape, atom.terminal(), atom.ref());
  }

  public AtomInfo classifyAtom(Item item) {
    return switch (item) {
      case Item.NameLeaf leaf -> {
        var name = leaf.name.string();
        if (this.tokenPatternByName.containsKey(name)) {
          yield new AtomInfo("PatternTokenNode", true, name);
        } else if (this.ruleNames.contains(name)) {
          yield new AtomInfo(StringUtils.toPascalCase(name) + "Context", false, name);
        }
        yield new AtomInfo("Object", false, name);
      }
      case Item.StringLeaf _ -> new AtomInfo("StringTokenNode", true, null);
      case Item.Named named -> classifyAtom(named.item);
      default -> throw new GrammarException("unexpected item: " + item);
    };
  }

  public record AtomInfo(String type, boolean terminal, @Nullable String ref) {
  }

  private void computeLeftRecursion() {
    var graph = makeFirstGraph();
    for (Set<String> scc : SccUtils.stronglyConnectedComponents(graph)) {
      if (scc.size() > 1) {
        for (String name : scc) {
          markLeftRecursive(name);
        }
        var leaders = new HashSet<>(scc);
        for (String start : scc) {
          for (List<String> cycle : findCyclesInScc(graph, scc, start)) {
            var notInCycle = new HashSet<>(scc);
            cycle.forEach(notInCycle::remove);
            leaders.removeAll(notInCycle);
          }
        }
        markLeader(Collections.min(leaders));
      } else {
        String name = scc.iterator().next();
        if (graph.getOrDefault(name, Set.of()).contains(name)) {
          markLeftRecursive(name);
          markLeader(name);
        }
      }
    }
  }

  private void markLeftRecursive(String name) {
    if (this.ruleByName.containsKey(name)) {
      this.leftRecursiveRules.add(name);
    }
  }

  private void markLeader(String name) {
    if (this.ruleByName.containsKey(name)) {
      this.leadingRules.add(name);
    }
  }

  private void computeNullables() {
    var visited = new HashSet<String>();
    for (var rule : this.ruleByName.values()) {
      ruleNullable(rule.getName(), visited);
    }
  }

  private boolean ruleNullable(String name, Set<String> visited) {
    var rule = this.ruleByName.get(name);
    if (rule == null || !visited.add(name)) {
      return this.nullableRules.contains(name);
    }
    if (rule.getNode().isNullable(referenced -> ruleNullable(referenced, visited))) {
      this.nullableRules.add(name);
    }
    return this.nullableRules.contains(name);
  }

  private Map<String, Set<String>> makeFirstGraph() {
    var graph = new LinkedHashMap<String, Set<String>>();
    var vertices = new HashSet<String>();
    for (RuleUnit rule : this.ruleByName.values()) {
      var names = new HashSet<String>();
      for (Alt alt : rule.getNode().alts) {
        names.addAll(initialNames(alt.items));
      }
      graph.put(rule.getName(), names);
      vertices.addAll(names);
    }
    for (String vertex : vertices) {
      graph.putIfAbsent(vertex, new HashSet<>());
    }
    return graph;
  }

  private Set<String> initialNames(List<? extends Item> items) {
    var names = new HashSet<String>();
    for (Item item : items) {
      names.addAll(initialNamesItem(item));
      var visited = new HashSet<String>();
      if (!item.isNullable(referenced -> ruleNullable(referenced, visited))) {
        break;
      }
    }
    return names;
  }

  private Set<String> initialNamesItem(Item item) {
    return switch (item) {
      case Item.NameLeaf leaf -> Set.of(leaf.name.string());
      case Item.Opt opt -> initialNamesItem(opt.item);
      case Item.Repeat0 repeat -> initialNamesItem(repeat.item);
      case Item.Repeat1 repeat -> initialNamesItem(repeat.item);
      case Item.Named named -> initialNamesItem(named.item);
      case Item.Gather gather -> {
        var names = new HashSet<>(initialNamesItem(gather.separator));
        names.addAll(initialNamesItem(gather.item));
        yield names;
      }
      case Item.Cut _,
           Item.EofMarker _,
           Item.Forced _,
           Item.NegativeLookahead _,
           Item.PositiveLookahead _,
           Item.StringLeaf _ -> Set.of();
    };
  }

  private List<List<String>> findCyclesInScc(Map<String, Set<String>> graph, Set<String> scc, String start) {
    var restricted = new HashMap<String, Set<String>>();
    for (var entry : graph.entrySet()) {
      if (scc.contains(entry.getKey())) {
        var dsts = new HashSet<String>();
        for (String d : entry.getValue()) {
          if (scc.contains(d)) {
            dsts.add(d);
          }
        }
        restricted.put(entry.getKey(), dsts);
      }
    }
    var cycles = new ArrayList<List<String>>();
    dfsCycles(restricted, start, new ArrayList<>(), cycles);
    return cycles;
  }

  private void dfsCycles(Map<String, Set<String>> graph, String node, List<String> path, List<List<String>> out) {
    if (path.contains(node)) {
      var cycle = new ArrayList<>(path);
      cycle.add(node);
      out.add(cycle);
      return;
    }
    var newPath = new ArrayList<>(path);
    newPath.add(node);
    for (String child : graph.getOrDefault(node, Set.of())) {
      dfsCycles(graph, child, newPath, out);
    }
  }

}
