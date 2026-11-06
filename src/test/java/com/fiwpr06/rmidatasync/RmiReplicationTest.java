package com.fiwpr06.rmidatasync;

import com.fiwpr06.rmidatasync.server.core.RmiServerManager;
import com.fiwpr06.rmidatasync.shared.model.RecordItem;
import com.fiwpr06.rmidatasync.shared.rmi.IClientDataService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

public class RmiReplicationTest {
    private static RmiServerManager nodeA;
    private static RmiServerManager nodeB;

    private static final int PORT_A = 10991;
    private static final int PORT_B = 10992;

    @BeforeAll
    public static void setup() {
        // Node A
        nodeA = new RmiServerManager("NODE_A", PORT_A, "127.0.0.1", PORT_B, "rmisync_test_a");
        boolean startedA = nodeA.start();
        Assertions.assertTrue(startedA, "Node A phai start thanh cong");

        // Node B
        nodeB = new RmiServerManager("NODE_B", PORT_B, "127.0.0.1", PORT_A, "rmisync_test_b");
        boolean startedB = nodeB.start();
        Assertions.assertTrue(startedB, "Node B phai start thanh cong");
    }

    @AfterAll
    public static void tearDown() {
        if (nodeA != null) nodeA.stop();
        if (nodeB != null) nodeB.stop();
    }

    @Test
    public void testTwoWayReplication() throws Exception {
        Registry regA = LocateRegistry.getRegistry("127.0.0.1", PORT_A);
        IClientDataService clientServiceA = (IClientDataService) regA.lookup("ClientDataService");

        // Tao ban ghi moi tren Node A
        RecordItem newRecord = new RecordItem();
        newRecord.setTitle("Test Distributed Record 101");
        newRecord.setCategory("Research");
        newRecord.setPrice(15000000);
        newRecord.setQuantity(10);
        newRecord.setStatus("Còn hàng");
        newRecord.setContent("Noi dung du lieu dong bo phan tan giua 2 Server qua RMI.");
        newRecord.setUpdatedBy("admin");

        boolean created = clientServiceA.createRecord(newRecord);
        Assertions.assertTrue(created, "Tao ban ghi tren Node A phai thanh cong");

        // Cho de replication truyen sang Node B
        Thread.sleep(1000);

        Registry regB = LocateRegistry.getRegistry("127.0.0.1", PORT_B);
        IClientDataService clientServiceB = (IClientDataService) regB.lookup("ClientDataService");

        List<RecordItem> recordsOnB = clientServiceB.getAllRecords();
        Assertions.assertFalse(recordsOnB.isEmpty(), "Danh sach records tren Node B khong duoc rong");

        boolean existsOnB = recordsOnB.stream().anyMatch(r -> r.getTitle().equals("Test Distributed Record 101"));
        Assertions.assertTrue(existsOnB, "Ban ghi tao tu Node A phai xuat hien tren Node B!");

        RecordItem recFromB = recordsOnB.stream()
                .filter(r -> r.getTitle().equals("Test Distributed Record 101"))
                .findFirst().orElse(null);
        Assertions.assertNotNull(recFromB);

        recFromB.setContent("Noi dung da duoc sua doi boi Client tren Node B!");
        boolean updatedOnB = clientServiceB.updateRecord(recFromB);
        Assertions.assertTrue(updatedOnB, "Cap nhat ban ghi tu Node B phai thanh cong");

        // Cho de dong bo nguoc ve Node A
        Thread.sleep(1000);

        List<RecordItem> recordsOnA = clientServiceA.getAllRecords();
        RecordItem recFromA = recordsOnA.stream()
                .filter(r -> r.getId().equals(recFromB.getId()))
                .findFirst().orElse(null);

        Assertions.assertNotNull(recFromA);
        Assertions.assertEquals("Noi dung da duoc sua doi boi Client tren Node B!", recFromA.getContent(),
                "Du lieu sua tu Node B phai duoc dong bo nguoc lai ve Node A!");
    }

    @Test
    public void testSingleActiveSessionAcrossNodes() throws Exception {
        Registry regA = LocateRegistry.getRegistry("127.0.0.1", PORT_A);
        IClientDataService clientServiceA = (IClientDataService) regA.lookup("ClientDataService");

        Registry regB = LocateRegistry.getRegistry("127.0.0.1", PORT_B);
        IClientDataService clientServiceB = (IClientDataService) regB.lookup("ClientDataService");

        String testUser = "user_session_" + System.currentTimeMillis();
        clientServiceA.register(testUser, "123", "Session Test User");
        Thread.sleep(500);

        // 1. Login tren Node A -> Thanh cong
        String statusA = clientServiceA.loginDetailed(testUser, "123");
        Assertions.assertEquals("SUCCESS", statusA, "Dang nhap tren Node A phai thanh cong");

        // 2. Thu login cung user tren Node B -> Bi chan ngay vi dang online tren Node A!
        String statusB = clientServiceB.loginDetailed(testUser, "123");
        Assertions.assertTrue(statusB.startsWith("ALREADY_LOGGED_IN:"), "Node B phai tu choi dang nhap vi user dang online: " + statusB);

        // 3. Logout tren Node A
        clientServiceA.logout(testUser);
        Thread.sleep(500);

        // 4. Bay gio login tren Node B -> Thanh cong!
        String statusB2 = clientServiceB.loginDetailed(testUser, "123");
        Assertions.assertEquals("SUCCESS", statusB2, "Sau khi logout tren Node A, Node B phai cho phep dang nhap");

        clientServiceB.logout(testUser);
    }

