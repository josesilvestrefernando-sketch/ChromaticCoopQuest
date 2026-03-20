package chickenfarm;

import android.annotation.TargetApi;
import android.os.Build;

import processing.core.PApplet;
import processing.core.PVector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

public class Gridmap {

    PApplet pApplet;
    int cols = 20;
    int rows = 20;
    int cellSize = 20;
    boolean[][] obstacles;
    int[][] chickenOccupant;
    ArrayList<Chicken> chickens = new ArrayList<Chicken>();

    PVector defaultStart;
    int nextChickenId = 0;
    ArrayList<Food> foodCells = new ArrayList<Food>();

    public Gridmap(PApplet pApplet1, int cols1, int rows1, int cellSize1) {
        pApplet = pApplet1;
        cols = cols1;
        rows = rows1;
        cellSize = cellSize1;

        defaultStart = new PVector(1, 1);
        obstacles = new boolean[rows1][cols1];
        chickenOccupant = new int[rows1][cols1];
        generateObstacles();

        chickens.add(new Chicken(pApplet1, this, cellCenter(defaultStart), pApplet.color(pApplet.random(120, 255), pApplet.random(90, 220), pApplet.random(60, 220)), nextChickenId++));
    }
    PVector copy(PVector pVector1) {
        return new PVector(pVector1.x, pVector1.y);
    }
    void drawFood() {
        for (Food food : foodCells) {
            pApplet.fill(food.color);
            pApplet.noStroke();
            pApplet.ellipse(cellCenter(food.cell).x, cellCenter(food.cell).y, cellSize * 0.6f, cellSize * 0.6f);
        }
    }

    public void draw() {
        pApplet.background(15, 30, 45);
        drawGrid();
        drawObstacles();
        drawFood();

        refreshOccupancy();
        for (int i = 0; i < chickens.size(); i++) {
            Chicken c = chickens.get(i);
            c.update();
            c.display();

            refreshOccupancy();
        }


    }

    public void mousePressed() {
        if (pApplet.mouseY > rows * cellSize) return;
        int cx = pApplet.constrain(pApplet.mouseX / cellSize, 0, cols - 1);
        int cy = pApplet.constrain(pApplet.mouseY / cellSize, 0, rows - 1);
        if (obstacles[cy][cx]) return;
        PVector newFoodPos = new PVector(cx, cy);
        for (Food f : foodCells) {
            if (f.cell.equals(newFoodPos)) {
                return;
            }
        }
        foodCells.add(new Food(newFoodPos, pApplet.color(pApplet.random(120, 255), pApplet.random(120, 255), pApplet.random(120, 255))));
       refreshnewtarget();
    }

    void refreshnewtarget() {
        for (int i = 0; i < chickens.size(); i++) {
            Chicken c = chickens.get(i);
            c.gridTarget =null;
        }
    }

    void drawGrid() {
        pApplet.stroke(60);
        for (int x = 0; x <= cols; x++) {
            pApplet.line(x * cellSize, 0, x * cellSize, rows * cellSize);
        }
        for (int y = 0; y <= rows; y++) {
            pApplet.line(0, y * cellSize, cols * cellSize, y * cellSize);
        }
    }

    void drawObstacles() {
        pApplet.noStroke();
        pApplet.fill(160);
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                if (obstacles[y][x]) {
                    pApplet.rect(x * cellSize, y * cellSize, cellSize, cellSize);
                }
            }
        }
    }




    void generateObstacles() {
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                obstacles[y][x] = pApplet.random(1) < 0.14;
            }
        }
        obstacles[(int) defaultStart.x][(int) defaultStart.y] = false;
    }

    PVector cellCenter(PVector cell) {
        return new PVector(cell.x * cellSize + cellSize / 2.0f, cell.y * cellSize + cellSize / 2.0f);
    }

    void refreshOccupancy() {
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                chickenOccupant[y][x] = -1;
            }
        }
        for (Chicken c : chickens) {
            PVector cell = c.getCurrentCell();
            int cx = (int) cell.x;
            int cy = (int) cell.y;
            if (cx >= 0 && cx < cols && cy >= 0 && cy < rows) {
                chickenOccupant[cy][cx] = c.id;
            }
        }
    }

    PathResult bestPathToFood(PVector from, int selfId) {
        float bestScore = Float.MAX_VALUE;
        PathResult best = null;
        for (Food food : foodCells) {
            ArrayList<PVector> candidate = findPath(from, food.cell, selfId);
            if (candidate != null && candidate.size() > 0) {
                float score = candidate.size();
                if (score < bestScore) {
                    bestScore = score;
                    best = new PathResult(copy(food.cell), candidate);
                }
            }
        }
        return best;
    }

    
    ArrayList<PVector> findPath(PVector from, PVector to, int selfId) {
        int sx = (int) from.x;
        int sy = (int) from.y;
        int tx = (int) to.x;
        int ty = (int) to.y;
        boolean[][] closed = new boolean[rows][cols];
        Node[][] nodes = new Node[rows][cols];
        PriorityQueue<Node> open = new PriorityQueue<Node>(new NodeComparator());
        Node start = new Node(sx, sy, null, 0, heuristic(sx, sy, tx, ty));
        nodes[sy][sx] = start;
        open.add(start);
        while (!open.isEmpty()) {
            Node current = open.poll();
            if (closed[current.y][current.x]) continue;
            closed[current.y][current.x] = true;
            if (current.x == tx && current.y == ty) {
                ArrayList<PVector> path = new ArrayList<PVector>();
                Node step = current;
                while (step != null) {
                    path.add(0, new PVector(step.x, step.y));
                    step = step.parent;
                }
                return path;
            }
            for (int d = 0; d < 4; d++) {
                int nx = current.x + ((d == 0) ? 1 : (d == 1) ? -1 : 0);
                int ny = current.y + ((d == 2) ? 1 : (d == 3) ? -1 : 0);
                if (nx < 0 || nx >= cols || ny < 0 || ny >= rows) continue;
                if (obstacles[ny][nx]) continue;
                if (!canOccupy(nx, ny, selfId)) continue;
                if (closed[ny][nx]) continue;
                float ng = current.g + 1;
                if (nodes[ny][nx] == null || ng < nodes[ny][nx].g) {
                    Node neighbor = new Node(nx, ny, current, ng, heuristic(nx, ny, tx, ty));
                    nodes[ny][nx] = neighbor;
                    open.add(neighbor);
                }
            }
        }
        return new ArrayList<PVector>();
    }

    boolean canOccupy(int x, int y, int selfId) {
        int occupant = chickenOccupant[y][x];
        return occupant == -1 || occupant == selfId;
    }

    float heuristic(int x, int y, int tx, int ty) {
        return pApplet.abs(x - tx) + pApplet.abs(y - ty);
    }


    //Next to do.
    //1. Chicken should stil detect the collission even in wandering.
    //2. The food should has a random color.
    //3. The chicken will be affected by the food color by adding and getting the average. For example multiply the
    //color of chicken to two then add to food color then divided by three is the new color of the chicken.
    //4. Chicken can have a new chick after eating of 3 food.
    //5. The size of a chicken was on 3 phase for every food it eats. small, medium, average


}
