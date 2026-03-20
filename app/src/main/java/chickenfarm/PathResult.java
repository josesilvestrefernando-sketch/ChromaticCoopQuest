package chickenfarm;

import processing.core.PVector;

import java.util.ArrayList;

public class PathResult {
    PVector food;
    ArrayList<PVector> path;
    PathResult(PVector food, ArrayList<PVector> path) {
        this.food = food;
        this.path = path;
    }
}
