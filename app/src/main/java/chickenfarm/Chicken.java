package chickenfarm;

import processing.core.PApplet;
import processing.core.PVector;

import java.util.ArrayList;

public class Chicken {
    PApplet pApplet;
    PVector pos;
    int bodyColor;
    PVector gridTarget = null;
    ArrayList<PVector> path = new ArrayList<PVector>();
    float speed = 2f;
    float age = 0;
    int foodsEaten = 0;
    float wanderOffset;
    int id;
    Gridmap gridmap;
    boolean needsNewTarget = true;
    float wandermultiplier=.5f;
    Chicken(PApplet pApplet1, Gridmap gridmap1, PVector startPos, int c, int id) {
        pApplet = pApplet1;
        gridmap = gridmap1;
        pos = copy(startPos);
        bodyColor = c;
        wanderOffset = pApplet.random(1000);
        this.id = id;
    }

    PVector copy(PVector pVector1) {
        return new PVector(pVector1.x, pVector1.y);
    }

    void update() {

        if (!gridmap.foodCells.isEmpty()) {
            PVector currentCell = getCurrentCell();
            needsNewTarget = gridTarget == null || !containsFood(gridTarget) || path.isEmpty();
            if (!needsNewTarget && ((int) gridTarget.x < 0 || !gridmap.canOccupy((int) gridTarget.x, (int) gridTarget.y, id))) {
                needsNewTarget = true;
            }
            if (needsNewTarget) {
                PathResult best = gridmap.bestPathToFood(currentCell, id);
                if (best != null) {
                    gridTarget = copy(best.food);
                    path = best.path;
                } else {
                    gridTarget = null;
                    path.clear();
                }
            }
            followPath();
        } else {
            gridTarget = null;
            path.clear();
            idleWander();
        }
    }

    boolean containsFood(PVector cell) {
        for (Food food : gridmap.foodCells) {
            if (food.cell.equals(cell)) return true;
        }

        return false;
    }

    PVector mult(PVector pVector1, float multiplier) {
        float x1 = pVector1.x * multiplier;
        float y1 = pVector1.y * multiplier;
        return new PVector(x1, y1);
    }

    void idleWander() {
        float baseAngle = pApplet.noise(wanderOffset + age * 0.01f) * pApplet.TWO_PI * 2;
        boolean moved = false;
        for (int attempt = 0; attempt < 6 && !moved; attempt++) {
            float angle = baseAngle + attempt * (pApplet.PI / 4);
            PVector dir = new PVector(pApplet.cos(angle), pApplet.sin(angle));
            PVector next = PVector.add(pos, mult(dir, wandermultiplier));
            PVector cell = posToCell(next);
            if (isValidCell(cell)) {
                pos = next;
                moved = true;
            }
        }
        pos.x = pApplet.constrain(pos.x, gridmap.cellSize / 2.0f, pApplet.width - gridmap.cellSize / 2.0f);
        pos.y = pApplet.constrain(pos.y, gridmap.cellSize / 2.0f, gridmap.rows * gridmap.cellSize - gridmap.cellSize / 2.0f);
    }

    PVector posToCell(PVector position) {
        int cx = pApplet.constrain(pApplet.floor(position.x / gridmap.cellSize), 0, gridmap.cols - 1);
        int cy = pApplet.constrain(pApplet.floor(position.y / gridmap.cellSize), 0, gridmap.rows - 1);
        return new PVector(cx, cy);
    }

    boolean isValidCell(PVector cell) {
        int cx = (int) cell.x;
        int cy = (int) cell.y;
        if (cx < 0 || cx >= gridmap.cols || cy < 0 || cy >= gridmap.rows) return false;
        if (gridmap.obstacles[cy][cx]) return false;
        if (!gridmap.canOccupy(cx, cy, id)) return false;
        return true;
    }

