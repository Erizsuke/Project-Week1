package com.yourorg.taskmanager.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * One-off helper to generate a BCrypt hash for the admin seed migration
 * (V2__seed_admin.sql). Run this class's main method, copy the printed hash
 * into the migration file, then change ADMIN_PASSWORD to something strong
 * of your own before running.
 *
 * Not wired into the Spring context — plain java, run directly from your IDE
 * (right-click > Run) or via: mvn -q exec:java -Dexec.mainClass=... (if exec plugin added).
 */
public class BCryptHashGenerator {

    private static final String ADMIN_PASSWORD = "ChangeMe@Admin123";

    public static void main(String[] args) {
        String hash = new BCryptPasswordEncoder().encode(ADMIN_PASSWORD);
        System.out.println("Password: " + ADMIN_PASSWORD);
        System.out.println("BCrypt hash: " + hash);
        System.out.println("Paste the hash above into V2__seed_admin.sql");
    }
}
