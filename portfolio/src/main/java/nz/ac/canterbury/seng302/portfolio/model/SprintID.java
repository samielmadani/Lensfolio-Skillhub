package nz.ac.canterbury.seng302.portfolio.model;

import nz.ac.canterbury.seng302.portfolio.model.entities.Project;

import java.io.Serializable;

public class SprintID implements Serializable {
    private String label;
    private Project project;

    @Override
    public boolean equals (Object o) {
        if (o == this) return true;
        if (!(o instanceof SprintID)) return false;
        return this.label.equals(((SprintID) o).label) && project.getId() == ((SprintID) o).project.getId();
    }

    @Override
    public int hashCode () {
        return label.hashCode() * project.hashCode();
    }
}
