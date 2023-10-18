import lombok.SneakyThrows;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

@Log
public class ResourceDummy {
    private Scanner sc = new Scanner(System.in);
    @SneakyThrows
    public void use(){
        for(int i= 0; i<= 3; i++){

            Thread.sleep(1000);
            System.out.println("Utilizando recurso: "+Math.ceil(33.333*(i))+"%");
        }
    }
}
