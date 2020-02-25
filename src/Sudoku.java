import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class Sudoku
{
    public static void main(String[] args)
    {
        SudokuGrid sudoku = new SudokuGenerator().Anneal(40+20/82d, 76+40/82d);
        System.out.println("Got this:");
        System.out.println(sudoku);
        System.out.println("non threaded time:");
        long start = System.currentTimeMillis();
        System.out.println(sudoku.GetRank() + "  " + (System.currentTimeMillis() - start)+" ms");
        boolean solved = sudoku.Solve();
        System.out.println("solved: " + solved);
    
        System.out.println("threaded version time:");
        start = System.currentTimeMillis();
        System.out.println(sudoku.GetRankThreaded() + "  " + (System.currentTimeMillis() - start)+" ms");
        System.out.println(sudoku);
    }
    public Sudoku()
    {
        SudokuGrid sudokuGrid = new SudokuGrid();
    
        var start2 = System.currentTimeMillis();
        System.out.println(sudokuGrid.GetRank());
        System.out.println("non threaded version " + (System.currentTimeMillis() - start2));
    
        var start1 = System.currentTimeMillis();
        System.out.println(sudokuGrid.GetRankThreaded());
        System.out.println("threaded time: " + (System.currentTimeMillis() - start1));
    }
    
    
}



/* the grid is NOT 0 based, the index for GridPosition is from 1 to 9 */
class SudokuGrid
{
    @Override
    // pretty prints the sudoku game grid
    public String toString()
    {
        /* pretty prints the sudoku grid */
        
        LinkedList<String> rows = new LinkedList<>();
        for (int[] row: this.grid)
        {
            String prettyRow = "";
            for (int field: row)
            {
                prettyRow += " | " + field;
            }
            rows.add("-".repeat(prettyRow.length()+2));
            rows.add(prettyRow + " |");
        }
        rows.add(rows.get(0));
        
        String finalStr = "";
        for (String row: rows)
        {
            finalStr += row + "\n";
        }
        return finalStr;
    }
    
    /* returns a deep copied grid */
    public int[][] CopyGrid(int[][] grid)
    {
        int[][] copiedGrid = new int[9][9];
        for (int i = 0; i < 9; i++)
        {
            copiedGrid[i] = grid[i].clone();
        }
        return copiedGrid;
    }
    
    // call this in constructor
    private void InitCanFillFields()
    {
        for (int i = 1; i <= 9; i++)
        {
            for (int j = 1; j <= 9; j++)
            {
                GridPosition position = new GridPosition(i, j);
                if (GetFieldValue(position) == 0)
                    this.canFillFields.add(position);
            }
        }
        this.initialCanFillFields = new LinkedList<GridPosition>(this.canFillFields);
    }
    
    public int[] GetRow(int index)
    {
        return this.grid[index-1];
    }
    
    public int[] GetColumn(int index)
    {
        int[] column = new int[9];
        for (int i = 0; i < 9; i++)
        {
            column[i] = this.grid[i][index-1];
        }
        return column;
    }
    
    /* returns null if no next position available */
    public GridPosition GetNextPosition(GridPosition current)
    {
        assert current != null;  // this should never be null anyways
        GridPosition nextPosition = current.column < 9 ? new GridPosition(current.row, current.column+1) : (current.row == 9 ? null : new GridPosition(current.row+1, 1));
        
        if (nextPosition == null)
            return null;
        if (GetFieldValue(nextPosition) == 0)
            return nextPosition;
        return GetNextPosition(nextPosition);
    }
    
    /* will also modifies the field canFillFields */
    public void FillInGrid(int val, GridPosition position)
    {
        this.grid[position.row-1][position.column-1] = val;
        if (val == 0 && !this.canFillFields.contains(position))
            this.canFillFields.add(position);
        if (val != 0 && !this.canFillFields.contains(position))
            this.canFillFields.remove(position);
    }
    
    public int GetFieldValue(GridPosition position)
    {
        return this.grid[position.row-1][position.column-1];
    }
    
