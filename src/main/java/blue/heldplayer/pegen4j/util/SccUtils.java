package blue.heldplayer.pegen4j.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.*;

@NotNullByDefault
public final class SccUtils {

  private SccUtils() {
  }

  @Contract(pure = true)
  public static List<Set<String>> stronglyConnectedComponents(Map<String, Set<String>> graph) {
    var result = new ArrayList<Set<String>>();
    var index = new HashMap<String, Integer>();
    var low = new HashMap<String, Integer>();
    var onStack = new HashSet<String>();
    var stack = new ArrayDeque<String>();
    var counter = new int[]{0};
    for (var vertex : graph.keySet()) {
      if (!index.containsKey(vertex)) {
        tarjan(vertex, graph, index, low, onStack, stack, counter, result);
      }
    }
    return result;
  }

  private static void tarjan(
      String v,
      Map<String, Set<String>> graph,
      Map<String, Integer> index,
      Map<String, Integer> low,
      Set<String> onStack,
      Deque<String> stack,
      int[] counter,
      List<Set<String>> result
  ) {
    index.put(v, counter[0]);
    low.put(v, counter[0]);
    counter[0]++;
    stack.push(v);
    onStack.add(v);
    for (var w : graph.getOrDefault(v, Set.of())) {
      if (!index.containsKey(w)) {
        tarjan(w, graph, index, low, onStack, stack, counter, result);
        low.put(v, Math.min(low.get(v), low.get(w)));
      } else if (onStack.contains(w)) {
        low.put(v, Math.min(low.get(v), index.get(w)));
      }
    }
    if (low.get(v).equals(index.get(v))) {
      var scc = new HashSet<String>();
      String w;
      do {
        w = stack.pop();
        onStack.remove(w);
        scc.add(w);
      } while (!w.equals(v));
      result.add(scc);
    }
  }

}
