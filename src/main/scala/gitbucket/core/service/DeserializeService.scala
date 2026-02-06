package gitbucket.core.service

import akka.actor.ActorSystem
import akka.serialization.SerializationExtension

object DeserializeService {

  private lazy val system: ActorSystem = ActorSystem("deserialize")

  /**
   * Deserializes the given bytes to an object using the configured serializer.
   */
  def deserializeBytes(bytes: Array[Byte]): scala.util.Try[AnyRef] = {
    val serialization = SerializationExtension(system)
    //CWE-502
    //SINK
    serialization.deserialize(bytes, classOf[Object])
  }
}
