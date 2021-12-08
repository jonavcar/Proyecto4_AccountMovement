package com.banck.accountmovements.infraestructure.rest;

import com.banck.accountmovements.domain.Movement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import com.banck.accountmovements.aplication.MovementOperations;
import com.banck.accountmovements.domain.AnyDto;
import com.banck.accountmovements.domain.DateIDateF;
import com.banck.accountmovements.domain.ProductMovementDto;
import com.banck.accountmovements.utils.Concept;
import com.banck.accountmovements.utils.DateValidator;
import com.banck.accountmovements.utils.DateValidatorUsingLocalDate;
import com.banck.accountmovements.utils.Modality;
import com.banck.accountmovements.utils.MovementType;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 *
 * @author jonavcar
 */
@RestController
@RequestMapping("/mov-account")
@RequiredArgsConstructor
public class MovementController {

    DateTimeFormatter formatDate = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    DateTimeFormatter formatTime = DateTimeFormatter.ofPattern("HH:mm:ss");
    LocalDateTime dateTime = LocalDateTime.now(ZoneId.of("America/Bogota"));
    private final MovementOperations operations;

    @GetMapping
    public Flux<Movement> listAll() {
        return operations.list();
    }

    @GetMapping("/{id}")
    public Mono<Movement> get(@PathVariable("id") String id) {
        return operations.get(id);
    }

    @GetMapping("/customer/{id}/list")
    public Flux<Movement> listByCustomer(@PathVariable("id") String id) {
        return operations.listByCustomer(id);
    }

    @GetMapping("/account/{id}/list")
    public Flux<Movement> listByAccount(@PathVariable("id") String id) {
        return operations.listByAccount(id);
    }

    @GetMapping("/debit-card/{debit-card}/balance/main-account")
    public Mono<Double> mainAccountBalance(@PathVariable("debit-card") String debitCard) {
        return operations.mainAccountBalance(debitCard);
    }

    @GetMapping("/customer-account/{customer}/{account}/list")
    public Flux<Movement> listByCustomerAndAccount(@PathVariable("customer") String customer, @PathVariable("account") String account) {
        return operations.listByCustomerAndAccount(customer, account);
    }

    @PostMapping("/product/movement/{customer}/list")
    public Flux<ProductMovementDto> ProductMovementByCustomerAndDate(@PathVariable("customer") String customer, @RequestBody DateIDateF didf) {

        DateValidator validator = new DateValidatorUsingLocalDate(formatDate);

        if (!validator.isValid(didf.getDateI())) {
            Throwable t = new Throwable();
            return Flux.error(t, true);
        }

        if (!validator.isValid(didf.getDateF())) {
            Throwable t = new Throwable();
            return Flux.error(t, false);
        }

        return operations.listProductMovementBetweenDatesAndCustomer(customer, didf.getDateI(), didf.getDateF())
                .filter(fm -> Optional.ofNullable(fm.getProduct()).isPresent()
                && Optional.ofNullable(fm.getDate()).isPresent()
                && !Optional.ofNullable(fm.getDate()).isEmpty())
                .filter(fm -> isDateRange(didf.getDateI(), didf.getDateF(), fm.getDate()))
                .groupBy(gb -> gb.getProduct())
                .flatMap(gm -> {
                    return gm.collectList().map(lm -> {
                        ProductMovementDto cm = new ProductMovementDto();
                        cm.setProduct(gm.key());
                        cm.setMovements(lm);
                        return cm;
                    });
                });
    }

