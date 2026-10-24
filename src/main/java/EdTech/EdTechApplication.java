package EdTech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class EdTechApplication {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(EdTechApplication.class, args);
    }
}
