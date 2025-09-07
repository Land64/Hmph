package hmph.rendering.world;

import hmph.math.Vector3f;

public enum Direction {
    NORTH(0, 0, -1),
    SOUTH(0, 0, 1),
    EAST(1, 0, 0),
    WEST(-1, 0, 0),
    UP(0, 1, 0),
    DOWN(0, -1, 0);

    private final float dx, dy, dz;

    Direction(float dx, float dy, float dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    public float x() { return dx; }
    public float y() { return dy; }
    public float z() { return dz; }

    public Vector3f[] getVertices() {
        switch (this) {
            case NORTH:
                return new Vector3f[]{
                        new Vector3f(1, 0, 0),
                        new Vector3f(0, 0, 0),
                        new Vector3f(0, 1, 0),
                        new Vector3f(1, 1, 0)
                };
            case SOUTH:
                return new Vector3f[]{
                        new Vector3f(0, 0, 1),
                        new Vector3f(1, 0, 1),
                        new Vector3f(1, 1, 1),
                        new Vector3f(0, 1, 1)
                };
            case EAST:
                return new Vector3f[]{
                        new Vector3f(1, 0, 0),
                        new Vector3f(1, 0, 1),
                        new Vector3f(1, 1, 1),
                        new Vector3f(1, 1, 0)
                };
            case WEST:
                return new Vector3f[]{
                        new Vector3f(0, 0, 0),
                        new Vector3f(0, 0, 1),
                        new Vector3f(0, 1, 1),
                        new Vector3f(0, 1, 0)
                };
            case UP:
                return new Vector3f[]{
                        new Vector3f(1, 1, 0),
                        new Vector3f(0, 1, 0),
                        new Vector3f(0, 1, 1),
                        new Vector3f(1, 1, 1)
                };
            case DOWN:
                return new Vector3f[]{
                        new Vector3f(1, 0, 1),
                        new Vector3f(0, 0, 1),
                        new Vector3f(0, 0, 0),
                        new Vector3f(1, 0, 0)
                };
            default:
                return new Vector3f[0];
        }
    }
}