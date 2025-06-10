package pcd.ass03.view;

import pcd.ass03.actors.GUIActor;
import pcd.ass03.model.BoidState;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BoidsPanel extends JPanel {

	private int framerate;
    private int nBoids;
    private final int envWidth, envHeight;
    private List<BoidState> boids;

    public BoidsPanel(int envWidth, int envHeight, int nBoids, List<BoidState> boids) {
    	this.envWidth = envWidth;
        this.envHeight = envHeight;
        this.nBoids = nBoids;
        this.boids = boids;
    }

    public void setNBoids(int nBoids) {
        this.nBoids = nBoids;
    }

    public void updateBoids(List<BoidState> newBoids) {
        this.boids = new ArrayList<>(newBoids);
    }

    public void setFrameRate(int framerate) {
    	this.framerate = framerate;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(Color.WHITE);

        g.setColor(Color.BLUE);
        for (BoidState boid : boids) {
        	var x = boid.pos().x();
        	var y = boid.pos().y();
        	int px = (int)(this.envWidth/2 + x);
        	int py = (int)(this.envHeight/2 - y);
            g.fillOval(px,py, 5, 5);
        }
        
        g.setColor(Color.BLACK);
        g.drawString("Num. Boids: " + nBoids, 10, 25);
        g.drawString("Framerate: " + framerate, 10, 40);
   }
}
