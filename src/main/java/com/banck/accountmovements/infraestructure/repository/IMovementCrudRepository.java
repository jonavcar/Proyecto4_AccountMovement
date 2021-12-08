package com.banck.accountmovements.infraestructure.repository;

import com.banck.accountmovements.infraestructure.model.dao.MovementDao;
import java.util.List;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 *
 * @author jonavcar
 */
public interface IMovementCrudRepository extends ReactiveCrudRepository<MovementDao, String> {

    Flux<MovementDao> findAllByProduct(String product);

    Flux<MovementDao> findAllByProduct(List<String> product);

    Flux<MovementDao> findAllByCustomer(String customer);

    Flux<MovementDao> findAllByCustomerAndProduct(String customer, String product);
}
