package pcd.ass03.model;

import pcd.ass03.BoidsModel;
import pcd.ass03.utils.*;

import java.util.*;

public record BoidState (P2d pos, V2d vel, String id) {
    
    private List<BoidState> getNearbyBoids(BoidsModel model) {
    	var list = new ArrayList<BoidState>();
        for (BoidState other : model.getBoids()) {
        	if (other != this) {
        		P2d otherPos = other.pos;
        		double distance = pos.distance(otherPos);
        		if (distance < model.getPerceptionRadius()) {
        			list.add(other);
        		}
        	}
        }
        return list;
    }
    
    private V2d calculateAlignment(List<BoidState> nearbyBoids, BoidsModel model) {
        double avgVx = 0;
        double avgVy = 0;
        if (nearbyBoids.size() > 0) {
	        for (BoidState other : nearbyBoids) {
	        	V2d otherVel = other.vel;
	            avgVx += otherVel.x();
	            avgVy += otherVel.y();
	        }	        
	        avgVx /= nearbyBoids.size();
	        avgVy /= nearbyBoids.size();
	        return new V2d(avgVx - vel.x(), avgVy - vel.y()).getNormalized();
        } else {
        	return new V2d(0, 0);
        }
    }

    private V2d calculateCohesion(List<BoidState> nearbyBoids, BoidsModel model) {
        double centerX = 0;
        double centerY = 0;
        if (nearbyBoids.size() > 0) {
	        for (BoidState other: nearbyBoids) {
	        	P2d otherPos = other.pos;
	            centerX += otherPos.x();
	            centerY += otherPos.y();
	        }
            centerX /= nearbyBoids.size();
            centerY /= nearbyBoids.size();
            return new V2d(centerX - pos.x(), centerY - pos.y()).getNormalized();
        } else {
        	return new V2d(0, 0);
        }
    }
    
    private V2d calculateSeparation(List<BoidState> nearbyBoids, BoidsModel model) {
        double dx = 0;
        double dy = 0;
        int count = 0;
        for (BoidState other: nearbyBoids) {
        	P2d otherPos = other.pos;
    	    double distance = pos.distance(otherPos);
    	    if (distance < model.getAvoidRadius()) {
    	    	dx += pos.x() - otherPos.x();
    	    	dy += pos.y() - otherPos.y();
    	    	count++;
    	    }
    	}
        if (count > 0) {
            dx /= count;
            dy /= count;
            return new V2d(dx, dy).getNormalized();
        } else {
        	return new V2d(0, 0);
        }
    }
}