    // check if a given field with the given value is legal
    public boolean ValidateField(GridPosition position, int value)
    {
        boolean rowOK = IntStream.of(GetRow(position.row)).noneMatch(x -> value == x);
        boolean columnOK = IntStream.of(GetColumn(position.column)).noneMatch(x -> value == x);
        if (! (rowOK && columnOK))
            return false;
        
        ArrayList<Integer> subgrid = new ArrayList<>(9);

        int rowStart = 1 + ((position.row-1) / 3) * 3;
        int columnStart = 1 + ((position.column-1) / 3) * 3;
        for (int i = rowStart; i < rowStart + 3; i++)
        {
            for (int j = columnStart; j < columnStart + 3; j++)
            {
                subgrid.add(GetFieldValue(new GridPosition(i,j)));
            }
        }
        // boolean subgridOK = ! Arrays.stream((Integer[])subgrid.toArray()).anyMatch(x -> value == x);
        boolean subgridOK = true;
        for (int girdVal: subgrid)
        {
            if (girdVal == value)
            {
                subgridOK = false;
                break;
            }
        }
        
        return rowOK && columnOK && subgridOK;
    }
    
    // TODO maybe implement this to see if the whole grid is legal
    public boolean ValidateGrid(){return true;}
    
    
    // the grid representing the sudoku game itself
    public int[][] grid = new int[9][9];
    // all solved grids, i.e. different solutions
    public LinkedList<int[][]> listOfSolutions = new LinkedList<>();
    public LinkedList<GridPosition> canFillFields = new LinkedList<>();
    public LinkedList<GridPosition> initialCanFillFields = new LinkedList<>();
    
    public final int[][] originalGrid;  // the original game grid, never modified

    // given a grid 2d-array, create the SudokuGrid object for it
    public SudokuGrid(int[][] grid)
    {
        this.grid = CopyGrid(grid);
        this.originalGrid = CopyGrid(grid);
        
        InitCanFillFields();
    }
    
    // load from file
    public SudokuGrid(String fileName) throws IOException, URISyntaxException
    {
        var path1 = Sudoku.class.getClassLoader().getResource(fileName);
        var path = Path.of(path1.toURI());
        System.out.println(path);
        assert (Files.exists(path));
        // TODO throw assertion error
        this.grid = new int[9][9];

        List<String> lines = Files.readAllLines(path);
        assert (lines.size() == 9);  // make sure the file is at least in the correct form
        for (int i = 0; i < 9; i++)
        {
            String line = lines.get(i);
            line = line.trim();
            String[] nums = line.split(" ");
            assert (nums.length == 9);
            for (int j = 0; j < 9; j++)
            {
                this.grid[i][j] = Integer.parseInt(nums[j]);
            }
        }
        
        this.originalGrid = CopyGrid(this.grid);
        
        InitCanFillFields();
    }
    
