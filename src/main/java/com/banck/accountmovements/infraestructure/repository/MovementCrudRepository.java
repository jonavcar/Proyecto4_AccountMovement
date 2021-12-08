package com.banck.accountmovements.infraestructure.repository;

import com.banck.accountmovements.domain.Movement;
import com.banck.accountmovements.infraestructure.model.dao.MovementDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.banck.accountmovements.aplication.model.MovementRepository;
import java.util.List;

/**
 *
 * @author jonavcar
 */
@Component
public class MovementCrudRepository implements MovementRepository {

    @Autowired
    IMovementCrudRepository movementRepository;

    @Override
    public Mono<Movement> get(String movement) {
        return movementRepository.findById(movement).map(this::MovementDaoToMovement);
    }

    @Override
    public Flux<Movement> list() {
        return movementRepository.findAll().map(this::MovementDaoToMovement);
    }

    @Override
    public Mono<Movement> create(Movement movement) {
        //Flux<MovementDao> fmd = movementRepository.findAllByCustomer(movement.getCustomer());
        return movementRepository.save(MovementToMovementDao(movement)).map(this::MovementDaoToMovement);
    }

    @Override
    public Mono<Movement> update(String movement, Movement c) {
        c.setMovement(movement);
        return movementRepository.save(MovementToMovementDao(c)).map(this::MovementDaoToMovement);
    }

    @Override
    public void delete(String movement) {
        movementRepository.deleteById(movement).subscribe();
    }

    public Movement MovementDaoToMovement(MovementDao md) {
        Movement m = new Movement();
        m.setMovement(md.getMovement());
        m.setMovementType(md.getMovementType());
        m.setProduct(md.getProduct());
        m.setProductType(md.getProductType());
        m.setThirdProduct(md.getThirdProduct());
        m.setThirdClient(md.getThirdClient());
        m.setModality(md.getModality());
        m.setCustomer(md.getCustomer());
        m.setAmount(md.getAmount());
        m.setConcept(md.getConcept());
        m.setObservation(md.getObservation());
        m.setDate(md.getDate());
        m.setHour(md.getHour());
        m.setState(md.isState());
        return m;
    }

    public MovementDao MovementToMovementDao(Movement m) {
        MovementDao md = new MovementDao();
        md.setMovement(m.getMovement());
        md.setMovementType(m.getMovementType());
        md.setProduct(m.getProduct());
        md.setProductType(m.getProductType());
        md.setThirdProduct(m.getThirdProduct());
        md.setThirdClient(m.getThirdClient());
        md.setModality(m.getModality());
        md.setCustomer(m.getCustomer());
        md.setAmount(m.getAmount());
        md.setConcept(m.getConcept());
        md.setObservation(m.getObservation());
        md.setDate(m.getDate());
        md.setHour(m.getHour());
        md.setState(m.isState());
        return md;
    }

    @Override
    public Flux<Movement> listByCustomer(String customer) {
        return movementRepository.findAllByCustomer(customer).map(this::MovementDaoToMovement);
    }

    @Override
    public Flux<Movement> listByCustomerAndAccount(String customer, String account) {
        return movementRepository.findAllByCustomerAndProduct(customer, account).map(this::MovementDaoToMovement);
    }

    @Override
    public Flux<Movement> listByCustomerAndAccountAndAccountType(String customer, String account, String accountType) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Flux<Movement> listByAccount(String account) {
        return movementRepository.findAllByProduct(account).map(this::MovementDaoToMovement);
    }

    @Override
    public Flux<Movement> listByAccount(List<String> account) {
        return movementRepository.findAllByProduct(account).map(this::MovementDaoToMovement);
    }
 
}
