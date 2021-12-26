package com.example.heliumproject;

public class Values {

    private String latitude,longitude,altitude;
    private float speed;
    private String addressLine;
    private float[] gravity, linear_acceleration;

    public Values() {

    }

    public Values(String latitude, String longitude, String altitude, float speed, String addressLine, float[] gravity, float[] linear_acceleration) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.speed = speed;
        this.addressLine = addressLine;
        this.gravity = gravity;
        this.linear_acceleration = linear_acceleration;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public void setAltitude(String altitude) {
        this.altitude = altitude;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public void setGravity(float[] gravity) {
        this.gravity = gravity;
    }

    public void setLinear_acceleration(float[] linear_acceleration) {
        this.linear_acceleration = linear_acceleration;
    }
}
