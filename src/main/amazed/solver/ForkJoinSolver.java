package amazed.solver;

import amazed.maze.Maze;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.*;
/**
 * <code>ForkJoinSolver</code> implements a solver for
 * <code>Maze</code> objects using a fork/join multi-thread
 * depth-first search.
 * <p>
 * Instances of <code>ForkJoinSolver</code> should be run by a
 * <code>ForkJoinPool</code> object.
 */


public class ForkJoinSolver
    extends SequentialSolver
{

    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal.
     *
     * @param maze   the maze to be searched
     */

    static Set<Integer> visited = new HashSet<Integer>();
    static boolean found = false;

    public ForkJoinSolver(Maze maze)
    {
        super(maze);
    }

    /**
     * Creates a solver that searches in <code>maze</code> from the
     * start node to a goal, forking after a given number of visited
     * nodes.
     *
     * @param maze        the maze to be searched
     * @param forkAfter   the number of steps (visited nodes) after
     *                    which a parallel task is forked; if
     *                    <code>forkAfter &lt;= 0</code> the solver never
     *                    forks new tasks
     */


    public ForkJoinSolver(Maze maze, int forkAfter)
    {
        this(maze);
        this.forkAfter = forkAfter;
    }

    /**
     * Searches for and returns the path, as a list of node
     * identifiers, that goes from the start node to a goal node in
     * the maze. If such a path cannot be found (because there are no
     * goals, or all goals are unreacheable), the method returns
     * <code>null</code>.
     *
     * @return   the list of node identifiers from the start node to a
     *           goal node in the maze; <code>null</code> if such a path cannot
     *           be found.
     */

    

    @Override
    public List<Integer> compute()
    {
        return parallelSearch();        
    }

    /*
    Should utilize java's fork/join. Add parallelism to the sequential depth-first search
    Return the path if there is one, otherwise null
    */
    private List<Integer> parallelSearch()
    {
        ArrayList<ForkJoinSolver> tasks = new ArrayList<ForkJoinSolver>();
        // one player active on the maze at start
        int player = maze.newPlayer(start);
        // start with start node
        frontier.push(start);
        // as long as not all nodes have been processed
        while (!frontier.empty() && !found) {
            // get the new node to process

            int current = frontier.pop();
            // if current node has a goal
            if (maze.hasGoal(current)) {
                // move player to goal
                maze.move(player, current);
                // search finished: reconstruct and return path
                found=true;
                return pathFromTo(start, current);
            }
            // if current node has not been visited yet
            if (!visited.contains(current)) {
                // move player to current node
                maze.move(player, current);
                // mark node as visited
                writeToVisited(current);
                //visited.add(current);
                // for every node nb adjacent to current
                for (int nb: maze.neighbors(current)) {
                    // add nb to the nodes to be processed
                    
                    //frontier.push(nb);
                    // if nb has not been already visited,
                    // nb can be reached from current (i.e., current is nb's predecessor)
                    if (!visited.contains(nb)) { 
                        predecessor.put(nb, current);
                        frontier.push(nb);
                    }
                }  

            }

            //if there are more than 2 nodes in the frontier, split the tasks
            //else continue
            if (frontier.size() >= 2) {
                /*
                    When give this.maze, new for has access to current state which is needed to compute the full path:
                        - predecessors, to remember the path it has taken to get to the current node
                        - visited, should be global so all threads know what nodes have been visited
                        - start, a new start value, so it knows where to start
                */

                for (int i = 0; i < frontier.size() - 1; i++) {
                    ForkJoinSolver newThread = new ForkJoinSolver(maze);
                    int new_start = this.frontier.pop();
                    newThread.start = new_start;
                    newThread.predecessor = this.predecessor;
                    newThread.fork();
                    tasks.add(newThread);
                }

            }
        }
        for (ForkJoinSolver task: tasks) {
            List<Integer> result = task.join(); // extract results
            if (result != null) {
                return pathFromTo(start, result.get(result.size()-1));
            }
        }
        // all nodes explored, no goal found
        return null;
    }


    private synchronized void writeToVisited(int node) {
        visited.add(node);
    }

}
