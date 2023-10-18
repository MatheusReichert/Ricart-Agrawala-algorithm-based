import dto.AccessGrantDTO;
import dto.RequestDTO;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.PublicKey;

public interface Peer extends Remote {
    public void requestResource(RequestDTO dto) throws RemoteException;
    public void grantResourceAccess(AccessGrantDTO dto) throws RemoteException;
    public PublicKey getPublicKey() throws RemoteException;
    public void joinNetwork(String name, PublicKey publicKey)  throws RemoteException;

}
