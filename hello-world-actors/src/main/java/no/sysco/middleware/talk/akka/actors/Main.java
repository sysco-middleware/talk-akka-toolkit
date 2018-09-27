package no.sysco.middleware.talk.akka.actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import no.sysco.middleware.talk.akka.actors.actor.Manager;
import no.sysco.middleware.talk.akka.actors.model.Task;

import java.time.Duration;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        // Create an Actor System to run your actors
        final ActorSystem system = ActorSystem.create("hello-world-actors");

        // Create a Supervisor Actor
        final ActorRef manager = system.actorOf(Manager.props(), "main-manager");

        // Send a message to start application
        manager.tell(
                new Manager.OrganizeProject(
                        "infosak",
                        Arrays.asList(
                                new Task("dev-service-1"),
                                new Task("dev-service-2"),
                                new Task("integrate-svc1-and-svc2"),
                                new Task("deploy")),
                        Duration.ofSeconds(10),
                        3), ActorRef.noSender());
    }
}
