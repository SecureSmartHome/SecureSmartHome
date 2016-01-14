package de.unipassau.isl.evs.ssh.core.database.dto;

import java.util.Comparator;

/**
 * @author Niko Fink
 */
public interface NamedDTO {
    String getName();

    Comparator<NamedDTO> COMPARATOR = new Comparator<NamedDTO>() {
        @Override
        public int compare(NamedDTO lhs, NamedDTO rhs) {
            if (lhs.getName() == null) {
                return rhs.getName() == null ? 0 : 1;
            }
            if (rhs.getName() == null) {
                return -1;
            }
            return lhs.getName().compareTo(rhs.getName());
        }
    };
}
