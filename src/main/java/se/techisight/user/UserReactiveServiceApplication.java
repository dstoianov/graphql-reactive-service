package se.techisight.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.stream.Stream;

@SpringBootApplication
public class UserReactiveServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserReactiveServiceApplication.class, args);
    }

}


@Controller
class UserGraphqlController {

    public UserGraphqlController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private final UserRepository userRepository;

    //    @SchemaMapping(typeName = "Query", field = "users")
    @QueryMapping
    Flux<User> users() {
        return this.userRepository.findAll();
    }

    @QueryMapping
    Flux<User> usersByName(@Argument String name) {
        return this.userRepository.findByName(name);
    }


    @SubscriptionMapping
    Flux<UserEvent> userEvents(@Argument Integer userId) {
        return this.userRepository.findById(userId)
                .flatMapMany(user -> {
                    Stream<UserEvent> generate = Stream.generate(
                            () -> new UserEvent(user, Math.random() > .5 ? UserEventType.DELETED : UserEventType.UPDATED)
                    );
                    return Flux.fromStream(generate);
                })
                .delayElements(Duration.ofSeconds(1))
                .take(10);
    }


    @MutationMapping
    Mono<User> addUser(@Argument String name) {
        return this.userRepository.save(new User(null, name));
    }

    @SchemaMapping(typeName = "User")
    Flux<Order> orders(User user) {
        var orders = new ArrayList<Order>();
//        for (var orderId = 1, maxOrderId = new Random().nextInt(100); orderId <= maxOrderId ; orderId++) {
        for (var orderId = 1; orderId <= (Math.random() * 100); orderId++) {
            orders.add(new Order(orderId, user.id()));
        }
        return Flux.fromIterable(orders);
    }


}


interface UserRepository extends ReactiveCrudRepository<User, Integer> {
    Flux<User> findByName(String name);
}

enum UserEventType {
    UPDATED,
    DELETED;
}

record UserEvent(User user, UserEventType userEventType) {
};

record Order(Integer id, Integer userId) {
}

record User(@JsonProperty("id") @Id Integer id, @JsonProperty("name") String name) {
}