    @Test
    public void testAdjustStockReplication() throws Exception {
        Registry regA = LocateRegistry.getRegistry("127.0.0.1", PORT_A);
        IClientDataService clientServiceA = (IClientDataService) regA.lookup("ClientDataService");

        Registry regB = LocateRegistry.getRegistry("127.0.0.1", PORT_B);
        IClientDataService clientServiceB = (IClientDataService) regB.lookup("ClientDataService");

        RecordItem item = new RecordItem();
        item.setTitle("Laptop Dell XPS 13 Plus");
        item.setCategory("Máy tính xách tay");
        item.setPrice(32000000);
        item.setQuantity(20);
        item.setStatus("Còn hàng");

        clientServiceA.createRecord(item);
        Thread.sleep(1000);

        List<RecordItem> itemsB = clientServiceB.getAllRecords();
        RecordItem itemB = itemsB.stream().filter(r -> r.getTitle().equals("Laptop Dell XPS 13 Plus")).findFirst().orElse(null);
        Assertions.assertNotNull(itemB);

        // Nhap kho +10 tu Node B
        boolean adjusted = clientServiceB.adjustStock(itemB.getId(), 10, "admin");
        Assertions.assertTrue(adjusted);

        Thread.sleep(1000);

        List<RecordItem> itemsA = clientServiceA.getAllRecords();
        RecordItem itemA = itemsA.stream().filter(r -> r.getId().equals(itemB.getId())).findFirst().orElse(null);
        Assertions.assertNotNull(itemA);
        Assertions.assertEquals(30, itemA.getQuantity(), "Ton kho phai duoc cap nhat len 30 tren Node A sau khi Node B nhap +10!");
    }

    @Test
    public void testUserAccountBiDirectionalReplicationAndReconciliation() throws Exception {
        Registry regA = LocateRegistry.getRegistry("127.0.0.1", PORT_A);
        IClientDataService clientServiceA = (IClientDataService) regA.lookup("ClientDataService");

        Registry regB = LocateRegistry.getRegistry("127.0.0.1", PORT_B);
        IClientDataService clientServiceB = (IClientDataService) regB.lookup("ClientDataService");

        // 1. Dang ky user moi tren Node A
        String usernameA = "sync_user_a_" + System.currentTimeMillis();
        boolean regOkA = clientServiceA.register(usernameA, "Pass@123", "User Created On A");
        Assertions.assertTrue(regOkA, "Dang ky user tren Node A phai thanh cong");

        // Cho de RMI replicate sang Node B
        Thread.sleep(1000);

        // Kiem tra Node B phai co user nay va dang nhap duoc voi mat khau that
        String loginOnB = clientServiceB.loginDetailed(usernameA, "Pass@123");
        Assertions.assertEquals("SUCCESS", loginOnB, "User dang ky tren Node A phai dang nhap duoc tren Node B!");
        clientServiceB.logout(usernameA);

        // 2. Dang ky user moi tren Node B
        String usernameB = "sync_user_b_" + System.currentTimeMillis();
        boolean regOkB = clientServiceB.register(usernameB, "Secret@456", "User Created On B");
        Assertions.assertTrue(regOkB, "Dang ky user tren Node B phai thanh cong");

        // Cho de RMI replicate sang Node A
        Thread.sleep(1000);

        // Kiem tra Node A phai co user nay va dang nhap duoc
        String loginOnA = clientServiceA.loginDetailed(usernameB, "Secret@456");
        Assertions.assertEquals("SUCCESS", loginOnA, "User dang ky tren Node B phai dang nhap duoc tren Node A!");
        clientServiceA.logout(usernameB);

        // 3. Kiem tra tinh nang Reconcile User 2 chieu khi chay reconcileWithPeer
        nodeA.reconcileWithPeer();
        Thread.sleep(1500);

        List<com.fiwpr06.rmidatasync.shared.model.User> usersOnA = nodeA.getUserDao().getAllUsers();
        List<com.fiwpr06.rmidatasync.shared.model.User> usersOnB = nodeB.getUserDao().getAllUsers();

        boolean foundOnA = usersOnA.stream().anyMatch(u -> u.getUsername().equals(usernameB));
        boolean foundOnB = usersOnB.stream().anyMatch(u -> u.getUsername().equals(usernameA));

        Assertions.assertTrue(foundOnA, "Node A phai co user tao tu Node B");
        Assertions.assertTrue(foundOnB, "Node B phai co user tao tu Node A");

        // 4. Test doi mat khau tren Node A -> Phai dong bo sang Node B
        boolean passUpdated = clientServiceA.updatePassword(usernameA, "NewPass@789");
        Assertions.assertTrue(passUpdated, "Doi mat khau tren Node A phai thanh cong");
        Thread.sleep(1000);

        String loginNewPassB = clientServiceB.loginDetailed(usernameA, "NewPass@789");
        Assertions.assertEquals("SUCCESS", loginNewPassB, "Node B phai dang nhap duoc voi mat khau moi da dong bo tu Node A!");
        clientServiceB.logout(usernameA);
    }

