package pcd.ass03.view;

import pcd.ass03.actors.GUIActor;
import pcd.ass03.model.BoidState;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class BoidsPanel extends JPanel {

	private final int width, height, nBoids;
    private int framerate;
    private final List<BoidState> boids;

    public BoidsPanel(int width, int height, int nBoids, List<BoidState> boids) {
    	this.width = width;
        this.height = height;
        this.nBoids = nBoids;
        this.boids = boids;
    }

    public void setFrameRate(int framerate) {
    	this.framerate = framerate;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(Color.WHITE);
        
        var w = GUIActor.SCREEN_WIDTH;
        var h = GUIActor.SCREEN_HEIGHT;
        var envWidth = this.width;
        var xScale = w/envWidth;
        // var envHeight = this.height;
        // var yScale = h/envHeight;

        g.setColor(Color.BLUE);
        for (BoidState boid : boids) {
        	var x = boid.pos().x();
        	var y = boid.pos().y();
        	int px = (int)(w/2 + x*xScale);
        	int py = (int)(h/2 - y*xScale);
            g.fillOval(px,py, 5, 5);
        }
        
        g.setColor(Color.BLACK);
        g.drawString("Num. Boids: " + nBoids, 10, 25);
        g.drawString("Framerate: " + framerate, 10, 40);
   }
}
