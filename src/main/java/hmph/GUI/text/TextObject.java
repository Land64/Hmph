package hmph.GUI.text;

public class TextObject {
    private String text;
    private float x;
    private float y;
    private float r = 1f, g = 1f, b = 1f, a = 1f;

    public TextObject(String text, float x, float y) {
        this.text = text != null ? text : "";
        this.x = x;
        this.y = y;
    }

    public String getText() { return text; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getR() { return r; }
    public float getG() { return g; }
    public float getB() { return b; }
    public float getA() { return a; }

    public void setText(String text) { this.text = text != null ? text : ""; }
    public void setPosition(float x, float y) { this.x = x; this.y = y; }
    public void setColor(float r, float g, float b, float a) {
        this.r = r; this.g = g; this.b = b; this.a = a;
    }
}
