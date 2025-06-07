package pcd.ass01;

/**
 *
 * 2-dimensional point
 * objects are completely state-less
 *
 */
public record P2d(double x, double y) {


    public P2d sum(V2d v){
        return new P2d(x+v.x(),y+v.y());
    }

    public V2d sub(P2d v){
        return new V2d(x-v.x,y-v.y);
    }
    
    public double distance(P2d p) {
    	double dx = p.x - x;
    	double dy = p.y - y;
    	return Math.sqrt(dx*dx + dy*dy);

    }
    
    public String toString(){
        return "P2d("+x+","+y+")";
    }

}
