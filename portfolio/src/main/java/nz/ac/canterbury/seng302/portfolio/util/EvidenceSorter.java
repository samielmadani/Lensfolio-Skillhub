package nz.ac.canterbury.seng302.portfolio.util;

import nz.ac.canterbury.seng302.portfolio.model.entities.Evidence;

import java.util.Comparator;

public class EvidenceSorter implements Comparator<Evidence> {
    public int compare(Evidence a, Evidence b) {
        if (a.getDate().after(b.getDate())) {
            return 1;
        } else if (a.getDate().before(b.getDate())) {
            return -1;
        }
        return 0;
    }
}
