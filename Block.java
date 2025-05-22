import java.awt.Color;


public class Block {


    private double pX, pY;
    private double width, height;
    private Color color;


    public Block(double x, double y, double w, double h, Color c) {
        pX = x;
        pY = y;
        width = w;
        height = h;
        color = c;
    }

    public double getX() { return pX; }
    public double getY() { return pY; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public Color getColor() { return color; }

}
