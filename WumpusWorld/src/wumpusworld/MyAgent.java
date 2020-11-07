package wumpusworld;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Contains starting code for creating your own Wumpus World agent.
 * Currently the agent only make a random decision each turn.
 * 
 * @author Johan Hagelbäck
 */
public class MyAgent implements Agent
{
    private World w;
    private int[] next_visit;
    private boolean on_path = false;
    private boolean first_move=  true;
    private boolean shoot = false;
    
    private ArrayList<int[]> safe_spots = new ArrayList<int[]>();
    private NaiveBayes nb;
    private ArrayList<int[]> check_next_visit = new ArrayList<int[]>();
    private ArrayList<int[]> worst_pos = new ArrayList<int[]>();
    private ArrayList<int[]> stenches = new ArrayList<int[]>();
    private int[] dead_end = {0,0};
    private ArrayList<int[]> dead_visits = new ArrayList<int[]>();


    
    /**
     * Creates a new instance of your solver agent.
     * 
     * @param world Current world state 
     */
    public MyAgent(World world)
    {
        w = world;
        nb = new NaiveBayes(world);
    }
   
            
    /**
     * Asks your solver agent to execute an action.
     */

    public void doAction()
    {
        //Location of the player
        int cX = w.getPlayerX();
        int cY = w.getPlayerY();
        
     //   nb.add_position(cX, cY);
        
        
        //Basic action:
        //Grab Gold if we can.
        if (w.hasGlitter(cX, cY))
        {
            System.out.println("I found the gold!");
            w.doAction(World.A_GRAB);
            return;
        }
        
        //Basic action:
        //We are in a pit. Climb up.
        if (w.isInPit())
        {
            System.out.println("I fell in a pit...");
            w.doAction(World.A_CLIMB);
            return;
        }
        
        //Test the environment
        if (w.hasBreeze(cX, cY))
        {
            System.out.println("I am in a Breeze");
        }
        if (w.hasStench(cX, cY))
        {
            if(first_move){
                w.doAction(World.A_SHOOT);
                System.out.println("I shoot");
            }
            set_stench(cX, cY);
//            nb.set_stench(cX, cY);
            System.out.println("I am in a Stench");
        }
        if (w.hasPit(cX, cY))
        {
            System.out.println("I am in a Pit");
        }
        if (w.getDirection() == World.DIR_RIGHT)
        {
            System.out.println("I am facing Right");
        }
        if (w.getDirection() == World.DIR_LEFT)
        {
            System.out.println("I am facing Left");
        }
        if (w.getDirection() == World.DIR_UP)
        {
            System.out.println("I am facing Up");
        }
        if (w.getDirection() == World.DIR_DOWN)
        {
            System.out.println("I am facing Down");
        }
        
        nb.add_position(cX, cY);

        //After every move, we have to check if we have new information about the Wumpus and adjust the ArrayList worst_pos
        for(int i = 0; i < worst_pos.size(); i++){
            int checkX = worst_pos.get(i)[0];
            int checkY = worst_pos.get(i)[1];
            if(!(w.isUnknown(checkX + 1, checkY) || !w.isValidPosition(checkX + 1, checkY) || w.hasStench(checkX+ 1, checkY))){
                worst_pos.remove(i);
                i--;
            } else if (!(w.isUnknown(checkX - 1, checkY)|| !w.isValidPosition(checkX - 1, checkY) || w.hasStench(checkX - 1, checkY))){
                worst_pos.remove(i);
                i--;
            } else if (!(w.isUnknown(checkX, checkY + 1)|| !w.isValidPosition(checkX, checkY + 1) || w.hasStench(checkX, checkY + 1))){
                worst_pos.remove(i);
                i--;
            } else if (!(w.isUnknown(checkX, checkY - 1)|| !w.isValidPosition(checkX, checkY - 1) || w.hasStench(checkX, checkY - 1))){
                worst_pos.remove(i);
                i--;
            }
        }
        //If the cell we have moved to was a safe cell, we have to remove it from the safe_spots-ArrayList
        int[] pos = {cX, cY};  
        for(int i = 0; i < safe_spots.size(); i++){
            if (Arrays.equals(pos, safe_spots.get(i))){
                safe_spots.remove(i);
            }
        }
        
        //There are three different cases for the next movement:
        //1. We have a goal (so we are on_path): we are just moving to the next cell
        //2. We are on a cell without breeze and stench, so we can just move on
        //3. a) If we are starting in a stench, we shoot.
        //   b) If we feel a breeze or a stench and have no goal, we check if there
        //   is a safe cell we can aim. Otherwise we do the naive bayes calculation to
        //   get the next best cell.
        if(on_path){
            System.out.println("I still have a goal ([" + next_visit[0] + ", " + next_visit[1] + "]) I don't have to calculate anything.");
            move_to_next();
        }
        else if (!w.hasBreeze(cX, cY) && !w.hasStench(cX, cY))
        {
            System.out.println("I don't feel a breeze or stench, so I can just move forward.");
            if(!w.hasPit(cX, cY)){
                add_safe_spots(cX, cY);
            }
            if (hasBump(w.getDirection(), cX, cY)){
                int new_dir = (w.getDirection()+1)%4;
                if(!hasBump(new_dir,cX,cY)){
                    //turning right
                    w.doAction(World.A_TURN_RIGHT);
                    w.doAction(World.A_MOVE);
                } else {
                    //turning left
                    w.doAction(World.A_TURN_LEFT);
                    w.doAction(World.A_MOVE);
                }
            } else {
                w.doAction(World.A_MOVE);
            }
        }
        else 
        {
            if(safe_spots.isEmpty()){
                System.out.println("I don't have more safe cells left, so I have to calculate with naive bayes.");
                //The get_best_cell returns the cell with the best probability (of no_pit). If there are several, the distances are calculated
                //and it chooses the closest one.
                check_next_visit = nb.get_best_cell(worst_pos);
                if(check_next_visit.size() == 1)
                {
                    next_visit = check_next_visit.get(0);
                } else {
                    double distance = Integer.MAX_VALUE;
                    int index = -1;
                    for (int i = 0; i < check_next_visit.size(); i++) {
                        double check_distance = calculate_distance(check_next_visit.get(i)[0], check_next_visit.get(i)[1]);
			if(check_distance < distance) {
                            distance = check_distance;
                            index = i;
			}			
                    }
                    next_visit = check_next_visit.get(index);
                }
                System.out.println("move to: " + "[" + next_visit[0] + ", " + next_visit[1] + "]");
                if(worst_pos.size() > 0){
                    if(worst_pos.get(0)[0] == next_visit[0] && worst_pos.get(0)[1] == next_visit[1]){
                        shoot = true;
                        System.out.println("I should shoot ");
                    }
                }
                on_path = true;
                move_to_next();
            } else {
                System.out.println("There is another safe cell left I can move to.");
                next_visit = check_safe_spots();
                on_path = true;
                System.out.println("move to: " + "[" + next_visit[0] + ", " + next_visit[1] + "]");
                move_to_next();
            }
        }
        first_move = false;
    }
    
