package no.sysco.middleware.talk.akka.actors.actor;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Scheduler;
import no.sysco.middleware.talk.akka.actors.model.Project;
import no.sysco.middleware.talk.akka.actors.model.Task;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Manager extends AbstractLoggingActor {

    public static Props props() {
        return Props.create(Manager.class, Manager::new);
    }

    private final Scheduler scheduler = getContext().getSystem().scheduler();

    ////
    // Mutable State
    ////

    private List<Task> pendingTasks = new ArrayList<>();
    private Integer maxNumTeamMembers;
    private Stack<ActorRef> teamMembers = new Stack<>();

    ////
    // Behaviors
    ////

    private final Receive idle =
            receiveBuilder()
                    .match(OrganizeProject.class, this::handleOrganizeProject)
                    .build();


    private final Receive startingProject =
            receiveBuilder()
                    .match(StartProject.class, command -> handleStartProject())
                    .build();

    private final Receive runningProject =
            receiveBuilder()
                    .match(ManageTasks.class, command -> handleManageTasks())
                    .match(CompletedTask.class, this::handleCompletedTask)
                    .build();


    @Override
    public Receive createReceive() {
        return idle;
    }

    ////
    // Handle messages
    ////

    private void handleCompletedTask(CompletedTask completedTask) {
        log().info("{} was completed :D", completedTask.task);
        pendingTasks.remove(completedTask.task);
        teamMembers.push(getSender());
        getSelf().tell(new ManageTasks(), ActorRef.noSender());
    }

    private void handleManageTasks() {
        if (pendingTasks.isEmpty()) {
            log().info("Project finished!!!");
            getContext().getSystem().terminate();
        } else {
            for (Task task : pendingTasks) {
                if (!teamMembers.empty()) {
                    ActorRef teamMember = teamMembers.pop();
                    teamMember.tell(new TeamMember.AssignTask(task), getSelf());
                    break;
                }
            }
        }
    }

    private void handleOrganizeProject(OrganizeProject organizeProject) {
        Project state = new Project(organizeProject.name);
        log().info("Project created {}", state);
        pendingTasks.addAll(organizeProject.tasks);
        maxNumTeamMembers = organizeProject.maxNumTeamMembers;

        getContext().become(startingProject);

        log().info("Wait for the project to start");
        scheduler.scheduleOnce(
                organizeProject.startIn,
                () -> getSelf().tell(new StartProject(), ActorRef.noSender()),
                getContext().dispatcher());
    }

    private void handleStartProject() {
        log().info("Starting project!");

        for (int i = 0; i < maxNumTeamMembers; i++) {
            ActorRef teamMember = getContext().actorOf(TeamMember.props(), "team-member-" + i);
            teamMembers.add(teamMember);
        }

        log().info("Team members allocated");

        getContext().become(runningProject);

        getSelf().tell(new ManageTasks(), ActorRef.noSender());
    }

    ////
    // Commands
    ////

    public static class OrganizeProject {
        final String name;
        final List<Task> tasks;
        final Duration startIn;
        final Integer maxNumTeamMembers;

        public OrganizeProject(String name, List<Task> tasks, Duration startIn, Integer maxNumTeamMembers) {
            this.name = name;
            this.tasks = tasks;
            this.startIn = startIn;
            this.maxNumTeamMembers = maxNumTeamMembers;
        }
    }

    static class CompletedTask {
        final Task task;

        CompletedTask(Task task) {
            this.task = task;
        }
    }

    private static class StartProject {
    }

    private static class ManageTasks {
    }
}
