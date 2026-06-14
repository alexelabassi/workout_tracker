package com.thesis.workout.gym.application;

/** Internal create/update payload for a gym, decoupled from the web request DTO. */
public record GymCommand(String name, String location) {
}
