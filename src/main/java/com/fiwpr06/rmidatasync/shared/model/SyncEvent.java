package com.fiwpr06.rmidatasync.shared.model;

import java.io.Serializable;

/**
 * Thuc the su kien dong bo RMI luu trong MySQL va hien thi tren Dashboard
 */
public class SyncEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private SyncAction eventType;
    private String recordId;
    private String recordTitle;
    private String sourceNode;
    private String targetNode;
    private String timestamp;
    private String status; // SUCCESS, FAILED, PENDING

    public SyncEvent() {}

    public SyncEvent(int id, SyncAction eventType, String recordId, String recordTitle, String sourceNode, String targetNode, String timestamp, String status) {
        this.id = id;
        this.eventType = eventType;
        this.recordId = recordId;
        this.recordTitle = recordTitle;
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;
        this.timestamp = timestamp;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public SyncAction getEventType() { return eventType; }
    public void setEventType(SyncAction eventType) { this.eventType = eventType; }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getRecordTitle() { return recordTitle; }
    public void setRecordTitle(String recordTitle) { this.recordTitle = recordTitle; }

    public String getSourceNode() { return sourceNode; }
    public void setSourceNode(String sourceNode) { this.sourceNode = sourceNode; }

    public String getTargetNode() { return targetNode; }
    public void setTargetNode(String targetNode) { this.targetNode = targetNode; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