    // just a relatively hard one to solve
    public SudokuGrid()
    {
        // TODO dispose this constructor, should never use this one
        // for now, just use this
        this.grid = new int[][]{{0, 0, 0, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {2, 0, 3, 0, 0, 0, 0, 0, 0}, {0, 0, 0, 5, 0, 0, 0, 0, 7}, {0, 0, 0, 0, 0, 0, 5, 3, 0}, {0, 0, 0, 0, 0, 0, 9, 4, 0}, {0, 0, 0, 0, 6, 0, 0, 2, 0}, {0, 0, 0, 0, 0, 0, 0, 0, 0}, {9, 7, 0, 2, 0, 0, 6, 0, 0}};
        
        this.originalGrid = CopyGrid(this.grid);
        InitCanFillFields();
    }
    
    /* returns the solved grid, OR null if unsolvable, basically just an entry point for SolveRecursively() */
    public boolean Solve()
    {
        // side effect will cause this.grid to be filled
        // int[][] solvedGrid = CopyGrid(this.grid);
        // TODO check what the desired behaviour is
        //this.grid = CopyGrid(originalGrid);
        return SolveRecursively(GetNextPosition(new GridPosition(1, 0)));
    }
    
    private boolean SolveRecursively(GridPosition currentPosition)
    {
        
        if (currentPosition == null)  // end of grid
            return true;
        
        for (int choice = 1; choice <= 9; choice++)  // each field can be 1  to 9
        {
            if (ValidateField(currentPosition, choice))
            {
                FillInGrid(choice, currentPosition);
                if (SolveRecursively(GetNextPosition(currentPosition)))
                    return true;
                
                // backtrack if the next call in recursion cannot find a valid choice
                FillInGrid(0, currentPosition);
            }
        }
        // exhausted all choices, thus returning false, i.e. no solution
        return false;
    }
    
    
    /* will add solved grid to the LinkedList listOfSolutions */
    private void SolveRecursiveAll(GridPosition currentPos)
    {
        // Instead of returning when one solution is found,
        // traverse the whole search space and backtrack whenever no solution is available
    
        // no more (next) position, even though the recursive call ensures that it won't be null,
        // there will be cases where there just isn't any next position from the beginning
        if (currentPos == null)
            return;
        
        for (int choice = 1; choice <= 9; choice++)
        {
            if (ValidateField(currentPos, choice))
            {
                FillInGrid(choice, currentPos);
                
                GridPosition nextPos = GetNextPosition(currentPos);
                if (nextPos != null)
                    SolveRecursiveAll(nextPos);
                else  // this is the last field in the grid
                {
                    this.listOfSolutions.add(CopyGrid(this.grid));
                    break;  // no other solution possible, this is the last field anyway
                }
                
            }
            // else select another choice, enter the loop with a different choice
        }
        // backtrack if the next call in recursion cannot find a valid choice
        FillInGrid(0, currentPos);
        return;  // return to the upper call
    }
    
    // solve recursively every next-step grid of current one with jobs, submit those jobs to threads, which invokes this method to solve (therefore recursively)
    private void SolveRecursiveAllThreaded(GridPosition currentPos, ExecutorService threads, List<Future<?>> futures, List<int[][]> solutionsList)
    {
        if (currentPos == null)
            return;
    
        // create a new SudokuGrid for every viable choice at current step, submit jobs to solve them
        for (int choice = 1; choice <= 9; choice++)
        {
//            if (ValidateField(currentPos, choice))
//            {
//                FillInGrid(choice, currentPos);
//
//                GridPosition nextPos = GetNextPosition(currentPos);
//                if (nextPos != null)
//                    SolveRecursiveAll(nextPos);
//                else  // this is the last field in the grid
//                {
//                    this.listOfSolutions.add(CopyGrid(this.grid));
//                    break;  // no other solution possible, this is the last field anyway
//                }
//
//            }
            if (ValidateField(currentPos, choice))
            {
                int[][] tempGrid = CopyGrid(this.grid);
                FillInGrid(choice, currentPos);
                if (GetNextPosition(currentPos) == null)  // fully filled, i.e. solved, add to solutionsList
                {
                    // adding to a list is not an atomic operation, must sync!!
                    synchronized(solutionsList)
                    {solutionsList.add(this.grid);}
                    return;
                }
                // create a new one to be solved (paralleled)
                SudokuGrid nextGrid = new SudokuGrid(this.grid);
                // TODO submit job for nextGrid
                Future<?> future = threads.submit(new SolveJob(nextGrid, threads, futures, solutionsList));
                synchronized(futures)
                {
                    futures.add(future);
                }
                this.grid = tempGrid;
            }
            // else select another choice, enter the loop with a different choice
        }
        // since everything is done in parallel, there is no backtracking, just stop, and return (this call dies)
        // FillInGrid(0, currentPos);
        return;  // return to the upper call
    }
    
    /* wrapper around SolveRecursiveAll(), this one is public, prints the number of solutions found */
    public void SolveAll()
    {
        SolveRecursiveAll(GetNextPosition(new GridPosition(1,0)));
        int numOfSolutions = this.listOfSolutions.size();
        System.out.println(String.format("Solved, there are %d solutions", numOfSolutions));
    }
    // TODO maybe implement this
    public void SolveAllThreaded(){}
    
    /* computes number of solutions there are by solving all from scratch */
    public int NumberOfSolutions()
    {
        this.listOfSolutions.clear();  // use clean list
        int[][] tempGrid = CopyGrid(this.grid);
        this.grid = CopyGrid(this.originalGrid);
        SolveRecursiveAll(GetNextPosition(new GridPosition(1,0)));
        this.grid = tempGrid;
        int n = this.listOfSolutions.size();
        this.listOfSolutions.clear();  // leave no side effect
        return n;
    }
    public int NumberOfSolutionsThreaded()
    {
        this.listOfSolutions.clear();  // use clean list
        int[][] tempGrid = CopyGrid(this.grid);
        this.grid = CopyGrid(this.originalGrid);
        ExecutorService threads = Executors.newFixedThreadPool(10);
        List<int[][]> solutionsList = new LinkedList<>();
        List<Future<?>> futures = new LinkedList<>();  // will use this to check if all jobs are finished
        SolveRecursiveAllThreaded(GetNextPosition(new GridPosition(1,0)), threads, futures, solutionsList);
        
        // check if all jobs have finished
        boolean allFinished;
        do
        {
            allFinished = true;
            
            synchronized(futures)
            {
                futures.removeIf(Future::isDone);
                for (var future : futures)
                {
                    boolean done = future.isDone();
                    if (!done)
                    {
                        allFinished = false;
                        break;
                    }
                    else  // this job is done, throw it away
                    {
                        // THIS CAUSES ConcurrentModificationException, DON'T DO THIS EVEN WHEN NOT THREADED
                        //futures.remove(future);
                    }
                }
            }
            
            try
            {
                Thread.sleep(10);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
    
        }
        while (!allFinished);
        // now, all are finished
        
        this.grid = tempGrid;
        int n = solutionsList.size();
        this.listOfSolutions.clear();  // leave no side effect
        return n;
    }
    
    /* the higher the rank value the worse it is, no solution is the worst */
    public double GetRank()
    {
        int numOfSolutions = NumberOfSolutions();
        if (numOfSolutions == 0)
            return Double.MAX_VALUE;  // worst rank
        
        // divide by 82 because it should be always less than 1, so as not to interfere with numOfSolutions
        return (numOfSolutions + (81-this.initialCanFillFields.size()) / 82d);
    }
    public double GetRankThreaded()
    {
        int num = NumberOfSolutionsThreaded();
        if (num == 0)
            return Double.MAX_VALUE;
        
        return (num + (81-this.initialCanFillFields.size()) / 82d);
    }
    
    
    /* the Runnable that will be submitted to ExecutorService when using threaded version (SolveRecursiveAllThreaded) */
    private static class SolveJob implements Runnable
    {
        SudokuGrid sudokuGrid;  ExecutorService threads;  List<Future<?>> futures;  List<int[][]> solutionsList;
        
        public SolveJob(SudokuGrid sudokuGrid, ExecutorService threads, List<Future<?>> futures, List<int[][]> solutionsList)
        {
            this.sudokuGrid = sudokuGrid;
            this.threads = threads;
            this.futures = futures;
            this.solutionsList = solutionsList;
        }
    
        @Override
        public void run()
        {
            this.sudokuGrid.SolveRecursiveAllThreaded(this.sudokuGrid.GetNextPosition(new GridPosition(1,0)), threads, futures, solutionsList);
        }
    }
    
}



class SudokuGenerator
{
    SudokuGrid game;
    
    public SudokuGenerator()
    {
        this.game = new SudokuGrid();
    }
    
    /* acceptance is used in the annealing process, to decide whether a new solution is to be accepted or not */
    public static double AcceptanceProbability(double oldCost, double newCost, double t)
    {
        if (oldCost > newCost)
            return 1;
        else
            return Math.exp((oldCost - newCost) / t);
    }
    
    // these two are used in the threaded version, using passed in sudokuGrid instead of this.game
    private void RandRmvField(SudokuGrid sudokuGrid)
    {
        ArrayList<GridPosition> filledFields = new ArrayList<>(81);
        for (int i = 1; i <= 9; i++)
        {
            for (int j = 1; j <= 9; j++)
            {
                GridPosition position = new GridPosition(i,j);
                if (!sudokuGrid.canFillFields.contains(position))
                    filledFields.add(position);
            }
        }
    
        int index = ThreadLocalRandom.current().nextInt(filledFields.size());
        GridPosition position = filledFields.get(index);
        sudokuGrid.FillInGrid(0, position);
        System.out.println(sudokuGrid);
    }
    private void RandAddField(SudokuGrid sudokuGrid)
    {
        LinkedList<GridPosition> canFillPositions = new LinkedList<>(sudokuGrid.canFillFields);
    
        while (canFillPositions.size() > 0)
        {
            int index = ThreadLocalRandom.current().nextInt(canFillPositions.size());
            GridPosition position = canFillPositions.get(index);
        
            ArrayList<Integer> valList = new ArrayList<>(9);
            for (int i = 1; i <= 9; i++)
            {
                valList.add(i);
            }
        
            boolean filled = false;
            while (valList.size() > 0)
            {
                //ArrayList<Integer> values = new ArrayList<>(valList);
                Integer valChoice = valList.get(ThreadLocalRandom.current().nextInt(valList.size()));
                if (sudokuGrid.ValidateField(position, valChoice))
                {
                    sudokuGrid.FillInGrid(valChoice, position);
                    filled = true;
                    break;
                }
                else
                    valList.remove(valChoice);
            }
        
            if (filled)
                break;
            else
                canFillPositions.remove(position);
        }
    }
    
    private void RandRmvField()
    {
        ArrayList<GridPosition> filledFields = new ArrayList<>(81);
        for (int i = 1; i <= 9; i++)
        {
            for (int j = 1; j <= 9; j++)
            {
                GridPosition position = new GridPosition(i,j);
                if (!this.game.canFillFields.contains(position))
                    filledFields.add(position);
            }
        }
        
        int index = ThreadLocalRandom.current().nextInt(filledFields.size());  // should never be of size 0 whenever this function is called
        GridPosition position = filledFields.get(index);
        this.game.FillInGrid(0, position);
        //this.game.canFillFields.add(position);  // don't need this, the FillInGrid method takes care of adding to it
        //filledFields.remove(index);  // no longer filled, doesn't matter, exiting function call
    }
    private void RandAddField()
    {
        LinkedList<GridPosition> canFillPositions = new LinkedList<>(this.game.canFillFields);
    
        while (canFillPositions.size() > 0)
        {
            int index = ThreadLocalRandom.current().nextInt(canFillPositions.size());
            GridPosition position = canFillPositions.get(index);
            
            ArrayList<Integer> valList = new ArrayList<>(9);
            for (int i = 1; i <= 9; i++)
            {
                valList.add(i);
            }
    
            boolean filled = false;
            while (valList.size() > 0)
            {
                //ArrayList<Integer> values = new ArrayList<>(valList);
                Integer valChoice = valList.get(ThreadLocalRandom.current().nextInt(valList.size()));
                if (this.game.ValidateField(position, valChoice))
                {
                    this.game.FillInGrid(valChoice, position);
                    filled = true;
                    break;
                }
                else
                    valList.remove(valChoice);
            }
    
            if (filled)
                break;
            else
                canFillPositions.remove(position);
        }
    }
    
    /* returns a neighbour solution, construct a new SudokuGrid object and return */
    public SudokuGrid GetNeighbourSolution()
    {
        int[][] currentSolution = this.game.CopyGrid(this.game.grid);
        if (this.game.canFillFields.size() == 0)
            RandRmvField();
        else if (this.game.canFillFields.size() == 81)
            RandAddField();
        else
        {
            if (ThreadLocalRandom.current().nextBoolean())  // 1/2 probability to remove or add a field
                RandAddField();
            else
                RandRmvField();
        }
    
        SudokuGrid newSolution = new SudokuGrid(this.game.grid);
        // restore, leave no side effect on the current grid
        this.game.grid = currentSolution;

        // in annealing process, use this new grid to find the new rank by calling its GetRank method
        return newSolution;
    }
    
    /* same as above, but used in threaded version */
    public SudokuGrid GetNeighbourSolution(SudokuGrid solution0)
    {
        // using a copy should be thread safe
        SudokuGrid solution = new SudokuGrid(solution0.grid);
        
        if (this.game.canFillFields.size() == 0)
            RandRmvField(solution);
        else if (this.game.canFillFields.size() == 81)
        {
            RandAddField();
            System.out.println("all empty when getting neighbour");
        }
        else
        {
            if (ThreadLocalRandom.current().nextBoolean())  // 1/2 probability to remove or add a field
                RandAddField(solution);
            else
                RandRmvField(solution);
        }
        return solution;
    }
    
    /* gets a fully filled grid for use in annealing */
    private SudokuGrid GetFullySolvedGrid()
    {
        LinkedList<int[][]> fullGridList = new LinkedList<>();
        // add solved grids here
        fullGridList.add(new int[][]{{9, 4, 7, 1, 6, 2, 3, 5, 8}, {6, 1, 3, 8, 5, 7, 9, 2, 4}, {8, 5, 2, 4, 9, 3, 1, 7, 6}, {1, 2, 9, 3, 8, 4, 5, 6, 7}, {5, 7, 8, 9, 2, 6, 4, 3, 1}, {3, 6, 4, 7, 1, 5, 2, 8, 9}, {2, 9, 1, 6, 3, 8, 7, 4,
        5}, {7, 8, 5, 2, 4, 1, 6, 9, 3}, {4, 3, 6, 5, 7, 9, 8, 1, 2}});
        // TODO add more here
        // fullGridList.add()
        
        SudokuGrid grid = new SudokuGrid(fullGridList.get(ThreadLocalRandom.current().nextInt(fullGridList.size())));
        return grid;
    }
    
    /**
     * the annealing process will create a whole lot of randomness, it doesn't really matter if the grids we start off
     * with are not that much random
     * @param rankStartValue the start of the range (of rank) for which a solution is expected to be in
     * @param rankEndValue the end of the range (of rank) for which a solution is expected to be in
     */
    public SudokuGrid Anneal(double rankStartValue, double rankEndValue)
    {
        if (rankStartValue > rankEndValue)
            throw new IllegalArgumentException("rank end value smaller than rank start value");
        
        this.game = GetFullySolvedGrid();
        double rank = Double.MAX_VALUE;
        double oldCost = Double.MAX_VALUE;
        double t = 1d;
        double t_min = 0.00001;
        double alpha = 0.9;
        
        while (t > t_min)
        {
            // 100 time annealing loop, at the same temperature t
            for (int i = 0; i < 100; i++)
            {
                SudokuGrid newSolution = GetNeighbourSolution();
                
                double newRank = newSolution.GetRank();
                // compute the cost i,e, the difference between actual rank and accepting rank range
                double newCost = Math.min(Math.abs(rankEndValue - newRank), Math.abs(rankStartValue - newRank));
                double acceptance = AcceptanceProbability(oldCost, newCost, t);
                if (ThreadLocalRandom.current().nextDouble() < acceptance)
                {
                    this.game = newSolution;  // accept the new SudokuGrid
                    rank = newRank;
                    oldCost = Math.min(Math.abs(rankEndValue - rank), Math.abs(rankStartValue - rank));
                }
                if (rankStartValue <= rank && rank <= rankEndValue)
                    return new SudokuGrid(this.game.CopyGrid(this.game.grid));
            }
            System.out.println(rank);
            t = alpha * t;  // cool down the temperature
            if (rankStartValue <= rank && rank <= rankEndValue)
                break;  // desired solution found! stop annealing
        }
        return new SudokuGrid(this.game.CopyGrid(this.game.grid));
    }
    
    public SudokuGrid AnnealThreaded(double rankStartValue, double rankEndValue)
    {
        if (rankStartValue > rankEndValue)
            throw new IllegalArgumentException("rank end value smaller than rank start value");
        
        this.game = GetFullySolvedGrid();
        double rank = Double.MAX_VALUE;
        double oldCost = Double.MAX_VALUE;
        double t = 1d;
        double t_min = 0.00001;
        double alpha = 0.9;
    
        while (t > t_min)
        {
            for (int i = 0; i < 100; i++)
            {
                SudokuGrid newSolution = GetNeighbourSolution();
        
                double newRank = newSolution.GetRankThreaded();
    
                double newCost = Math.min(Math.abs(rankEndValue - newRank), Math.abs(rankStartValue - newRank));
                double acceptance = AcceptanceProbability(oldCost, newCost, t);
                if (ThreadLocalRandom.current().nextDouble() < acceptance)
                {
                    this.game = newSolution;  // accept the new SudokuGrid
                    rank = newRank;
                    oldCost = Math.min(Math.abs(rankEndValue - rank), Math.abs(rankStartValue - rank));
                }
                if (rankStartValue <= rank && rank <= rankEndValue) //
                    return new SudokuGrid(this.game.CopyGrid(this.game.grid));
            }
            System.out.println(rank);
            t = alpha * t;  // cool down the temperature
            if (rankStartValue <= rank && rank <= rankEndValue)
                break;  // desired solution found! stop annealing
        }
        return new SudokuGrid(this.game.CopyGrid(this.game.grid));
    }
    
    
    // THE WRONG WAY OF DOING ANNEALING, THE ANNEALING PROCESS SHOULD NOT PARALLELIZE
    public SudokuGrid WrongAnnealThreaded(double rankStartValue, double rankEndValue)
    {
        // TODO implement multithreaded Anneal method
        var threads = Executors.newFixedThreadPool(10);

        this.game = GetFullySolvedGrid();
        double rank = Double.MAX_VALUE;
        double t = 1d;
        double t_min = 0.00001;
        double alpha = 0.9;

        while (t > t_min)
        {
            // 100 times annealing

            // create 100 jobs to get 100 neighbour solutions
            var jobs = new ArrayList<AnnealJob>(100);
            for (int i = 0; i < 100; i++)
            {
                // TODO submit a job, and handle its result
                jobs.add(new AnnealJob(this.game));
            }

            // process the results, after calling invokeAll()
            ArrayList<SudokuGrid> newSolutions = new ArrayList<>(100);
            ArrayList<Double> ranks = new ArrayList<>(100);

            try
            {
                List<Future<Object[]>> futures = threads.invokeAll(jobs);  // this blocks until all jobs are done

                for (Future<Object[]> future : futures)
                {
                    Object[] result = future.get();
                    newSolutions.add((SudokuGrid) result[0]);
                    ranks.add((double) result[1]);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            SudokuGrid solution = new SudokuGrid(this.game.grid);
            for (int i = 0; i < 100; i++)
            {
                double newRank = ranks.get(i);
                double acceptance = AcceptanceProbability(rank, newRank, t);
                if (ThreadLocalRandom.current().nextDouble() < acceptance)
                {
                    solution = newSolutions.get(i);
                    rank = newRank;
                }
                if (rankStartValue <= rank && rank <= rankEndValue)
                    return new SudokuGrid(solution.grid);
            }
            // if (solution != null)  // ensure not null
            this.game = solution;
            //System.out.println(solution);

            System.out.println(rank);
            t = alpha * t;  // cool down the temperature
            if (rankStartValue <= rank && rank <= rankEndValue)
                break;  // desired solution found! stop annealing
        }
        return new SudokuGrid(this.game.grid);
    }
    
    /* this job runs the 100 time annealing loop, Object[0] = SudokuGrid Object[1] = (Double) rank
    * this is used in WrongAnnealThreaded method */
    class AnnealJob implements Callable<Object[]>
    {
        SudokuGrid gameGrid;
        public AnnealJob(SudokuGrid sudokuGrid)
        {
            // TODO implement initializer
            this.gameGrid = new SudokuGrid(sudokuGrid.grid);
        }
    
        @Override
        public Object[] call()
        {
            // TODO
            SudokuGrid neighbourSolution = GetNeighbourSolution(this.gameGrid);
            Double rank = neighbourSolution.GetRank();
            return new Object[]{neighbourSolution, rank};
        }
    }
    
    /* The overhead of syncing when using multiple threads is always very significant, and from observation,
    * non threaded version has always been faster than the threaded version */
    public static void main1(String[] args)
    {
        SudokuGrid sudoku = new SudokuGenerator().Anneal(40+20/82d, 76+40/82d);
        System.out.println("Got this:");
        System.out.println(sudoku);
        System.out.println("non threaded time:");
        long start = System.currentTimeMillis();
        System.out.println(sudoku.GetRank() + "  " + (System.currentTimeMillis() - start)+" ms");
        boolean solved = sudoku.Solve();
        System.out.println("solved: " + solved);
        
        System.out.println("threaded version time:");
        start = System.currentTimeMillis();
        System.out.println(sudoku.GetRankThreaded() + "  " + (System.currentTimeMillis() - start)+" ms");
        System.out.println(sudoku);
    }
    
    /* Test AnnealThreaded() method */
    public static void main2(String[] args)
    {
        SudokuGrid sudoku = new SudokuGenerator().AnnealThreaded(1+20/82d, 1+23/82d);
        System.out.println("Got this:");
        System.out.println(sudoku);
        System.out.println("After Solving:");
        System.out.println(sudoku.NumberOfSolutions());
        boolean solved = sudoku.Solve();
        System.out.println("solved: " + solved);
        System.out.println(sudoku);
    }
    
    public static void main3(String[] args) throws IOException, URISyntaxException
    {
        SudokuGrid sudokuGrid = new SudokuGrid("check.txt");
        //System.out.println(sudokuGrid.NumberOfSolutions() + "  " + sudokuGrid.NumberOfSolutionsThreaded());
        
        var start2 = System.currentTimeMillis();
        System.out.println(sudokuGrid.GetRank());
        System.out.println("non threaded version " + (System.currentTimeMillis() - start2));
    
        var start1 = System.currentTimeMillis();
        System.out.println(sudokuGrid.GetRankThreaded());
        System.out.println("threaded time: " + (System.currentTimeMillis() - start1));
    }
    
}