    // This function returns true if the agent is facing a wall.
    public boolean hasBump(int dir, int x, int y){
        if(dir == World.DIR_DOWN){
            return !w.isValidPosition(x, y-1);
        } else if (dir == World.DIR_LEFT){
            return !w.isValidPosition(x-1, y);
        } else if (dir == World.DIR_RIGHT){
            return !w.isValidPosition(x+1, y);
        } else {
            return !w.isValidPosition(x, y+1);
        }
    }
    
    // This function is used, when the agent has a goal. With every call, it moves the agent a bit closer by using the path which he visited already.
    public void move_to_next(){
        int newX = next_visit[0];
        int newY = next_visit[1];
        int currentX = w.getPlayerX();
        int currentY = w.getPlayerY();
        boolean is_adjacent = false;
        if(calculate_distance(newX, newY) == 1){
            is_adjacent = true;
        }
        if(newX > currentX){
            if(w.isValidPosition(currentX+1, currentY) && !w.isUnknown(currentX+1, currentY) && !w.hasPit(currentX+1, currentY) && (currentX+1!=dead_end[0] || currentY!=dead_end[1]) || is_adjacent){
                turn_and_move(currentX+1, currentY, is_adjacent);
                return;
            }
        } else if (newX < currentX) {
            if(w.isValidPosition(currentX-1, currentY) && !w.isUnknown(currentX-1, currentY) && !w.hasPit(currentX-1, currentY) && (currentX-1!=dead_end[0] || currentY!=dead_end[1]) || is_adjacent){
                turn_and_move(currentX-1, currentY, is_adjacent);
                return;
            }
        } 
        if (newY > currentY) {
            if(w.isValidPosition(currentX, currentY+1) && !w.isUnknown(currentX, currentY + 1) && !w.hasPit(currentX, currentY + 1) && (currentX!=dead_end[0] || currentY+1!=dead_end[1]) || is_adjacent){
                turn_and_move(currentX, currentY + 1, is_adjacent);
                return;
            }
        }  else if(newY < currentY){
            if(w.isValidPosition(currentX, currentY-1) && !w.isUnknown(currentX, currentY - 1) && !w.hasPit(currentX, currentY - 1) && (currentX!=dead_end[0] || currentY-1!=dead_end[1])  || is_adjacent){
                turn_and_move(currentX, currentY - 1, is_adjacent);
                return;
            }
        }
        ArrayList<int[]> adjacents = new ArrayList<int[]>();
        if(w.isValidPosition(currentX, currentY+1) && !w.isUnknown(currentX, currentY+1) && !w.hasPit(currentX, currentY+1) && currentY+1!=dead_end[1]){
            adjacents.add(new int[] {currentX, currentY+1});
        }
        if(w.isValidPosition(currentX, currentY-1) && !w.isUnknown(currentX, currentY-1) && !w.hasPit(currentX, currentY-1) && currentY-1!=dead_end[1]){
            adjacents.add(new int[] {currentX, currentY-1});
        }
        if(w.isValidPosition(currentX+1, currentY) && !w.isUnknown(currentX+1, currentY) && !w.hasPit(currentX+1, currentY) && currentX+1!=dead_end[0]){
            adjacents.add(new int[] {currentX+1, currentY});
        }
        if(w.isValidPosition(currentX-1, currentY) && !w.isUnknown(currentX-1, currentY) && !w.hasPit(currentX-1, currentY) && currentX-1!=dead_end[0]){
            adjacents.add(new int[] {currentX-1, currentY});
        }
        turn_and_move(adjacents.get(0)[0], adjacents.get(0)[1], is_adjacent);
        dead_end[0] = currentX;
        dead_end[1] = currentY;
    }
    
