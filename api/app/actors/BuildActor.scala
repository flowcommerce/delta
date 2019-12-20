package io.flow.delta.actors

object BuildActor {

  trait Message

  object Messages {
    case object Setup extends Message
    case object CheckLastState extends Message
  }


}
