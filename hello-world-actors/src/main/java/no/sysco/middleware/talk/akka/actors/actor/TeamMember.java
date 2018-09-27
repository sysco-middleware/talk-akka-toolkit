package no.sysco.middleware.talk.akka.actors.actor;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import no.sysco.middleware.talk.akka.actors.model.Task;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class TeamMember extends AbstractLoggingActor {
    private final Random random = new Random();
    Task currentTask;

    public static Props props() {
        return Props.create(TeamMember.class, TeamMember::new);
    }

    @Override
    public void preStart() throws Exception {
        log().info("Team Member ready!!!");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(AssignTask.class, this::handleAssignClass)
                .build();
    }

    private void handleAssignClass(AssignTask assignTask) {
        ActorRef sender = getSender();
        CompletableFuture.runAsync(() -> {
            if (currentTask == null) {
                currentTask = assignTask.task;
                log().info("Starting to work on {}", currentTask);
                try {
                    Thread.sleep(random.nextInt(10000) + 5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log().info("{} completed!! :), ready for the next one", currentTask);
                sender.tell(new Manager.CompletedTask(currentTask), getSelf());
                currentTask = null;
            } else {
                log().warning("Too busy! :(");
            }
        });

    }

    public static class AssignTask {
        final Task task;

        public AssignTask(Task task) {
            this.task = task;
        }
    }
}
