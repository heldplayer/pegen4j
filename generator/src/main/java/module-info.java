module pegen4j.generator {
  requires static org.jetbrains.annotations;
  requires org.apache.commons.cli;
  requires org.apache.commons.text;

  requires transitive pegen4j.runtime;

  exports blue.heldplayer.pegen4j.generator;
  exports blue.heldplayer.pegen4j.peg;
  exports blue.heldplayer.pegen4j.peg.ast;
  exports blue.heldplayer.pegen4j.peg.cst;
  exports blue.heldplayer.pegen4j.unit;
  exports blue.heldplayer.pegen4j.util;
}