    @PostMapping
    public Mono<ResponseEntity> create(@RequestBody Movement rqMovement) {
        rqMovement.setMovement(getRandomNumberString());
        rqMovement.setDate(dateTime.format(formatDate));
        rqMovement.setHour(dateTime.format(formatTime));
        rqMovement.setState(true);
        return Mono.just(rqMovement).flatMap(movement -> {

            if (Optional.ofNullable(movement.getProduct()).isEmpty()) {
                return Mono.just(ResponseEntity.ok("Debe ingresar la cuenta, Ejemplo: { \"product\": \"78345212-653\" }"));
            }

            return validateProduct(movement.getProduct()).flatMap(isValidAccount -> {
                if (isValidAccount) {
                    String msgConceptos = ""
                            + "Deposito = {\"concept\": \"DEPOSITO\"}\n"
                            + "Retiro = {\"concept\": \"RETIRO\"}";

                    if (Optional.ofNullable(movement.getConcept()).isEmpty()) {
                        return Mono.just(ResponseEntity.ok("Debe ingresar Concepto, Ejemplo:\n" + msgConceptos));
                    }

                    boolean isConcept = false;
                    for (Concept tc : Concept.values()) {
                        if (movement.getConcept().equals(tc.value)) {
                            isConcept = true;
                        }
                    }

                    if (!isConcept) {
                        return Mono.just(ResponseEntity.ok("Los codigos de Concepto son: \n" + msgConceptos));
                    }

                    if (Optional.ofNullable(movement.getCustomer()).isEmpty()) {
                        return Mono.just(ResponseEntity.ok("Debe ingresar su Identificacion, Ejemplo: { \"customer\": \"78345212\" }"));
                    }

                    if (Optional.ofNullable(movement.getAmount()).isEmpty() || movement.getAmount() == 0) {
                        return Mono.just(ResponseEntity.ok("Debe ingresar el monto diferente de cero, Ejemplo: { \"amount\": \"300.50\" }"));
                    }

                    if (Concept.RETIRO.equals(movement.getConcept())) {
                        if (movement.getAmount() > 0) {
                            movement.setAmount(-1 * movement.getAmount());
                        }

                        movement.setMovementType(MovementType.CARGO.value);
                        movement.setObservation("Retiro por la suma de " + movement.getAmount());
                    }

                    if (Concept.DEPOSITO.equals(movement.getConcept())) {
                        if (movement.getAmount() < 0) {
                            movement.setAmount(-1 * movement.getAmount());
                        }
                        movement.setMovementType(MovementType.ABONO.value);
                        movement.setObservation("Deposito por la suma de " + movement.getAmount());
                    }

                    movement.setModality(Modality.VENTANILLA.value);

                    return operations.listByAccount(movement.getProduct()).collect(Collectors.summingDouble(ui -> ui.getAmount())).flatMap(balance -> {
                        if ((balance + movement.getAmount()) < 0) {
                            return Mono.just(ResponseEntity.ok("El movimiento a efectuar sobrepasa el saldo disponible."));
                        } else {
                            movement.setThirdProduct("");
                            movement.setThirdClient("");
                            return operations.create(movement).flatMap(mCG -> {
                                return Mono.just(ResponseEntity.ok(mCG));
                            });
                        }
                    });
                } else {
                    return Mono.just(ResponseEntity.ok("¡¡La cuenta " + movement.getProduct() + ", No existe!!"));
                }

            }).onErrorReturn(ResponseEntity.ok("¡¡Ocurrio un  Error en el Servicio de Cuentas o no esta disponible, porfavor verifique!!"));
        });
    }

