package com.jazzkuh.commandlib.minestom.utils.permission;

public interface PermissionProvider {
    boolean hasPermission(Permissable permissable, String permission);
}
