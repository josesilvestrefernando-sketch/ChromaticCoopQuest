package chickenfarm;

public class Node {
    int x, y;
    Node parent;
    float g, f;
    Node(int x, int y, Node parent, float g, float h) {
        this.x = x;
        this.y = y;
        this.parent = parent;
        this.g = g;
        this.f = g + h;
    }

}