    void blendWithFood(int foodColor) {
        float cr = pApplet.red(bodyColor);
        float cg = pApplet.green(bodyColor);
        float cb = pApplet.blue(bodyColor);
        float fr = pApplet.red(foodColor);
        float fg = pApplet.green(foodColor);
        float fb = pApplet.blue(foodColor);
        float nr = pApplet.constrain((2 * cr + fr) / 3.0f, 0, 255);
        float ng = pApplet.constrain((2 * cg + fg) / 3.0f, 0, 255);
        float nb = pApplet.constrain((2 * cb + fb) / 3.0f, 0, 255);
        bodyColor = pApplet.color(nr, ng, nb);
    }

    void followPath() {
        if (path == null || path.isEmpty()) return;
        while (!path.isEmpty() && path.get(0).equals(getCurrentCell())) {
            path.remove(0);
        }
        if (path.isEmpty()) {
            checkFoodReached();
            return;
        }
        PVector nextCell = path.get(0);
        int nx = (int) nextCell.x;
        int ny = (int) nextCell.y;
        if (!gridmap.canOccupy(nx, ny, id)) {
            gridTarget = null;
            path.clear();
            return;
        }
        PVector next = gridmap.cellCenter(nextCell);
        PVector dir = PVector.sub(next, pos);
        float dist = dir.mag();
        if (dist < 1.5f) {
            path.remove(0);
            if (path.isEmpty()) {
                checkFoodReached();
            }
        } else {
            dir.normalize();
            pos.add(mult(dir,pApplet.min(speed, dist)));
        }
    }

    void checkFoodReached() {
        PVector currentCell = getCurrentCell();
        int indexToRemove = -1;
        int consumedColor = pApplet.color(255);

        for (int i = 0; i < gridmap.foodCells.size(); i++) {
            if (gridmap.foodCells.get(i).cell.equals(currentCell)) {
                consumedColor = gridmap.foodCells.get(i).color;
                indexToRemove = i;
                break;
            }
        }
        if (indexToRemove >= 0) {
            gridmap.foodCells.remove(indexToRemove);
            foodsEaten++;
            blendWithFood(consumedColor);
            grow();
            maybeSpawnChick();
        }
        gridTarget = null;
        path.clear();
    }

    void grow() {
        age++;
    }

    void maybeSpawnChick() {
        if (foodsEaten >= 5) {
            foodsEaten = 0;
            PVector spawn = findEmptyAdjacent(getCurrentCell());
            if (spawn != null) {
                int childColor = getChickColor();
                gridmap.chickens.add(new Chicken(pApplet, gridmap, gridmap.cellCenter(spawn), childColor, gridmap.nextChickenId++));
                gridmap.refreshOccupancy();
            }
        }
    }

    PVector findEmptyAdjacent(PVector base) {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] dir : dirs) {
            int nx = (int) base.x + dir[0];
            int ny = (int) base.y + dir[1];
            if (nx < 0 || nx >= gridmap.cols || ny < 0 || ny >= gridmap.rows) continue;
            if (gridmap.obstacles[ny][nx]) continue;
            if (gridmap.chickenOccupant[ny][nx] == -1) {
                return new PVector(nx, ny);
            }
        }
        return null;
    }

    int getChickColor() {
        return bodyColor;
    }

    float brighten(float channel) {
        return pApplet.min(channel + 40, 255);
    }

    void display() {
        float sizemul = 1;
        if (age < 3) {
            sizemul = .4f;
        } else if (age < 6) {
            sizemul = .8f;
        } else if (age < 9) {
            sizemul = 1;
        }
        float radius = gridmap.cellSize * sizemul;
        pApplet.fill(bodyColor);
        pApplet.noStroke();
        pApplet.ellipse(pos.x, pos.y, radius, radius);
        pApplet.stroke(255, 200);
        pApplet.noFill();
        pApplet.strokeWeight(1);
        pApplet.ellipse(pos.x, pos.y, radius + 6, radius + 6);
    }


    PVector getCurrentCell() {
        int cx = pApplet.constrain(pApplet.floor(pos.x / gridmap.cellSize), 0, gridmap.cols - 1);
        int cy = pApplet.constrain(pApplet.floor(pos.y / gridmap.cellSize), 0, gridmap.rows - 1);
        return new PVector(cx, cy);
    }
}
