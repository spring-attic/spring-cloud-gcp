package org.springframework.cloud.gcp.data.firestore.repository.support;

import org.springframework.cloud.gcp.data.firestore.FirestoreTemplate;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestoreMappingContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * The bean to create Firestore repository factories.
 * @param <S> the entity type of the repository
 * @param <ID> the id type of the entity
 * @param <T> the repository type
 *
 * @author Chengyuan Zhao
 *
 * @since 1.2
 */
public class FirestoreRepositoryFactoryBean<T extends Repository<S, ID>, S, ID> extends
        RepositoryFactoryBeanSupport<T, S, ID> {

    private final FirestoreTemplate firestoreTemplate;

    private final FirestoreMappingContext firestoreMappingContext;

    /**
     * Constructor.
     * @param repositoryInterface the repository interface class.
     * @param firestoreTemplate the template to use in repositories.
     * @param firestoreMappingContext the mapping context to use in repositories.
     */
    public FirestoreRepositoryFactoryBean(Class<T> repositoryInterface, FirestoreTemplate firestoreTemplate, FirestoreMappingContext firestoreMappingContext) {
        super(repositoryInterface);
        this.firestoreTemplate = firestoreTemplate;
        this.firestoreMappingContext = firestoreMappingContext;
    }

    @Override
    protected RepositoryFactorySupport createRepositoryFactory() {
        return new ReactiveFirestoreRepositoryFactory(this.firestoreTemplate, this.firestoreMappingContext);
    }
}
