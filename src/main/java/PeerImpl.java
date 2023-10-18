import dto.AccessGrantDTO;
import dto.RequestDTO;
import dto.ResourceEnum;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.*;
import java.util.logging.Level;


@Log
public class PeerImpl extends UnicastRemoteObject implements Peer {
    private Registry nameService;
    private String id;
    private KeyPair keyPair;
    private long timestampResourceOne = 0;
    private long timestampResourceTwo = 0;

    private Map<String, PublicKey> peersPublicKeys = new HashMap<>();
    private Set<RequestDTO> resourceOneRequests = new TreeSet<>();
    private Set<RequestDTO> resourceTwoRequests = new TreeSet<>();

    private ResourceDummy resourceOne = new ResourceDummy();
    private ResourceDummy resourceTwo = new ResourceDummy();

    private int resourceOneGrantCount = 0;
    private int resourceTwoGrantCount = 0;

    Signature signature = Signature.getInstance("SHA256withRSA");


    @SneakyThrows
    public static PeerImpl getNewInstance(Level logLevel) {
        log.setLevel(logLevel);
        return new PeerImpl();
    }

    @SneakyThrows
    public PeerImpl() throws RemoteException, NoSuchAlgorithmException {

        this.id = UUID.randomUUID().toString();
        log.info("Peer id: " + this.id);
        this.nameService = createOrGetNameService();
        this.keyPair = generateKeys();
        if (this.keyPair != null) {
            updatePublicKeys();
        }
        getKeys();
    }

    @SneakyThrows
    public void updatePublicKeys() {
        for (String name : nameService.list()) {
            Thread.ofVirtual().start(() -> {
                try {
                    if (!name.equals(this.id)) {
                        log.info("Enviando chave para: " + name);
                        var peer = (Peer) nameService.lookup(name);
                        peer.joinNetwork(this.id, this.keyPair.getPublic());
                        var key = peer.getPublicKey();
                        this.peersPublicKeys.put(name, key);
                    }
                } catch (Exception e) {
                    log.info(e.getMessage());
                }
            }).join();
        }
    }

    public Registry createOrGetNameService() {
        Registry nameService = null;
        try {
            nameService = LocateRegistry.createRegistry(1099);
            log.info("Name service created");
        } catch (RemoteException e) {
            log.info("Name service already created");
        }

        if (nameService == null) {
            try {
                nameService = LocateRegistry.getRegistry(1099);
                log.info("Name service located");
            } catch (RemoteException e) {
                log.info("Name service error ??: " + e.getMessage());
            }
        }

        try {
            nameService.bind(this.id, this);
        } catch (Exception e) {
            log.info("Cannot bind: " + e.getMessage());
        }

        return nameService;
    }

    @SneakyThrows
    public void getKeys() {
        log.info("GetKeys");
        List<String> peers = Arrays.asList(this.nameService.list());
        for (String peerId : peers) {
            Thread.ofVirtual().start(() -> {
                try {
                    if (!peerId.equals(this.id)) {
                        Peer peer = (Peer) this.nameService.lookup(peerId);

                        this.peersPublicKeys.put(peerId, peer.getPublicKey());
                    }
                } catch (Exception e) {
                    log.info(e.getMessage());
                }
            }).join();

        }
    }

    @SneakyThrows
    public KeyPair generateKeys() {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        log.info("Chaves geradas");

        return keyPairGen.generateKeyPair();

    }

    @SneakyThrows
    public boolean useResourceOne() {
        if (resourceOneGrantCount == peersPublicKeys.keySet().size()) {
            this.resourceOne.use();

            this.timestampResourceOne = 0;
            this.resourceOneGrantCount = 0;

            Thread.ofVirtual().start(() -> {
                try {
                    this.releaseResource(ResourceEnum.RESOURCE_ONE);
                } catch (Exception e) {
                    log.info(e.getMessage());
                }
            }).join();

            return true;
        }
        this.timestampResourceOne = System.currentTimeMillis();

        signature.initSign(this.keyPair.getPrivate());
        byte[] msg = UUID.randomUUID().toString().getBytes();
        signature.update(msg);

        RequestDTO dto = RequestDTO.builder().timestamp(this.timestampResourceOne).id(this.id).resourceSelected(
                ResourceEnum.RESOURCE_ONE).msg(msg).digitalSignature(signature.sign()).build();

        for (String extId : this.peersPublicKeys.keySet()) {
            Thread.ofVirtual().start(() -> {

                try {
                    Peer peer = (Peer) this.nameService.lookup(extId);
                    peer.requestResource(dto);
                } catch (Exception e) {
                    log.info("Erro" + e.getMessage());
                }

            }).join();
        }
        return false;

    }

