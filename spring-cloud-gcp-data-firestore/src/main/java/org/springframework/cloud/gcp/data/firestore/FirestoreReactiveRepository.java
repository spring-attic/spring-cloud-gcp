package org.springframework.cloud.gcp.data.firestore;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/**
 * The Firestore repository type.
 * @param <T> the domain type.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.2
 */
public interface FirestoreReactiveRepository<T> extends ReactiveCrudRepository<T, String> {
}
