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
        // 1. Client ket noi toi Node A
        Registry regA = LocateRegistry.getRegistry("127.0.0.1", PORT_A);
        IClientDataService clientServiceA = (IClientDataService) regA.lookup("ClientDataService");

        // Dang nhap
        boolean loggedIn = clientServiceA.login("admin", "admin");
        Assertions.assertTrue(loggedIn, "Dang nhap tai khoan admin phai thanh cong tren Node A");

        // 2. Tao ban ghi moi tren Node A
        RecordItem newRecord = new RecordItem();
        newRecord.setTitle("Test Distributed Record 101");
        newRecord.setCategory("Research");
        newRecord.setContent("Noi dung du lieu dong bo phan tan giua 2 Server qua RMI.");
        newRecord.setUpdatedBy("admin");

        boolean created = clientServiceA.createRecord(newRecord);
        Assertions.assertTrue(created, "Tao ban ghi tren Node A phai thanh cong");

        // 3. Cho 1 giay de tien trinh replication bat dong bo truyen sang Node B
        Thread.sleep(1000);

        // 4. Client ket noi toi Node B de kiem tra du lieu da sang Node B chua
        Registry regB = LocateRegistry.getRegistry("127.0.0.1", PORT_B);
        IClientDataService clientServiceB = (IClientDataService) regB.lookup("ClientDataService");

        List<RecordItem> recordsOnB = clientServiceB.getAllRecords();
        Assertions.assertFalse(recordsOnB.isEmpty(), "Danh sach records tren Node B khong duoc rong");

        boolean existsOnB = recordsOnB.stream().anyMatch(r -> r.getTitle().equals("Test Distributed Record 101"));
        Assertions.assertTrue(existsOnB, "Ban ghi tao tu Node A phai xuat hien tren Node B!");

        // 5. Lay ban ghi tren Node B va cap nhat noi dung tu Node B
        RecordItem recFromB = recordsOnB.stream()
                .filter(r -> r.getTitle().equals("Test Distributed Record 101"))
                .findFirst().orElse(null);
        Assertions.assertNotNull(recFromB);

        recFromB.setContent("Noi dung da duoc sua doi boi Client tren Node B!");
        boolean updatedOnB = clientServiceB.updateRecord(recFromB);
        Assertions.assertTrue(updatedOnB, "Cap nhat ban ghi tu Node B phai thanh cong");

        // 6. Cho 1 giay de dong bo nguoc tu Node B sang Node A
        Thread.sleep(1000);

        List<RecordItem> recordsOnA = clientServiceA.getAllRecords();
        RecordItem recFromA = recordsOnA.stream()
                .filter(r -> r.getId().equals(recFromB.getId()))
                .findFirst().orElse(null);

        Assertions.assertNotNull(recFromA);
        Assertions.assertEquals("Noi dung da duoc sua doi boi Client tren Node B!", recFromA.getContent(),
                "Du lieu sua tu Node B phai duoc dong bo nguoc lai ve Node A!");
    }
}
