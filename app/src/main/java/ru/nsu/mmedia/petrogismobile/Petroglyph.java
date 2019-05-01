package ru.nsu.mmedia.petrogismobile;

/**
 * Created by vkazakov on 26.07.2017.
 */

public class Petroglyph {
    public int id;
    public String uuid;
    public double lat;
    public double lng;
    public String name;
    public String image;
    public double orientation_x;
    public double orientation_y;
    public double orientation_z;


    Petroglyph(int id, String uuid, String name, double lat, double lng, String image, double orientation_x, double orientation_y, double orientation_z)
    {
        this.id = id;
        this.uuid = uuid;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.image = image;
        this.orientation_x = orientation_x;
        this.orientation_y = orientation_y;
        this.orientation_z = orientation_z;
    }
}
