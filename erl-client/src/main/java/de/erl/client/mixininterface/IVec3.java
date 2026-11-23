/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.mixininterface;

import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

@SuppressWarnings("UnusedReturnValue")
public interface IVec3 {
    Vec3 erl$set(double x, double y, double z);

    default Vec3 erl$set(Vec3i vec) {
        return erl$set(vec.getX(), vec.getY(), vec.getZ());
    }

    default Vec3 erl$set(Vector3d vec) {
        return erl$set(vec.x, vec.y, vec.z);
    }

    default Vec3 erl$set(Vec3 pos) {
        return erl$set(pos.x, pos.y, pos.z);
    }

    Vec3 erl$setXZ(double x, double z);

    Vec3 erl$setY(double y);
}
