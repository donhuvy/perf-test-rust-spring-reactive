package dev.tpcoder.perftest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

@Repository
interface EmployeeRepository extends ReactiveCrudRepository<Employee, Long> {

}

@SpringBootApplication
public class PerfTestApplication {

    private final Logger logger = LoggerFactory.getLogger(PerfTestApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(PerfTestApplication.class, args);
    }

    @Bean
    CommandLineRunner init(EmployeeRepository employeeRepository) {
        return (args) -> logger.info("The database has employee: {}", employeeRepository.count().block());
    }

}

@Component
class TestHandler {
    private static final MediaType json = MediaType.APPLICATION_JSON;
    private final EmployeeRepository employeeRepository;

    TestHandler(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Bean
    RouterFunction<ServerResponse> composedRoutes() {
        return RouterFunctions
                .route(GET("/").and(accept(json)), this::findAll)
                .andRoute(GET("/{id}").and(accept(json)), this::findById)
                .andRoute(POST("/").and(accept(json)), this::createEmployee);
    }

    public Mono<ServerResponse> findAll(ServerRequest request) {
        var result = employeeRepository.findAll();
        return ServerResponse.ok().contentType(json).body(result, Employee.class);
    }

    public Mono<ServerResponse> findById(ServerRequest request) {
        var id = request.pathVariable("id");
        var result = employeeRepository.findById(Long.valueOf(id));
        return ServerResponse.ok().contentType(json).body(result, Employee.class);
    }

    public Mono<ServerResponse> createEmployee(ServerRequest request) {
        Mono<Employee> payload = request.bodyToMono(Employee.class);
        var result = payload.flatMap(employeeRepository::save);
        return ServerResponse.ok().contentType(json).body(result, Employee.class);
    }
}

record Employee(@Id Long id, String name) {

}