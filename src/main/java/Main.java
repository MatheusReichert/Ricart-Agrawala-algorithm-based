import lombok.SneakyThrows;
import lombok.extern.java.Log;

import java.util.Scanner;
import java.util.logging.Level;

@Log()
public class Main {
    @SneakyThrows
    public static void main(String[] args) {
        Level logLevel = Level.OFF;
        PeerImpl peer = PeerImpl.getNewInstance(logLevel);
        Scanner sc = new Scanner(System.in);

        do {
            System.out.println("Recurso 1 ou 2: ");
            var opc = Integer.valueOf(sc.next());
            sc.nextLine();
            switch (opc) {
                case 1:
                    peer.useResourceOne();
                    break;
                case 2:
                    peer.useResourceTwo();
                    break;
            }

        } while (true);
    }


}
