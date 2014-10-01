package com.opendoorlogistics.codefromweb.shapes;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;

import javax.swing.*;


/**
 * This code is from http://java-sl.com/shapes.html
 */
public class TestApplication extends JFrame {
    Canvas cPolygons = new Canvas();
    Canvas cStars = new Canvas();
    Canvas cRoundPolygons = new Canvas();
    Canvas cStandard = new Canvas();
    public TestApplication() {
        super("Shapes demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(new GridBagLayout());

        getContentPane().add(new JLabel("Regular polygons"),
                             new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
        getContentPane().add(cPolygons, new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 0, 5), 0, 0));

        getContentPane().add(new JLabel("Star polygons"),
                             new GridBagConstraints(0, 2, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
        getContentPane().add(cStars, new GridBagConstraints(0, 3, 1, 1, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 0, 5), 0, 0));

        getContentPane().add(new JLabel("Round polygons"),
                             new GridBagConstraints(0, 4, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
        getContentPane().add(cRoundPolygons, new GridBagConstraints(0, 5, 1, 1, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));

        getContentPane().add(new JLabel("Standard polygons"),
                new GridBagConstraints(0, 6, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
        getContentPane().add(cStandard, new GridBagConstraints(0, 7, 1, 1, 1, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));


        initRegular();
        initStars();
        initRoundPolygon();
        initStandard();

        setSize(800, 600);
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        TestApplication fr = new TestApplication();
        fr.setVisible(true);
    }

    protected void initRegular() {
        Shape[] shapes = new Shape[7];
        shapes[0] = new RegularPolygon(50, 50, 50, 3,3* Math.PI / 2);
        shapes[1] = new RegularPolygon(150, 100, 50, 4, 0);
        shapes[2] = new RegularPolygon(250, 50, 50, 5, Math.PI / 4);
        shapes[3] = new RegularPolygon(350, 100, 50, 6, 0);
        shapes[4] = new RegularPolygon(450, 50, 50, 7, Math.PI / 4);
        shapes[5] = new RegularPolygon(550, 100, 50, 8, 0);
        shapes[6] = new RegularPolygon(650, 50, 50, 9, Math.PI / 4);

        cPolygons.setShapes(shapes, Color.blue);
    }

    protected void initRoundPolygon() {
        Shape[] shapes = new Shape[7];
        shapes[0] = new RoundPolygon(new RegularPolygon(50, 50, 50, 3, Math.PI / 4), 10);
        shapes[1] = new RoundPolygon(new RegularPolygon(150, 100, 50, 4, 0), 15);
        shapes[2] = new RoundPolygon(new StarPolygon(250, 50, 50, 20, 5, Math.PI / 4), 7);
        shapes[3] = new RoundPolygon(new Polygon(new int[] {300, 350, 400, 350}, new int[] {50, 50, 150, 150}, 4), 10);
        shapes[4] = new RoundPolygon(new RegularPolygon(450, 50, 50, 7, Math.PI / 4), 10);
        shapes[5] = new RoundPolygon(new StarPolygon(550, 100, 50, 20, 8, 0), 10);
        shapes[6] = new RoundPolygon(new StarPolygon(650, 50, 50, 30, 9, Math.PI / 4), 10);

        cRoundPolygons.setShapes(shapes, Color.green);
    }

    protected void initStars() {
        Shape[] shapes = new Shape[7];
        shapes[0] = new StarPolygon(50, 50, 50, 10, 3, Math.PI / 4);
        shapes[1] = new StarPolygon(150, 100, 50, 20, 4, 0);
        shapes[2] = new StarPolygon(250, 50, 50, 20, 5, Math.PI / 4);
        shapes[3] = new StarPolygon(350, 100, 50, 20, 6, 0);
        shapes[4] = new StarPolygon(450, 50, 50, 30, 7, Math.PI / 4);
        shapes[5] = new StarPolygon(550, 100, 50, 20, 8, 0);
        shapes[6] = new StarPolygon(650, 50, 50, 30, 9, Math.PI / 4);

        cStars.setShapes(shapes, Color.red);
    }
    
    protected void initStandard() {
    	ArrayList<Shape> list = new ArrayList<>();


    	// triangle
    	list.add(new RegularPolygon(50, 50, 50, 3,3* Math.PI / 2));

    	// inverted triangle
    	list.add(new RegularPolygon(150, 50, 50, 3, Math.PI / 2));

    	// diamond
        list.add( new RegularPolygon(250, 50, 50, 4, 0));
        
    	// square
        list.add( new RegularPolygon(350, 50, 50, 4,Math.PI/4));

    	// pentagon
        list.add(new RegularPolygon(450, 50, 50, 5,-Math.PI/10));
        
        // star
        list.add(new StarPolygon(550, 50, 50, 6*50/10, 5, 3*Math.PI/10));
        
        // hexagon
        list.add(new RegularPolygon(650, 50, 50, 6,0));
        
//        Shape[] shapes = new Shape[7];
//        shapes[0] = new StarPolygon(50, 50, 50, 10, 3, Math.PI / 4);
//        shapes[1] = new StarPolygon(150, 100, 50, 20, 4, 0);
//        shapes[2] = new StarPolygon(250, 50, 50, 20, 5, Math.PI / 4);
//        shapes[3] = new StarPolygon(350, 100, 50, 20, 6, 0);
//        shapes[4] = new StarPolygon(450, 50, 50, 30, 7, Math.PI / 4);
//        shapes[5] = new StarPolygon(550, 100, 50, 20, 8, 0);
//        shapes[6] = new StarPolygon(650, 50, 50, 30, 9, Math.PI / 4);

        cStandard.setShapes(list.toArray(new Shape[list.size()]), Color.red);
    }

    protected static class Canvas extends JPanel {
        Shape[] shapes;
        Color color;
        public void setShapes(Shape[] shapes, Color color) {
            this.shapes = shapes;
            this.color = color;
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.white);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.black);
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            g.setColor(color);
            for (int i = 0; i < shapes.length; i++) {
                ( (Graphics2D) g).draw(shapes[i]);

            }
            RoundRectangle2D.Double rd2;
        }
    }
}