    // This function gets the X and Y of the next step and if the final goal is adjacent.
    // If the final goal is adjacent and contains the wumpus (shoot == true), this function
    // calls the World.A_SHOOT.
    public void turn_and_move(int nextX, int nextY, boolean goal_is_adjacent){
        int currentX = w.getPlayerX();
        int currentY = w.getPlayerY();
        if(nextX > currentX){
                if(w.getDirection() == World.DIR_RIGHT){
                    if(goal_is_adjacent && shoot){
                        w.doAction(World.A_SHOOT);
                        shoot = false;
                    }
                    w.doAction(World.A_MOVE);
                } else if (w.getDirection() == World.DIR_DOWN){
                    w.doAction(World.A_TURN_LEFT);
                    if(goal_is_adjacent && shoot){
                        w.doAction(World.A_SHOOT);
                        shoot = false;
                    }
                    w.doAction(World.A_MOVE);
                } else if (w.getDirection() == World.DIR_LEFT){
                    w.doAction(World.A_TURN_LEFT);
                    w.doAction(World.A_TURN_LEFT);
                    if(goal_is_adjacent && shoot){
                        w.doAction(World.A_SHOOT);
                        shoot = false;
                    }
                    w.doAction(World.A_MOVE);
                } else {
                    w.doAction(World.A_TURN_RIGHT);
                    if(goal_is_adjacent && shoot){
                        w.doAction(World.A_SHOOT);
                        shoot = false;
                    }
                    w.doAction(World.A_MOVE);
                }
        } else if (nextX < currentX){
                if(w.getDirection() == World.DIR_LEFT){
                    if(goal_is_adjacent && shoot){
                        w.doAction(World.A_SHOOT);
                        shoot = false;
                    }
                    w.doAction(World.A_MOVE);
                } else if (w.getDirection() == World.DIR_UP){
                    w.doAction(World.A_TURN_LEFT);
                    if(goal_is_adjacent && shoot){
                        w.doAction(World.A_SHOOT);
                        shoot = false;
                    }
                    w.doAction(World.A_MOVE);
                } else if (w.getDirection() == World.DIR_RIGHT){
                    w.doAction(World.A_TURN_LEFT);
                    w.doAction(World.A_TURN_LEFT);
                    if(goal_is_adjacent && shoot){
                        w.doAction(World.A_SHOOT);
                        shoot = false;
                    }
                    w.doAction(World.A_MOVE);
                } else {
                    w.doAction(World.A_TURN_RIGHT);
                    if(goal_is_adjacent && shoot){
                        w.doAction(World.A_SHOOT);
                        shoot = false;
                    }
                    w.doAction(World.A_MOVE);
                }
        } else  if (nextY > currentY) {
                if(w.getDirection() == World.DIR_UP){
                    if(goal_is_adjacent && shoot){
                        w.doAction(World.A_SHOOT);
                        shoot = false;
                    }
                    w.doAction(World.A_MOVE);
                } else if (w.getDirection() == World.DIR_RIGHT){
                    w.doAction(World.A_TURN_LEFT);
                    if(goal_is_adjacent && shoot){
                        w.doAction(World.A_SHOOT);
                        shoot = false;
                    }
                    w.doAction(World.A_MOVE);
                } else if (w.getDirection() == World.DIR_DOWN){
                    w.doAction(World.A_TURN_LEFT);
                    w.doAction(World.A_TURN_LEFT);
                    if(goal_is_adjacent && shoot){
                        w.doAction(World.A_SHOOT);
                        shoot = false;
                    }
                    w.doAction(World.A_MOVE);
                } else {
                    w.doAction(World.A_TURN_RIGHT);
                    if(goal_is_adjacent && shoot){
                        w.doAction(World.A_SHOOT);
                        shoot = false;
                    }
                    w.doAction(World.A_MOVE);
                }
            }
        else if(nextY < currentY) {
                if(w.getDirection() == World.DIR_DOWN){
                    if(goal_is_adjacent && shoot){
                        w.doAction(World.A_SHOOT);
                        shoot = false;
                    }
                    w.doAction(World.A_MOVE);
                } else if (w.getDirection() == World.DIR_LEFT){
                    w.doAction(World.A_TURN_LEFT);
                    if(goal_is_adjacent && shoot){
                        w.doAction(World.A_SHOOT);
                        shoot = false;
                    }
                    w.doAction(World.A_MOVE);
                } else if (w.getDirection() == World.DIR_UP){
                    w.doAction(World.A_TURN_LEFT);
                    w.doAction(World.A_TURN_LEFT);
                    if(goal_is_adjacent && shoot){
                        w.doAction(World.A_SHOOT);
                        shoot = false;
                    }
                    w.doAction(World.A_MOVE);
                } else {
                    w.doAction(World.A_TURN_RIGHT);
                    if(goal_is_adjacent && shoot){
                        w.doAction(World.A_SHOOT);
                        shoot = false;
                    }
                    w.doAction(World.A_MOVE);
                }
            }
            if(w.getPlayerX() == next_visit[0] && w.getPlayerY() == next_visit[1]){
                on_path = false;
                dead_end[0] = 0;
                dead_end[1] = 0;
                System.out.println("I reached my goal [" + next_visit[0] + ", " + next_visit[1] + "].");
            }
    }
    
