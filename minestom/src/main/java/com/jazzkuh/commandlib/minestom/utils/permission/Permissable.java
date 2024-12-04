package com.jazzkuh.commandlib.minestom.utils.permission;

import com.jazzkuh.commandlib.minestom.MinestomCommandLoader;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class Permissable {
    private final UUID uuid;

    public boolean hasPermission(String permission) {
        if (uuid == null) return true;
        return MinestomCommandLoader.getPermissionProvider().hasPermission(this, permission);
    }
}
