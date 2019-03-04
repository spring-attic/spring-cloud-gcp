package org.springframework.cloud.gcp.data.datastore.core.mapping.event;

import com.google.cloud.datastore.Key;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * An event published immediately after a read operation has finished.
 *
 * @author Chengyuan Zhao
 */
public class AfterKeyReadEvent extends ReadEvent {

    private final Set<Key> targetKeys;

    /**
     * Constructor.
     *
     * @param results A list of results from the read operation where each item was mapped from a Cloud Datastore
     *                entity.
     * @param keys The Keys that were attempted to be read.
     */
    public AfterKeyReadEvent(List results, Set<Key> keys) {
        super(results);
        this.targetKeys = keys;
    }

    /**
     * Get the list of target read keys.
     * @return the list of Keys that were attempted to be read.
     */
    public Set<Key> getTargetKeys(){
        return this.targetKeys;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AfterKeyReadEvent that = (AfterKeyReadEvent) o;
        return Objects.equals(getResults(), that.getResults())
                && Objects.equals(getTargetKeys(), that.getTargetKeys());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getResults(), getTargetKeys());
    }
}
