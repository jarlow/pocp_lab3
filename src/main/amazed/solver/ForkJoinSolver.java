package amazed.solver;

import amazed.maze.Maze;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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

    static ReentrantReadWriteLock visited_lock = new ReentrantReadWriteLock();
    static volatile Set<Integer> visited = new HashSet<Integer>();
    static boolean found = false;
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

    
    private synchronized void incrementHash(Map<Integer,Integer> map, Integer key ){
        map.putIfAbsent(key,0);
        map.put(key,map.get(key)+1);
    }
    private synchronized void decrementHash(Map<Integer,Integer> map, Integer key ){
        map.putIfAbsent(key,0);
        map.put(key,map.get(key)-1);
    }

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
        boolean just_started = true;
        visited_lock.readLock().lock();
        if (visited.contains(start)) {
            visited_lock.readLock().unlock();
            return null;
        }else{
            visited_lock.readLock().unlock();
            visited_lock.writeLock().lock();
            visited.add(start);
            visited_lock.writeLock().unlock();
            
        }
        ArrayList<ForkJoinSolver> tasks = new ArrayList<ForkJoinSolver>();
        // one player active on the maze at start
        int player = maze.newPlayer(start);
        
        // start with start node
        frontier.push(start);
        // as long as not all nodes have been processed
        while (!frontier.empty() && !found) {
            visited_lock.writeLock().lock();
            // get the new node to process
            int current = frontier.pop();
            visited.add(current);
            visited_lock.writeLock().unlock();
            // visited_lock.writeLock().lock();
            // visited.add(current);
            // visited_lock.writeLock().unlock();

            // if current node has a goal
            if (maze.hasGoal(current)) {
                // move player to goal
                maze.move(player, current);
                // search finished: reconstruct and return path
                found=true;

                int i = predecessor.get(start);
                while (true) {
                    try {
                        i = predecessor.get(i);
                    }catch(Exception e) {
                        break;
                    }
                }
                return pathFromTo(i, current);
            }
                // if current node has not been visited yet
                // move player to current node
                steps++;
                maze.move(player, current);
                // mark node as visited
                // visited_lock.writeLock().lock();
                // visited.add(current);
                // visited_lock.writeLock().unlock();
                // for every node nb adjacent to current
                for (int nb : maze.neighbors(current)) {
                    // add nb to the nodes to be processed

                    //frontier.push(nb);
                    // if nb has not been already visited,
                    // nb can be reached from current (i.e., current is nb's predecessor)
                    visited_lock.readLock().lock();
                    if (!visited.contains(nb)) {
                        predecessor.put(nb, current);
                        frontier.push(nb);
                    }
                    visited_lock.readLock().unlock();
                }
                

            //if there are more than 2 nodes in the frontier, split the tasks
            //else continue
            if (frontier.size() >= 2 && steps > forkAfter ){//&& (wantToVisit.get(current) == null || (wantToVisit.get(current) <= 1))) {
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
                //System.out.println("my child found something!");
                //List<Integer> path = pathFromTo(start, result.get(result.size()-1));
                //System.out.println(path);
                //return pathFromTo(start, result.get(result.size()-1));
                return result;
            }
        }
        // all nodes explored, no goal found
        return null;
    }


    // private HashSet handleVisited(int node, String action) {
        
    //     if (action.equals("write")) {
    //         visited.add(node);
    //         return new HashSet<Integer>();
    //     }
    //     else{
    //         return (HashSet)visited;
    //     }
    // }

}
