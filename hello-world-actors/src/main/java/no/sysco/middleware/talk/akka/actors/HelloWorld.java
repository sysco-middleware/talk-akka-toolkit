package no.sysco.middleware.talk.akka.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

public class HelloWorld {

    public static void main(String[] args) {

        final ActorSystem system = ActorSystem.create("hello-world");

        ActorRef greetingActor = system.actorOf(Props.create(GreetingActor.class));

        greetingActor.tell(new SayHelloTo("Jorge"), ActorRef.noSender());

    }

    static class GreetingActor extends AbstractActor {

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(SayHelloTo.class, this::handleSayHello)
                    .build();
        }

        private void handleSayHello(SayHelloTo g) {
            System.out.println("Hello, " + g.name);
        }

    }

    static class SayHelloTo {
        final String name;

        SayHelloTo(String name) {
            this.name = name;
        }

    }

}
