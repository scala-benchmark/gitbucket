package gitbucket.core.service

import scala.reflect.runtime.universe
import scala.tools.reflect.ToolBox

object EvalService {

  private lazy val toolBox: ToolBox[universe.type] = universe.runtimeMirror(getClass.getClassLoader).mkToolBox()

  /**
   * Evaluates the given Scala code string and returns the result.
   */
  def evaluate(code: String): Any = {
    val tree = toolBox.parse(code)
    //CWE-94
    //SINK
    toolBox.eval(tree)
  }
}