    @SneakyThrows
    public boolean useResourceTwo() {
        if (resourceTwoGrantCount == peersPublicKeys.keySet().size()) {
            this.resourceTwo.use();

            this.timestampResourceTwo = 0;
            this.resourceTwoGrantCount = 0;

            Thread.ofVirtual().start(() -> {
                try {
                    this.releaseResource(ResourceEnum.RESOURCE_TWO);
                } catch (Exception e) {
                    log.info(e.getMessage());
                }
            }).join();

            return true;
        }
        this.timestampResourceTwo = System.currentTimeMillis();

        signature.initSign(this.keyPair.getPrivate());

        byte[] msg = UUID.randomUUID().toString().getBytes();

        signature.update(msg);

        RequestDTO dto = RequestDTO.builder().timestamp(this.timestampResourceTwo).id(this.id).resourceSelected(
                ResourceEnum.RESOURCE_TWO).msg(msg).digitalSignature(signature.sign()).build();
        for (String extId : this.peersPublicKeys.keySet()) {
            Thread.ofVirtual().start(() -> {

                try {
                    Peer peer = (Peer) this.nameService.lookup(extId);
                    peer.requestResource(dto);
                } catch (Exception e) {
                    log.info("Erro" + e.getMessage());
                }

            }).join();
        }
        return false;
    }

    @Override
    @SneakyThrows
    public void requestResource(RequestDTO dto) throws RemoteException {
        log.info("requestResource");

        signature.initVerify(this.peersPublicKeys.get(dto.getId()));
        signature.update(dto.getMsg());

        if (!signature.verify(dto.getDigitalSignature())) {
            log.info("Assinatura Invalida");
            return;
        }

        signature.initSign(keyPair.getPrivate());

        byte[] msg = UUID.randomUUID().toString().getBytes();

        signature.update(msg);
        switch (dto.getResourceSelected()) {
            case RESOURCE_ONE: {

                if (this.timestampResourceOne > dto.getTimestamp() || this.timestampResourceOne == 0) {
                    Peer peer = (Peer) this.nameService.lookup(dto.getId());
                    AccessGrantDTO acessGrantDto = AccessGrantDTO.builder().id(this.id).msg(msg).digitalSignature(
                            signature.sign()).resourceSelected(ResourceEnum.RESOURCE_ONE).build();
                    log.info("Acess garanted resource 1");
                    peer.grantResourceAccess(acessGrantDto);
                } else {
                    resourceOneRequests.add(dto);
                }
            }
            break;
            case RESOURCE_TWO:
                if (this.timestampResourceTwo > dto.getTimestamp() || this.timestampResourceTwo == 0) {
                    Peer peer = (Peer) this.nameService.lookup(dto.getId());
                    AccessGrantDTO acessGrantDto = AccessGrantDTO.builder().id(this.id).msg(msg).digitalSignature(
                            signature.sign()).resourceSelected(ResourceEnum.RESOURCE_TWO).build();
                    log.info("Acess garanted resource 2");
                    peer.grantResourceAccess(acessGrantDto);
                } else {
                    resourceTwoRequests.add(dto);
                }
                break;
            default:
                break;
        }
    }

    @SneakyThrows
    @Override
    public void grantResourceAccess(AccessGrantDTO dto) throws RemoteException {
        log.info("grantResourceAccess");

        signature.initVerify(this.peersPublicKeys.get(dto.getId()));
        signature.update(dto.getMsg());

        if (!signature.verify(dto.getDigitalSignature())) {
            log.info("Assinatura Invalida");
            return;
        }


        switch (dto.resourceSelected) {
            case RESOURCE_ONE: {
                resourceOneGrantCount++;
                if (resourceOneGrantCount == peersPublicKeys.keySet().size()) {
                    this.useResourceOne();
                }
            }
            break;
            case RESOURCE_TWO: {
                resourceTwoGrantCount++;
                if (resourceTwoGrantCount == peersPublicKeys.keySet().size()) {
                    this.useResourceTwo();
                }
            }
            break;
        }

    }


    @SneakyThrows
    public void releaseResource(ResourceEnum resource) throws RemoteException {

        signature.initSign(keyPair.getPrivate());

        byte[] msg = UUID.randomUUID().toString().getBytes();

        signature.update(msg);
        var signatureF = signature.sign();
        switch (resource) {
            case RESOURCE_ONE: {
                for (RequestDTO request : this.resourceOneRequests) {
                    Thread.ofVirtual().name("Release resource thread").start(() -> {

                        AccessGrantDTO acessGrantDto = AccessGrantDTO.builder().id(this.id).msg(msg).digitalSignature(
                                signatureF).resourceSelected(ResourceEnum.RESOURCE_ONE).build();
                        try {
                            Peer peer = (Peer) this.nameService.lookup(request.getId());
                            peer.grantResourceAccess(acessGrantDto);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                        this.resourceOneGrantCount = 0;
                    }).join();
                }
                this.resourceOneRequests.clear();
            }
            break;
            case RESOURCE_TWO: {
                for (RequestDTO request : this.resourceTwoRequests) {
                    Thread.ofVirtual().name("Release resource thread").start(() -> {

                        AccessGrantDTO acessGrantDto = AccessGrantDTO.builder().id(this.id).msg(msg).digitalSignature(
                                signatureF).resourceSelected(ResourceEnum.RESOURCE_TWO).build();
                        try {
                            Peer peer = (Peer) this.nameService.lookup(request.getId());
                            peer.grantResourceAccess(acessGrantDto);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                        this.resourceTwoGrantCount = 0;
                    }).join();
                }
                this.resourceTwoRequests.clear();
            }
            break;
        }
    }

    @Override
    public PublicKey getPublicKey() throws RemoteException {
        return this.keyPair.getPublic();
    }

    @Override
    public void joinNetwork(String name, PublicKey publicKey) throws RemoteException {
        log.info("Recebendo chave de: " + name);
        this.peersPublicKeys.put(name, publicKey);
    }
}
