package edu.put;

import edu.put.apps.ClientApplication;

public class AppRunner {

    public static void main(String[] args) throws InterruptedException {
        var clientAppCount = 2;
        var clientApps = new ClientApplication[clientAppCount];

        for (int i = 0; i < clientAppCount; i++) {
            var clientApplication = new ClientApplication(i);
            clientApplication.start();
            clientApps[i] = clientApplication;
        }

        for (int i = 0; i < clientAppCount; i++) {
            clientApps[i].join();
        }

    }


}
