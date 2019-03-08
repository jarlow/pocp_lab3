package amazed.solver;

import amazed.maze.Maze;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ConcurrentHashMap;
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

   // static ReentrantReadWriteLock visited_lock = new ReentrantReadWriteLock(true);
    //static volatile ConcurrentSkipListSet<Integer> visited = new ConcurrentSkipListSet<Integer>();
    //static Set<Integer> visited = Collections.synchronizedSet(new HashSet<Integer>());
    static Set<Integer> visited = new HashSet<Integer>();
    static ConcurrentHashMap<Integer,Integer> predecessor = new ConcurrentHashMap<Integer,Integer>();
    static volatile boolean found = false;
    //static Map<Integer,Integer> wantToVisit = Collections.synchronizedMap(new HashMap<>());
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
        int player = 99999;
        while (!frontier.empty() && !found) {
            int current = frontier.pop();
            synchronized(visited) {
                if (!visited.contains(current)) {
                    visited.add(current);
                } else {
                    continue;
                }
            }

            if (!player_has_been_created) {
                player = maze.newPlayer(current);
                player_has_been_created = true;
            }
            steps++;
            maze.move(player, current);

            if (maze.hasGoal(current)) {
                found=true;
                int counter = 1;
                int i = current;
                List<Integer> path = new LinkedList<>();
                while (predecessor.get(i) != null) {
                    path.add(i);
                    i = predecessor.get(i);
                    counter += 1;
                }
                path.add(i);
                Collections.reverse(path);
                System.out.println(" This is i : " + i );
                System.out.println("We are here: " + i + " counter: " + counter);
                return path;
            }

            for (int nb : maze.neighbors(current)) {
                synchronized (visited) {
                    if (!visited.contains(nb)) {
                        predecessor.put(nb, current);
                        frontier.push(nb);
                    }
                }
            }

            if (frontier.size() >= 2 && steps > forkAfter ){
                for (int i = 0; i < frontier.size() - 1; i++) {
                    int new_start = this.frontier.pop();
                    ForkJoinSolver newThread = new ForkJoinSolver(maze, forkAfter);
                    newThread.start = new_start;
                    newThread.fork();
                    tasks.add(newThread);
                    steps=0;
                }
            }

        }
        for (ForkJoinSolver task: tasks) {
            List<Integer> result = task.join();
            if (result != null) {
                System.out.println("This is result size: " +result.size());
                return result;
            }
        }
        if (frontier.size()!=0){
            System.out.println("Found: " + found);
            System.out.println("This is frontier size() " + frontier.size());
        }
        return null;
    }
}