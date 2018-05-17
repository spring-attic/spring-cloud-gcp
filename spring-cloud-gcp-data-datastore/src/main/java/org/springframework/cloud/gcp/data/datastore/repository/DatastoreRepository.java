package org.springframework.cloud.gcp.data.datastore.repository;

import java.util.function.Function;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * A {@link PagingAndSortingRepository} that provides Datastore-specific functionality.
 *
 * @param <T> The type of the domain object
 * @param <ID> The type of the ID property in the domain object
 *
 * @author Chengyuan Zhao
 */
public interface DatastoreRepository<T, ID> extends PagingAndSortingRepository<T, ID> {

  /**
   * Performs multiple read and write operations in a single transaction.
   * @param operations the function representing the operations to perform using a
   * DatastoreRepository based on a single transaction.
   * @param <A> the final return type of the operations.
   * @return the final result of the transaction.
   */
  <A> A performTransaction(Function<DatastoreRepository<T, ID>, A> operations);
}
