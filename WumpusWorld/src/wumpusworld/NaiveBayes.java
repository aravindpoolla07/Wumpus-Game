/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wumpusworld;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * 
 */
public class NaiveBayes {
    private ArrayList<int[]> neighbor_cells = new ArrayList<int[]>();
    private ArrayList<int[]> pit_neighbors = new ArrayList<int[]>();
    private ArrayList<double[]> probabilities = new ArrayList<double[]>();
    private ArrayList<ArrayList<int[]>> combinations = new ArrayList<ArrayList<int[]>>();
    private ArrayList<int[]> breezes = new ArrayList<int[]>();
    private ArrayList<int[]> founded_pits = new ArrayList<int[]>();
    private ArrayList<int[]> worst_pos = new ArrayList<int[]>();
    private ArrayList<int[]> best_cells = new ArrayList<int[]>();
    private ArrayList<int[]> combs = new ArrayList<int[]>();

    private World world;
    private double visitedCells;
    
    //probabilities for pits
    private double pit_prob = 0.2;
    
    public NaiveBayes(World world){
        this.world = world;
        find_breezes();
    }
    
    //for every new cell, we are adding all adjacent cells to our array neighbor_cells
    //and remove the new cell from the array. The neighbor_cells ArrayList is needed for
    //the calculation of the combinations
    public void add_position(int x, int y)
    {
        boolean pit_neighbor = false;
        if(world.hasPit(x,y)){
            pit_neighbor = true;
        }
        visitedCells++;
        add_neighborcell(x+1,y, pit_neighbor);
        add_neighborcell(x-1,y, pit_neighbor);
        add_neighborcell(x,y+1, pit_neighbor);
        add_neighborcell(x,y-1, pit_neighbor);
        for(int i = 0; i<neighbor_cells.size(); i++){
            if (neighbor_cells.get(i)[0] == x && neighbor_cells.get(i)[1] == y){
                neighbor_cells.remove(i);
                break;
            }
        }
    }
    
    public void add_neighborcell(int x, int y, boolean pit_neighbor)
    {
        int[] new_neighborcell;
        if(pit_neighbor){
            new_neighborcell = new int[]{x, y, 2};
        } else {
            new_neighborcell = new int[]{x, y, 0};
        }
        if(world.isValidPosition(x, y) && world.isUnknown(x,y)){ 
            for(int i = 0; i < neighbor_cells.size(); i++){
                if(neighbor_cells.get(i)[0] == x && neighbor_cells.get(i)[1] == y){
                    if(pit_neighbor){
                        neighbor_cells.get(i)[2] = 2;
                    }
                    return;
                }
            }
            neighbor_cells.add(new_neighborcell);
            probabilities.add(new double[]{0,0});
        }
    }
    
    //this function iterates over the world and for every known cell that contains
    //a breeze, it adds these coordinates to the breezes ArrayList
    //the breezes-ArrayList is needed to check if a combination is valid
    public void find_breezes(){
        int worldSize = world.getSize();
        for (int i = 1; i <= worldSize; i++) {
            for (int j = 1; j <= worldSize; j++)
            {
                if(!world.isUnknown(i,j)&&world.hasBreeze(i,j)){
                    breezes.add(new int[] {i,j});
                } 
            }
        }
    }
    
    //this function is called from the agent, if it needs the calculation of the
    //next cell. The function returns the cell(s) with the best probability.
    //It only returns a cell, if it can't contain a wumpus and if the agent doesn't
    //have to pass a pit to reach it.
    public ArrayList<int[]> get_best_cell(ArrayList<int[]> worst_pos){
        this.worst_pos = worst_pos;
        double founded_pit = 0;
        for (int x = 1; x <= world.getSize(); x++) {
            for (int y = 1; y <= world.getSize(); y++)
            {
                if(!world.isUnknown(x,y)&&world.hasPit(x,y)){
                    founded_pit += 1;
                }
            }
        }
        pit_prob = (3-founded_pit)/(15-visitedCells);
        probabilities = new ArrayList<double[]>();
        find_breezes();
        getCombinations();
        calculateProbabilities();
        
        double best_prob = Integer.MIN_VALUE;
        int next_visit = 0;
        best_cells = new ArrayList<int[]>();
        if(pit_neighbors.size() == 1){
            best_cells.add(pit_neighbors.get(0));
        } else {
            for (int i = 0; i < probabilities.size(); i++){
                if (probabilities.get(i)[1] == best_prob){
                    int[] best_pos = new int[] {pit_neighbors.get(i)[0], pit_neighbors.get(i)[1]};
                    boolean safe_cell = true;
                    if(pit_neighbors.get(i)[2] == 2){
                        safe_cell = false;
                    }
                    for(int[] wump_pos: worst_pos){
                        if(Arrays.equals(wump_pos, best_pos)|| pit_neighbors.get(i)[2] == 2){
                            safe_cell = false;
                        }
                    }
                    if(safe_cell){
                        best_prob = probabilities.get(i)[1];
                        next_visit = i;
                        best_cells.add(pit_neighbors.get(next_visit));
                    }
                } else if(probabilities.get(i)[1] > best_prob){
                    best_cells = new ArrayList<int[]>();
                    int[] best_pos = new int[] {pit_neighbors.get(i)[0], pit_neighbors.get(i)[1]};
                    boolean safe_cell = true;
                    if(pit_neighbors.get(i)[2] == 2){
                        safe_cell = false;
                    }
                    if(worst_pos.size() > 1){
                        for(int[] wump_pos: worst_pos){
                            if(Arrays.equals(wump_pos, best_pos)){
                                safe_cell = false;
                            }
                        }
                    }
                    if(safe_cell){
                        best_prob = probabilities.get(i)[1];
                        next_visit = i;
                        best_cells.add(pit_neighbors.get(next_visit));
                    }
                }
            }
            if(best_cells.size() == 0){
                if(pit_neighbors.size() == 0){
                    for(int[] neighbor: neighbor_cells){
                        best_cells.add(neighbor);
                    }
                } 
                else 
                {
                    next_visit = 0;
                    for(int j = 0; j < pit_neighbors.size(); j++){
                        boolean safe_cell = true;
                        for(int[] wump_pos: worst_pos){
                            if(Arrays.equals(wump_pos, new int[] {pit_neighbors.get(j)[0], pit_neighbors.get(j)[1]})){
                                safe_cell = false;
                            }
                        }
                        if(safe_cell){
                            next_visit = j;
                            break;
                        }           
                    }
                    best_cells.add(pit_neighbors.get(next_visit));
                }
            }
        }
        return best_cells;
    }
    
