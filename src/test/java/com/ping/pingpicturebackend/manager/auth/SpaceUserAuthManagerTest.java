package com.ping.pingpicturebackend.manager.auth;

import com.ping.pingpicture.infrastructure.manager.auth.SpaceUserAuthManager;
import org.junit.jupiter.api.Test;

import java.util.List;
class SpaceUserAuthManagerTest {

    @Test
    void test() {
        SpaceUserAuthManager spaceUserAuthManager = new SpaceUserAuthManager();
        String spaceUserRole = "viewer";
        List<String> result = spaceUserAuthManager.getPermissionsByRole(spaceUserRole);
        System.out.println(result);
    }
}