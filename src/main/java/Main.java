import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import lombok.SneakyThrows;
import lombok.extern.flogger.Flogger;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;

@Log()
public class Main {
    @SneakyThrows
    public static void main(String[] args) {
        Level logLevel = Level.OFF;
        PeerImpl peer = PeerImpl.getNewInstance(logLevel);
        Scanner sc = new Scanner(System.in);

        while (true) {
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

        }
    }


}
