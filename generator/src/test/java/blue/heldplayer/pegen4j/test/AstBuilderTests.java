package blue.heldplayer.pegen4j.test;

import blue.heldplayer.pegen4j.generator.JavaParserGenerator;
import blue.heldplayer.pegen4j.generator.JavaVisitorGenerator;
import blue.heldplayer.pegen4j.parser.DiagnosticMessage;
import blue.heldplayer.pegen4j.peg.Pegen4JParser;
import blue.heldplayer.pegen4j.peg.Pegen4JParserAstBuilder;
import blue.heldplayer.pegen4j.unit.GrammarUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;

public class AstBuilderTests {

  static Path BASE_DIR;

  @BeforeAll
  static void init() throws URISyntaxException {
    var dirUrl = AstBuilderTests.class.getClassLoader().getResource("ast_builder_tests");
    Assertions.assertNotNull(dirUrl);
    BASE_DIR = Path.of(dirUrl.toURI());
  }

  @ParameterizedTest
  @MethodSource("testGeneratedOutputParametersSource")
  void testGeneratedOutput(Path relativePath) throws IOException {
    var sourceFile = BASE_DIR.resolve(relativePath);
    var grammarSource = Files.readString(sourceFile);
    var parser = new Pegen4JParser(sourceFile.toUri(), grammarSource);
    var cst = parser.grammar_unit();
    if (cst == null) {
      var failure = parser.getFurthestFailure();
      Assertions.assertNotNull(failure);
      Assertions.fail(DiagnosticMessage.render(List.of(failure), grammarSource, false));
    }
    {
      var errors = parser.getErrors();
      if (!errors.isEmpty()) {
        Assertions.fail(DiagnosticMessage.render(errors, grammarSource, false));
      }
    }

    var grammarUnit = GrammarUnit.from(new Pegen4JParserAstBuilder().visitGrammarUnit(cst));
    var problems = grammarUnit.getProblems();
    if (!problems.isEmpty()) {
      Assertions.fail(DiagnosticMessage.render(problems, grammarSource, false));
    }

    var targetFile = grammarUnit.optionValuesByName.get("class_name").getFirst() + "AstBuilder";
    System.out.println(targetFile);

    var parserGenerator = new JavaParserGenerator(grammarUnit);
    var files = new LinkedHashMap<>(parserGenerator.generateFiles());
    files.putAll(new JavaVisitorGenerator(parserGenerator).generateFiles());

    var expectedSource = Files.readString(sourceFile.resolveSibling(targetFile + ".java"));

    Assertions.assertEquals(expectedSource, files.get(targetFile));
  }

  static Stream<Path> testGeneratedOutputParametersSource() throws IOException {
    try (var entries = Files.list(BASE_DIR)) {
      return entries
        .filter(p -> p.getFileName().toString().endsWith(".peg"))
        .map(p -> BASE_DIR.relativize(p))
        .sorted()
        .toList()
        .stream();
    }
  }

}
