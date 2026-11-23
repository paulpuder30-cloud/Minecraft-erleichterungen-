/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package de.erl.client.systems.waypoints.events;

import de.erl.client.systems.waypoints.Waypoint;

public record WaypointRemovedEvent(Waypoint waypoint) {
}