    @Test
    public void testDeleteRecordReplicationAndNoResurrect() throws Exception {
        Registry regA = LocateRegistry.getRegistry("127.0.0.1", PORT_A);
        IClientDataService clientServiceA = (IClientDataService) regA.lookup("ClientDataService");

        Registry regB = LocateRegistry.getRegistry("127.0.0.1", PORT_B);
        IClientDataService clientServiceB = (IClientDataService) regB.lookup("ClientDataService");

        // 1. Tao san pham moi de test xoa
        RecordItem item = new RecordItem();
        item.setTitle("San pham test xoa phan tan");
        item.setCategory("Test");
        item.setPrice(500000);
        item.setQuantity(5);
        item.setStatus("Còn hàng");

        clientServiceA.createRecord(item);
        Thread.sleep(1000);

        List<RecordItem> itemsB = clientServiceB.getAllRecords();
        RecordItem foundB = itemsB.stream().filter(r -> r.getTitle().equals("San pham test xoa phan tan")).findFirst().orElse(null);
        Assertions.assertNotNull(foundB, "San pham phai co tren Node B");

        // 2. Xoa san pham tren Node A -> Node B phai bi xoa
        boolean deleted = clientServiceA.deleteRecord(foundB.getId());
        Assertions.assertTrue(deleted, "Xoa tren Node A phai thanh cong");
        Thread.sleep(1000);

        List<RecordItem> itemsAfterDeleteB = clientServiceB.getAllRecords();
        boolean stillOnB = itemsAfterDeleteB.stream().anyMatch(r -> r.getId().equals(foundB.getId()));
        Assertions.assertFalse(stillOnB, "Node B phai khong con san pham sau khi da bi xoa tren Node A");

        // 3. Reconcile giua 2 server -> San pham khong duoc phep hoi sinh!
        nodeA.reconcileWithPeer();
        Thread.sleep(1500);

        List<RecordItem> itemsAfterReconcileA = clientServiceA.getAllRecords();
        List<RecordItem> itemsAfterReconcileB = clientServiceB.getAllRecords();
        Assertions.assertFalse(itemsAfterReconcileA.stream().anyMatch(r -> r.getId().equals(foundB.getId())), "San pham khong duoc hoi sinh tren Node A");
        Assertions.assertFalse(itemsAfterReconcileB.stream().anyMatch(r -> r.getId().equals(foundB.getId())), "San pham khong duoc hoi sinh tren Node B");
    }

    @Test
    public void testPairingHandshake() {
        // 1. Node A gui yeu cau ghep doi sang Node B (mac dinh chap nhan)
        RmiServerManager.PairingResult result = nodeA.connectAndPairWithPeer("127.0.0.1", PORT_B);
        Assertions.assertTrue(result.isSuccess(), "Ghep doi mac dinh phai thanh cong: " + result.getMessage());
        Assertions.assertTrue(nodeA.isPeerConnected(), "Node A phai ket noi thanh cong voi Node B");
        Assertions.assertTrue(nodeB.isPeerConnected(), "Node B phai nhan dien va ket noi voi Node A");

        // 2. Test tinh huong Node B tu choi (Reject)
        nodeB.setPairingRequestHandler((originNodeId, originHost, originPort, originDbName) -> false);

        RmiServerManager.PairingResult rejectResult = nodeA.connectAndPairWithPeer("127.0.0.1", PORT_B);
        Assertions.assertFalse(rejectResult.isSuccess(), "Khi doi tac reject thi ket qua phai la false");
        Assertions.assertTrue(rejectResult.getMessage().contains("từ chối"), "Thong bao phai chua tu khoa tu choi");

        // Khoi phuc handler mac dinh chap nhan cho cac test tiep theo
        nodeB.setPairingRequestHandler((originNodeId, originHost, originPort, originDbName) -> true);
        RmiServerManager.PairingResult reAccepted = nodeA.connectAndPairWithPeer("127.0.0.1", PORT_B);
        Assertions.assertTrue(reAccepted.isSuccess(), "Khoi phuc chap nhan phai thanh cong");
    }
}