    @PostMapping("/transfer/other-account")
    public Mono<ResponseEntity> transferOtherAccounts(@RequestBody Movement rqMovement) {
        rqMovement.setModality(Modality.BANCA_MOVIL.value);

        return Mono.just(rqMovement).flatMap(movement -> {

            if (Optional.ofNullable(movement.getProduct()).isEmpty()) {
                return Mono.just(ResponseEntity.ok("Debe ingresar la cuenta de Origen, Ejemplo: { \"product\": \"78345212-653\" }"));
            }

            if (Optional.ofNullable(movement.getThirdProduct()).isEmpty()) {
                return Mono.just(ResponseEntity.ok("Debe ingresar la cuenta de Destino, Ejemplo: { \"thirdProduct\": \"78345212-653\" }"));
            }

            return validateProduct(movement.getProduct()).flatMap(isValidAccount -> {
                if (isValidAccount) {
                    return validateProduct(movement.getThirdProduct()).flatMap(isValidAccountTransfer -> {
                        if (isValidAccountTransfer) {
                            if (Optional.ofNullable(movement.getCustomer()).isEmpty()) {
                                return Mono.just(ResponseEntity.ok("Debe ingresar su Identificacion, Ejemplo: { \"customer\": \"78345212\" }"));
                            }

                            if (Optional.ofNullable(movement.getThirdClient()).isEmpty()) {
                                return Mono.just(ResponseEntity.ok("Debe ingresar Identificacion Beneficiario, Ejemplo: { \"thirdClient\": \"78345212\" }"));
                            }

                            if (Optional.ofNullable(movement.getAmount()).isEmpty() || movement.getAmount() == 0) {
                                return Mono.just(ResponseEntity.ok("Debe ingresar el monto diferente de cero, Ejemplo: { \"amount\": \"300.50\" }"));
                            }

                            movement.setConcept(Concept.TRANSFERENCIA.value);

                            if (Concept.TRANSFERENCIA.equals(movement.getConcept())) {
                                if (movement.getAmount() > 0) {
                                    movement.setAmount(-1 * movement.getAmount());
                                }

                                movement.setMovementType(MovementType.CARGO.value);
                                movement.setObservation("Transferencia a la cuenta " + movement.getThirdProduct() + " por la suma de " + movement.getAmount() * -1);
                            }

                            return operations.listByAccount(movement.getProduct()).collect(Collectors.summingDouble(ui -> ui.getAmount())).flatMap(balance -> {
                                if ((balance + movement.getAmount()) < 0) {
                                    return Mono.just(ResponseEntity.ok("El movimiento a efectuar sobrepasa el saldo disponible."));
                                } else {

                                    rqMovement.setMovement(getRandomNumberString());
                                    rqMovement.setDate(dateTime.format(formatDate));
                                    rqMovement.setHour(dateTime.format(formatTime));
                                    rqMovement.setState(true);

                                    return operations.create(movement).flatMap(mCG -> {
                                        if (Concept.TRANSFERENCIA.equals(movement.getConcept())) {
                                            if (movement.getAmount() < 0) {
                                                movement.setAmount(-1 * movement.getAmount());
                                            }

                                            movement.setMovementType(MovementType.ABONO.value);
                                            movement.setObservation("Transferencia desde la cuenta " + mCG.getProduct() + " por la suma de " + movement.getAmount());
                                        }
                                        movement.setProduct(movement.getThirdProduct());
                                        movement.setCustomer(movement.getThirdClient());
                                        movement.setThirdProduct(mCG.getProduct());
                                        movement.setThirdClient(mCG.getCustomer());

                                        rqMovement.setMovement(getRandomNumberString());
                                        rqMovement.setDate(dateTime.format(formatDate));
                                        rqMovement.setHour(dateTime.format(formatTime));
                                        rqMovement.setState(true);

                                        return operations.create(movement).flatMap(mAB -> {
                                            return Mono.just(ResponseEntity.ok(mCG));
                                        });
                                    });
                                }
                            });
                        } else {
                            return Mono.just(ResponseEntity.ok("¡¡La cuenta destino " + movement.getThirdProduct() + ", No existe!!"));
                        }

                    }).onErrorReturn(ResponseEntity.ok("¡¡Ocurrio un  Error en el Servicio de Cuentas o no esta disponible, porfavor verifique!!"));
                } else {
                    return Mono.just(ResponseEntity.ok("¡¡La cuenta origen " + movement.getProduct() + ", No existe!!"));
                }

            }).onErrorReturn(ResponseEntity.ok("¡¡Ocurrio un  Error en el Servicio de Cuentas o no esta disponible, porfavor verifique!!"));
        });
    }

    @PostMapping("/debit-card/payment")
    public Mono<ResponseEntity> debitCardPayment(@RequestBody AccuntCardDebit accuntCardDebit) {
        AnyDto api = new AnyDto();

        if (Optional.ofNullable(accuntCardDebit.getDebitCard()).isEmpty()) {
            return Mono.just(new ResponseEntity("Debe ingresar la targeta de debito, Ejemplo: { \"debitCard\": \"TD-78345212-653\" }", HttpStatus.BAD_REQUEST));
        }

        if (Optional.ofNullable(accuntCardDebit.getAmount()).isEmpty() || accuntCardDebit.getAmount() == 0) {
            return Mono.just(new ResponseEntity("Debe ingresar el monto diferente de cero, Ejemplo: { \"amount\": \"300.50\" }", HttpStatus.BAD_REQUEST));
        }

        return operations.createMovementWithDebitCard(accuntCardDebit.getDebitCard(), accuntCardDebit.getAmount()).flatMap(rr -> {
            if (rr.getCode().equals("1")) {
                return Mono.just(new ResponseEntity(rr, HttpStatus.OK));
            } else {
                return Mono.just(new ResponseEntity(rr, HttpStatus.OK));
            }

        });
    }

