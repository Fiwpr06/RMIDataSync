package com.fiwpr06.rmidatasync.server;

import javafx.application.Application;

public class ServerBLauncher {
    public static void main(String[] args) {
        ServerApp.setStartupNode("NODE_B");
        Application.launch(ServerApp.class, "NODE_B");
    }
}
