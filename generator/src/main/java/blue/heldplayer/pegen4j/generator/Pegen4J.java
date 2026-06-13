package blue.heldplayer.pegen4j.generator;

import blue.heldplayer.pegen4j.parser.DiagnosticMessage;
import blue.heldplayer.pegen4j.peg.Pegen4JParser;
import blue.heldplayer.pegen4j.peg.Pegen4JParserAstBuilder;
import blue.heldplayer.pegen4j.peg.ast.Grammar;
import blue.heldplayer.pegen4j.unit.GrammarUnit;
import org.apache.commons.cli.*;
import org.apache.commons.cli.help.HelpFormatter;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Pegen4J {

  private Pegen4J() {
  }

  private static Map<String, String> generateCst(URI sourceUri, String grammarSource) {
    var grammarUnit = GrammarUnit.from(parse(sourceUri, grammarSource));
    var problems = grammarUnit.getProblems();
    if (!problems.isEmpty()) {
      throw new GrammarException(problems.size() + " problem(s) in grammar:\n" + DiagnosticMessage.render(problems, grammarSource, false));
    }
    var cst = new JavaParserGenerator(grammarUnit);
    var files = new LinkedHashMap<>(cst.generateFiles());
    files.putAll(new JavaVisitorGenerator(cst).generateFiles());
    return files;
  }

  private static Grammar parse(URI sourceUri, String grammarSource) {
    var parser = new Pegen4JParser(sourceUri, grammarSource);
    var cst = parser.grammar_unit();
    if (cst == null) {
      var failure = parser.getFurthestFailure();
      throw new GrammarException(DiagnosticMessage.render(failure == null ? List.of() : List.of(failure), grammarSource, false));
    }
    var errors = parser.getErrors();
    if (!errors.isEmpty()) {
      throw new GrammarException(errors.size() + " parse error(s) in grammar:\n" + DiagnosticMessage.render(errors, grammarSource, false));
    }
    return (Grammar) new Pegen4JParserAstBuilder().visit(cst);
  }

  static void main(String[] args) throws IOException {
    var options = new Options()
      .addOption(Option.builder("o").longOpt("output")
        .hasArg().argName("file")
        .desc("output directory").get())
      .addOption(Option.builder("h").longOpt("help")
        .desc("show this help and exit").get());

    var help = HelpFormatter.builder().setShowSince(false).get();
    var usage = "Pegen4J [options] <input.peg>";

    CommandLine cmd;
    try {
      cmd = new DefaultParser().parse(options, args);
    } catch (ParseException e) {
      System.err.println(e.getMessage());
      help.printHelp(usage, null, options, null, true);
      System.exit(2);
      return;
    }

    if (cmd.hasOption("help")) {
      help.printHelp(usage, null, options, null, true);
      return;
    }

    var inputs = cmd.getArgList();
    if (inputs.size() != 1) {
      System.err.println("expected exactly one input grammar file, got " + inputs.size());
      help.printHelp(usage, null, options, null, false);
      System.exit(2);
      return;
    }
    if (!cmd.hasOption("output")) {
      System.err.println("missing required option: -o/--output");
      help.printHelp(usage, null, options, null, false);
      System.exit(2);
      return;
    }

    var inputPath = Path.of(inputs.getFirst());
    var outputPath = Path.of(cmd.getOptionValue("output"));
    var grammarSource = Files.readString(inputPath);

    var files = generateCst(inputPath.toUri(), grammarSource);
    for (var entry : files.entrySet()) {
      var file = outputPath.resolve(entry.getKey().replace('.', '/') + ".java");
      Files.createDirectories(file.getParent());
      Files.writeString(file, entry.getValue());
    }
  }

}
