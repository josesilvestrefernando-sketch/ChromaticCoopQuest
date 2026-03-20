package chickenfarm;

import processing.core.PVector;

public class Food {
    PVector cell;
    int color;
    Food(PVector cell, int color1) {
        this.cell = cell;
        this.color = color1;
    }
}