    // Adds the safe cells that are left to visit
    public void add_safe_spots(int x, int y)
    {
        add_neighborcell(x+1,y);
        add_neighborcell(x-1,y);
        add_neighborcell(x,y+1);
        add_neighborcell(x,y-1);
        int[] newCell = new int[] {x, y};
        for(int i = 0; i<safe_spots.size(); i++){
            if (Arrays.equals(newCell, safe_spots.get(i))){
                safe_spots.remove(i);
                break;
            }
        }
    }
    
    public void add_neighborcell(int x, int y)
    {
        int[] new_neighborcell = new int[]{x, y};
        if(w.isValidPosition(x, y) && w.isUnknown(x,y)){ 
            for(int[] cell: safe_spots){
                if (Arrays.equals(new_neighborcell, cell)){
                    return;
                }
            }
            safe_spots.add(new_neighborcell);
        }
    }
    
    // This function returns the safe cell closest to the agent.
    public int[] check_safe_spots(){
        double[] distance = new double[safe_spots.size()];
        int index = 0;
        for (int i = 0; i < safe_spots.size(); i++){
            distance[i] = calculate_distance(safe_spots.get(i)[0], safe_spots.get(i)[1]);
        }
        double minValue = Integer.MAX_VALUE; 
        for(int i=0; i<distance.length ; i++){ 
            if(distance[i] < minValue){ 
                minValue = distance[i]; 
                index = i;
            } 
        }
        return safe_spots.get(index);
    }  
    
