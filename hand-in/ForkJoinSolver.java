package amazed.solver;

import amazed.maze.Maze;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentSkipListSet;

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
    static volatile boolean found = false;
    int steps = 0;

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
        frontier.add(start);
        boolean player_has_been_created = false;
        Integer player = null;
        while (!frontier.empty() && !found) {
            int current = frontier.pop();
            synchronized (visited) {                 // lock visited, then read, then write
                if (!visited.contains(current)) {
                    visited.add(current);
                } else {
                    continue;                       // current is in visited, go back to while(!frontier....)
                }
            }                                       // unlock visited
            if (!player_has_been_created) {	    // if its the first time this thread is in this part of the code,
                player = maze.newPlayer(current);   // make new player model in gui
                player_has_been_created = true;
            }
            steps++;
            maze.move(player, current);

            if (maze.hasGoal(current)) {	    // if goal is found, trace back steps from the
                found = true;			    // starting-point of the thread that found the goal
                int i = start;			    // until we reach the initial starting-point of
                while (predecessor.get(i) != null) {// the first thread that was created
                    i = predecessor.get(i);
                }
                return pathFromTo(i, current);
            }

            for (int nb : maze.neighbors(current)) { 
                synchronized (visited) {	    // lock when we read from visited
                    if (!visited.contains(nb)) {
                        predecessor.put(nb, current);
                        frontier.push(nb);
                    }
                }			  	    // unlock
            }

            if (frontier.size() >= 2 && steps > forkAfter) { 	// if there are atleast 2 paths to choose from, create paths-1 children
                for (int i = 0; i < frontier.size() - 1; i++) {	// then continue along the path which dosnt have a child in it
                    ForkJoinSolver child = new ForkJoinSolver(maze, forkAfter);
                    child.start = this.frontier.pop();
                    child.predecessor = new HashMap<Integer, Integer>(predecessor); // give copy of the parents predecessor-map to the child
                    child.fork();
                    tasks.add(child);
                }
                steps = 0;
            }
        }
        for (ForkJoinSolver task: tasks) {			// wait for the children to return. if they have a result, 
            List<Integer> result = task.join();			// keep sending it "up" until we reach initial thread,
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
