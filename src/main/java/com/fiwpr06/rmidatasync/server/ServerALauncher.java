package com.fiwpr06.rmidatasync.server;

import javafx.application.Application;

public class ServerALauncher {
    public static void main(String[] args) {
        ServerApp.setStartupNode("NODE_A");
        Application.launch(ServerApp.class, "NODE_A");
    }
}