    // The function assigns the combinations-ArrayList to all possible pit-combination.
    // It whereby excludes the cells where a pit is for sure.
    public void getCombinations(){
        founded_pits = new ArrayList<int[]>();
        combinations = new ArrayList<ArrayList<int[]>>();
        int size = neighbor_cells.size();
        int diff_comb = (int) Math.pow(2, size);
        combs = new ArrayList<int[]>();
        getBitCombs(size, new int[size], 0);
        for (int i = 0; i < diff_comb; i++){
            ArrayList<int[]> clonedNeighbors = cloneArrayList(neighbor_cells);
            for (int j = 0; j < size; j++){
                clonedNeighbors.get(j)[2] = combs.get(i)[j];
            }
            if(validPitComb(clonedNeighbors)){
                combinations.add(clonedNeighbors);
            }
        }
        check_founded_pits();
        for (int k = 0; k < combinations.size(); k++){
            System.out.println("Combination " + (k+1));
            for(int h = 0; h < combinations.get(k).size();h++){
                System.out.print(Arrays.toString(combinations.get(k).get(h)));
            }
            System.out.println("");
        }
    }

    //The function returns if a combination is valid or not.
    public boolean validPitComb(ArrayList<int[]> combination){
        int pits = 0;
        for (int i=0; i < combination.size(); i++){
            if (combination.get(i)[2] == 1){
                pits++;
                int x = combination.get(i)[0];
                int y = combination.get(i)[1];
                if(!(world.isUnknown(x + 1, y) || !world.isValidPosition(x + 1, y) || world.hasBreeze(x + 1, y))){
                    return false;
                } else if (!(world.isUnknown(x - 1, y)|| !world.isValidPosition(x - 1, y) || world.hasBreeze(x - 1, y))){
                    return false;
                } else if (!(world.isUnknown(x , y + 1)|| !world.isValidPosition(x, y + 1) || world.hasBreeze(x, y + 1))){
                    return false;
                } else if (!(world.isUnknown(x, y - 1)|| !world.isValidPosition(x, y - 1) || world.hasBreeze(x, y - 1))){
                    return false;
                }
            }
            if(pits + founded_pits.size() > 3){
                return false;
            }
        }
        for(int i = 0; i < breezes.size(); i++){
            boolean breeze_has_pit = false;
            int x = breezes.get(i)[0];
            int y = breezes.get(i)[1];
            if(world.isValidPosition(x + 1, y)){
                if(world.isUnknown(x + 1, y)){
                    int[] coords = new int[] {x + 1, y, 1};
                    for(int[] cells: combination){
                        if (Arrays.equals(coords, cells)){
                            breeze_has_pit = true;
                        }
                    }
                    int[] has_pit = new int[] {x + 1, y};
                    for(int[] pit_cell: founded_pits){
                        if(Arrays.equals(has_pit, pit_cell)){
                            breeze_has_pit = true;
                        }
                    }
                } else if(world.hasPit(x + 1, y)){
                    breeze_has_pit = true;
                }
            }
            if(world.isValidPosition(x - 1, y)){
                if(world.isUnknown(x - 1, y)){
                    int[] coords = new int[] {x - 1, y, 1};
                    for(int[] cells: combination){
                        if (Arrays.equals(coords, cells)){
                            breeze_has_pit = true;
                        }
                    }
                    int[] has_pit = new int[] {x - 1, y};
                    for(int[] pit_cell: founded_pits){
                        if(Arrays.equals(has_pit, pit_cell)){
                            breeze_has_pit = true;
                        }
                    }
                } else if(world.hasPit(x - 1, y)){
                    breeze_has_pit = true;
                }
            }
            if(world.isValidPosition(x, y + 1)){
                if(world.isUnknown(x, y + 1)){
                    int[] coords = new int[] {x, y + 1, 1};
                    for(int[] cells: combination){
                        if (Arrays.equals(coords, cells)){
                            breeze_has_pit = true;
                        }
                    }
                    int[] has_pit = new int[] {x, y + 1};
                    for(int[] pit_cell: founded_pits){
                        if(Arrays.equals(has_pit, pit_cell)){
                            breeze_has_pit = true;
                        }
                    }
                } else if(world.hasPit(x, y + 1)){
                    breeze_has_pit = true;
                }
            }
            if(world.isValidPosition(x, y - 1)){
                if(world.isUnknown(x, y - 1)){
                    int[] coords = new int[] {x, y - 1, 1};
                    for(int[] cells: combination){
                        if (Arrays.equals(coords, cells)){
                            breeze_has_pit = true;
                        }
                    }
                    int[] has_pit = new int[] {x, y - 1};
                    for(int[] pit_cell: founded_pits){
                        if(Arrays.equals(has_pit, pit_cell)){
                            breeze_has_pit = true;
                        }
                    }
                } else if(world.hasPit(x, y - 1)){
                    breeze_has_pit = true;
                }
            }
            if(!breeze_has_pit){
                return false;
            }
        }
        return true;
    }
    
