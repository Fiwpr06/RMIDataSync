package com.fiwpr06.rmidatasync.shared.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Ban ghi du lieu Kho Hang & San pham phan tan duoc dong bo giua 2 Server qua Java RMI
 */
public class RecordItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String title;
    private String category;
    private double price;
    private int quantity;
    private String status;
    private String content;
    private int version;
    private long lastModified;
    private String updatedBy;
    private String originNode; // Node A hoac Node B (nguon goc ban ghi)
    private boolean synced;

    public RecordItem() {
        this.status = "Còn hàng";
        this.version = 1;
        this.synced = true;
    }

    public RecordItem(String id, String title, String category, double price, int quantity, String status, String content, int version, long lastModified, String updatedBy, String originNode, boolean synced) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.price = price;
        this.quantity = quantity;
        this.status = status != null ? status : "Còn hàng";
        this.content = content;
        this.version = version;
        this.lastModified = lastModified;
        this.updatedBy = updatedBy;
        this.originNode = originNode;
        this.synced = synced;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public String getOriginNode() { return originNode; }
    public void setOriginNode(String originNode) { this.originNode = originNode; }

    public boolean isSynced() { return synced; }
    public void setSynced(boolean synced) { this.synced = synced; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecordItem that = (RecordItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
