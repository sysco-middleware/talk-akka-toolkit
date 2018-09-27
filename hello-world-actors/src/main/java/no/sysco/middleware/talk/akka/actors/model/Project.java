package no.sysco.middleware.talk.akka.actors.model;

public class Project {
    final String name;

    public Project(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Project{" +
                "name='" + name + '\'' +
                '}';
    }
}