    //This function assigns the combs-ArrayList to all possible Combination of "there is a pit"(1) 
    // and "there is not pit"(0) as bit-combinations.
    public void getBitCombs(int amountNeighbors, int[] bitCombs, int iter){
        if(iter == amountNeighbors){
            int[] combCopy = new int[amountNeighbors];
            for(int i = 0; i < amountNeighbors; i++){
                combCopy[i] = bitCombs[i];
            }
            combs.add(combCopy);
            return;
        }
        bitCombs[iter] = 0;
        getBitCombs(amountNeighbors, bitCombs, iter+1);
        bitCombs[iter] = 1;
        getBitCombs(amountNeighbors, bitCombs, iter+1);
    }

//    0 = x-value
//    1 = y-value
//    2 = true/false if pit
    // This function is calculating the probabilities for each adjacent cell with the naive bayes algorithm.
    // The probabilities are saved in the probabilities-ArrayList
    public void calculateProbabilities(){
        for (int i = 0; i < pit_neighbors.size(); i++){
            double pPos = 0;
            double pNeg = 0;
            for (int j = 0; j < combinations.size(); j++){
                int amount_pit = 0;
                int amount_not_pit = 0;
                for(int k = 0; k < combinations.get(j).size(); k++){
                    if(k!=i){
                        if(combinations.get(j).get(k)[2] == 1){
                            amount_pit++;
                        } else{
                            amount_not_pit++;
                        }
                    }
                }
                if(combinations.get(j).get(i)[2] == 1){
                   pPos += Math.pow(pit_prob,amount_pit) * Math.pow(1-pit_prob,amount_not_pit);
                } else {
                   pNeg += Math.pow(pit_prob,amount_pit) * Math.pow(1-pit_prob,amount_not_pit);
                }
            }
            double has_pit_p = pit_prob * pPos;
            double has_no_pit_p = (1-pit_prob) * pNeg;
            probabilities.add(i, new double[] {has_pit_p, has_no_pit_p});  
//            System.out.println("pPos: " + pPos);
//            System.out.println("pNeg: " + pNeg);
//            System.out.println("has_pit_p: " + has_pit_p);
//            System.out.println("has_no_pit_p: " + has_no_pit_p);
//            System.out.println("pit_prob: " + pit_prob);
        }
    }
    
    //this function helps to find the pits we are sure about: if a cell contains
    //in all possible combinations a pit, there must be a pit.
    public void check_founded_pits(){
        founded_pits = new ArrayList<int[]>();
        pit_neighbors = cloneArrayList(neighbor_cells);
        boolean always_a_pit = true;
        for(int i = 0; i < pit_neighbors.size(); i++){
            for(ArrayList<int[]> combination: combinations){
                if(combination.get(i)[2] == 0){
                    always_a_pit = false;
                }
            }
            if (always_a_pit){
                founded_pits.add(combinations.get(0).get(i));
                pit_neighbors.remove(i);
                for(ArrayList<int[]> combination: combinations){
                    combination.remove(i);
                }
                i--;
            }
        always_a_pit = true;
        }
    }
    
    public ArrayList<int[]> cloneArrayList(ArrayList<int[]> list) {
        ArrayList<int[]> clonedArrayList = new ArrayList<int[]>();
        for (int[] item : list) clonedArrayList.add(item.clone());
        return clonedArrayList;
    }
}
