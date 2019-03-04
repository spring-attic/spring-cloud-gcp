package org.springframework.cloud.gcp.data.datastore.core.mapping.event;

import com.google.cloud.datastore.Query;

import java.util.List;
import java.util.Objects;

public class AfterQueryReadEvent extends ReadEvent{

    private final Query query;

    /**
     * Constructor.
     *
     * @param results A list of results from the read operation where each item was mapped from a Cloud Datastore
     *                entity.
     * @param query The query run on Cloud Datastore.
     */
    public AfterQueryReadEvent(List results, Query query) {
        super(results);
        this.query = query;
    }

    /**
     * Get the query that was run.
     * @return the query that was run.
     */
    public Query getQuery(){
        return this.query;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AfterQueryReadEvent that = (AfterQueryReadEvent) o;
        return Objects.equals(getResults(), that.getResults())
                && Objects.equals(getQuery(), that.getQuery());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getResults(), getQuery());
    }
}
