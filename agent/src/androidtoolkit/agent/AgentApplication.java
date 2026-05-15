package androidtoolkit.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the AndroidToolkit Agent.
 * The Agent runs on a developer's local machine, performing ADB operations
 * and bridging device data to the Server via WebSocket.
 */
@SpringBootApplication
public class AgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
}