    @PostMapping("/transfer/my-account")
    public Mono<ResponseEntity> transferMyAccounts(@RequestBody Movement rqMovement) {
        rqMovement.setModality(Modality.VENTANILLA.value);
        return Mono.just(rqMovement).flatMap(movement -> {
            if (Optional.ofNullable(movement.getProduct()).isEmpty()) {
                return Mono.just(ResponseEntity.ok("Debe ingresar la cuenta de Origen, Ejemplo: { \"product\": \"78345212-653\" }"));
            }

            if (Optional.ofNullable(movement.getThirdProduct()).isEmpty()) {
                return Mono.just(ResponseEntity.ok("Debe ingresar la cuenta de Destino, Ejemplo: { \"thirdProduct\": \"78345212-653\" }"));
            }
            return validateProduct(movement.getProduct()).flatMap(isValidAccount -> {
                if (isValidAccount) {

                    return validateProduct(movement.getThirdProduct()).flatMap(isValidAccountTransfer -> {
                        if (isValidAccountTransfer) {

                            if (Optional.ofNullable(movement.getCustomer()).isEmpty()) {
                                return Mono.just(ResponseEntity.ok("Debe ingresar su Identificacion, Ejemplo: { \"customer\": \"78345212\" }"));
                            }

                            if (Optional.ofNullable(movement.getAmount()).isEmpty() || movement.getAmount() == 0) {
                                return Mono.just(ResponseEntity.ok("Debe ingresar el monto diferente de cero, Ejemplo: { \"amount\": \"300.50\" }"));
                            }

                            movement.setConcept(Concept.TRANSFERENCIA.value);

                            if (Concept.TRANSFERENCIA.equals(movement.getConcept())) {
                                if (movement.getAmount() > 0) {
                                    movement.setAmount(-1 * movement.getAmount());
                                }

                                movement.setMovementType(MovementType.CARGO.value);
                                movement.setObservation("Transferencia a la cuenta " + movement.getThirdProduct() + " por la suma de " + movement.getAmount() * -1);
                            }

                            return operations.listByAccount(movement.getProduct()).collect(Collectors.summingDouble(ui -> ui.getAmount())).flatMap(balance -> {
                                if ((balance + movement.getAmount()) < 0) {
                                    return Mono.just(ResponseEntity.ok("El movimiento a efectuar sobrepasa el saldo disponible."));
                                } else {
                                    rqMovement.setThirdClient(rqMovement.getCustomer());
                                    rqMovement.setMovement(getRandomNumberString());
                                    rqMovement.setDate(dateTime.format(formatDate));
                                    rqMovement.setHour(dateTime.format(formatTime));
                                    rqMovement.setState(true);

                                    return operations.create(movement).flatMap(mCG -> {
                                        if (Concept.TRANSFERENCIA.equals(movement.getConcept())) {
                                            if (movement.getAmount() < 0) {
                                                movement.setAmount(-1 * movement.getAmount());
                                            }

                                            movement.setMovementType(MovementType.ABONO.value);
                                            movement.setObservation("Transferencia desde la cuenta " + mCG.getProduct() + " por la suma de " + movement.getAmount());
                                        }
                                        rqMovement.setProduct(mCG.getThirdProduct());
                                        rqMovement.setThirdProduct(mCG.getProduct());
                                        rqMovement.setMovement(getRandomNumberString());
                                        rqMovement.setDate(dateTime.format(formatDate));
                                        rqMovement.setHour(dateTime.format(formatTime));
                                        rqMovement.setState(true);

                                        return operations.create(movement).flatMap(mAB -> {
                                            return Mono.just(ResponseEntity.ok(mCG));
                                        });
                                    });
                                }
                            });

                        } else {
                            return Mono.just(ResponseEntity.ok("¡¡La cuenta destino " + movement.getThirdProduct() + ", No existe!!"));
                        }

                    }).onErrorReturn(ResponseEntity.ok("¡¡Ocurrio un  Error en el Servicio de Cuentas o no esta disponible, porfavor verifique!!"));
                } else {
                    return Mono.just(ResponseEntity.ok("¡¡La cuenta origen " + movement.getProduct() + ", No existe!!"));
                }

            }).onErrorReturn(ResponseEntity.ok("¡¡Ocurrio un  Error en el Servicio de Cuentas o no esta disponible, porfavor verifique!!"));
        });
    }

    @PutMapping("/{id}")
    public Mono<Movement> update(@PathVariable("id") String id, @RequestBody Movement movement) {
        return operations.update(id, movement);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") String id) {
        operations.delete(id);
    }

    public static String getRandomNumberString() {
        Random rnd = new Random();
        int number = rnd.nextInt(999999999);
        return String.format("%09d", number);
    }

    public Mono<Boolean> validateProduct(String account) {
        /*return accountOperations.getAccount(account).flatMap(accountR -> {
            return Mono.just(true);
        }).switchIfEmpty(Mono.just(false));
         */
        return Mono.just(true);
    }

    public boolean isDateRange(String strDateI, String strDateF, String strDateC) {
        LocalDate dateI = LocalDate.parse(strDateI, formatDate);
        LocalDate dateF = LocalDate.parse(strDateF, formatDate);
        LocalDate dateC = LocalDate.parse(strDateC, formatDate);
        return ((dateC.isAfter(dateI) || dateC.isEqual(dateI)) && (dateC.isBefore(dateF) || dateC.isEqual(dateF)));
    }

}

class AccuntCardDebit {

    String debitCard;
    double amount;

    public String getDebitCard() {
        return debitCard;
    }

    public void setDebitCard(String debitCard) {
        this.debitCard = debitCard;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

}
