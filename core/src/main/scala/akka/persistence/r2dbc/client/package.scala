package akka.persistence.r2dbc

import akka.NotUsed
import akka.stream.scaladsl.Source
import org.reactivestreams.Publisher

package object client {

  private[client] def fromVoidPublisher[T](publisher: Publisher[Void]): Source[T, NotUsed] =
    Source.fromPublisher(publisher.asInstanceOf[Publisher[T]])

}
