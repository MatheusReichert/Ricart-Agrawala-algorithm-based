import lombok.SneakyThrows;
import lombok.extern.java.Log;
@Log
public class ResourceDummy {
    @SneakyThrows
    public void use(){
        for(int i= 0; i<= 3; i++){

            Thread.sleep(1000);
            System.out.println("Utilizando recurso: "+Math.ceil(33.333*(i))+"%");
        }
    }
}
