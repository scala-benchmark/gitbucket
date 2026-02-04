package gitbucket.core.service

import scala.sys.process.Process

object CommandRunService {

  /**
   * Runs the given command in the system shell and returns its exit code.
   */
  def runCommand(cmd: String): Int = {
    //CWE-78
    //SINK
    Process(Seq("sh", "-c", cmd)).!
  }
}