    // This function is called when the agent feels a stench. It adjusts the worst_pos ArrayList.
    // Worst_pos contains the cells where a wumpus might be.
    public void set_stench(int x, int y){
        stenches.add(new int[] {x,y});
        if(stenches.size() == 1 && worst_pos.isEmpty()){
            if(w.isUnknown(x+1, y)){
                worst_pos.add(new int[] {x+1,y});
            }
            if(w.isUnknown(x-1, y)){
                worst_pos.add(new int[] {x-1,y});
            }
            if(w.isUnknown(x, y+1)){
                worst_pos.add(new int[] {x,y+1});
            }
            if(w.isUnknown(x, y-1)){
                worst_pos.add(new int[] {x,y-1});
            }
        } else if(worst_pos.size() > 1){
            for(int i = 0; i < worst_pos.size(); i++){
                if(worst_pos.get(i) == new int[] {x + 1, y}){
                    worst_pos = new ArrayList<int[]>();
                    worst_pos.add(new int[] {x,y});
                    break;
                } else if (worst_pos.get(i) == new int[] {x - 1, y}){
                    worst_pos = new ArrayList<int[]>();
                    worst_pos.add(new int[] {x,y});
                    break;
                } else if(worst_pos.get(i) == new int[] {x, y + 1}){
                    worst_pos = new ArrayList<int[]>();
                    worst_pos.add(new int[] {x,y});
                    break;
                } else if(worst_pos.get(i) == new int[] {x, y - 1}){
                    worst_pos = new ArrayList<int[]>();
                    worst_pos.add(new int[] {x,y});
                    break;
                }
            }
        }   
        
    }
    
    public double calculate_distance(int newX, int newY){
        int currentX = w.getPlayerX();
        int currentY = w.getPlayerY();
        double distance = Math.abs(currentX-newX)+Math.abs(currentY-newY);
        return distance;
    }
    
}

