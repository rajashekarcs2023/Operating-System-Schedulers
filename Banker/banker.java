import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class banker {
    public static void main(String[] args) throws FileNotFoundException {
        File file = new File(args[0]);
        Scanner input = new Scanner(file);
        //Master List to hold all the tasks from the input
        ArrayList<task> tasks = new ArrayList<>();
        //Original List not to be changed
        //For printing when an algorithm is done
        ArrayList<task> inputTasks = new ArrayList<>();
        //Set up tasks arraylist with file input
        setUp(input, tasks);
        //add all tasks to inputTasks
        for(task t: tasks) inputTasks.add(t);
        //Execute fifo algorithm
        fifo(tasks);
        input.close();
        System.out.printf("%15s\n","FIFO");
        printData(inputTasks);
        //Reset tasks for next algorithm
        resetTasks(inputTasks);

        inputTasks = new ArrayList<>();
        tasks = new ArrayList<>();
        input = new Scanner(file);
        setUp(input, tasks);
        for(task t: tasks) inputTasks.add(t);
        //Execute Banker algorithm
        bankerAlg(tasks);
        System.out.printf("\n%15s\n","BANKER");
        printData(inputTasks);

    }
    //Resets tasks for the next algorithm
    public static void resetTasks(ArrayList<task> inputTasks){
        for(task t: inputTasks){
            t.reset();
        }
    }

    //Print and format data for each algorithm
    public static void printData(ArrayList<task> inputTasks){
        int totalWaiting = 0;
        int totalTime = 0;
        for(task t: inputTasks){
            if(t.abort){
                System.out.printf("%-10s aborted\n" , "Task "+t.taskNumber);
            }
            else {
                totalTime += t.terminatedCycle;
                totalWaiting += t.waiting;
                System.out.printf("%-10s %-3s %-3s %-3s\n" , "Task "+ t.taskNumber, ""+t.terminatedCycle, ""+t.waiting, ""+(int)(((double) t.waiting/t.terminatedCycle)*100)+"%");
            }
        }
        System.out.printf("%-10s %-3s %-3s %-3s\n","Total",""+totalTime,""+totalWaiting, ""+(int)(((double) totalWaiting/totalTime)*100)+"%");
    }
    //Update waiting values if deadlock or in blocked queue
    public static void updateWaiting(ArrayList<task> blockedQ){
        for(task t: blockedQ){
            t.waiting++;
        }
    }

    //Check if tasks claim more than resources available in system
    public static void doBankerInitiate(ArrayList<task> tasks){
        ArrayList<task> toRemove = new ArrayList<>();
        for(task t: tasks){
            ArrayList<String[]> list = t.commands;
            String[] command = list.get(0);
            if(Integer.parseInt(list.get(0)[4]) > task.resources.get(Integer.parseInt(list.get(0)[3]))){
                t.abort = true;
                System.out.println("\nBANKER: Task: " + t.taskNumber + " is aborted (claim exceeds total in system)");
                toRemove.add(t);
            }
            else{
                int resourceType = Integer.parseInt(command[3]);
                int claim = Integer.parseInt(command[4]);
                t.initialClaim.put(resourceType, claim);
                list.remove(0);
           }
        }
        tasks.removeAll(toRemove);
    }

    //Check if tasks request more than originally claimed
    public static boolean requestCheck(task t, int request, int resourceType){
        if(t.initialClaim.get(resourceType) < t.currentResources.getOrDefault(resourceType, 0) + request){
            System.out.println("\nBANKER: Task: " + t.taskNumber + " is aborted (request exceeds claim)");
            t.abort = true;
            return false;
        }
        return true;
    }

    //Remove all tasks from tasks list if flagged as abort
    public static void removeAbort(ArrayList<task> tasks){
        ArrayList<task> toRemove = new ArrayList<>();
        for(task t: tasks){
            if(t.abort) {
                for(int i = 0; i < task.resourceTypes; i++){
                    int reclaim = t.currentResources.getOrDefault(i+1, 0);
                    task.resources.put(i+1, task.resources.get(i+1) + reclaim);
                }
                toRemove.add(t);
            }
        }
        tasks.removeAll(toRemove);

    }

    //Handle commands from input if release and request, or skip if acted already in blocked q
    public static void bankerRequestOrRelease(ArrayList<task> tasks, ArrayList<task> blockedQ){
        for(task t: tasks){
            if(t.queuedMove){
                t.queuedMove = false;
                continue;
            }
            ArrayList<String[]> list = t.commands;
            String[] command = list.get(0);
            if(command[0].equals("request")){
                //If delay, decrement by one and do nothing
                if(Integer.parseInt(command[2]) == 0)
                doBankerRequests(t, tasks, blockedQ);
                else command[2] = Integer.toString(Integer.parseInt(command[2]) - 1);
            }
            else if(command[0].equals("release")){
                //If delay, decrement by one and do nothing
                if(Integer.parseInt(command[2]) == 0)
                doRelease(t);
                else command[2] = Integer.toString(Integer.parseInt(command[2]) - 1);
            }

        }

    }

    //Iterate for all blocked tasks and attempt to execute requests
    public static void bankerBlockedRequests(ArrayList<task> blockedQ, ArrayList<task> tasks){
        for(task t: blockedQ){
            //Handle delays
            if(Integer.parseInt(t.commands.get(0)[2]) == 0)
            doBankerRequests(t, tasks, blockedQ);
            else t.commands.get(0)[2] = Integer.toString(Integer.parseInt(t.commands.get(0)[2])-1);
        }
    }

    //Implementation to check if granting request is safe
    public static boolean isSafe(ArrayList<task> tasks, task t, int request, int resourceType){
        if((task.resources.get(resourceType) < request)|| t.abort) return false;
        //Create replicas of current tasks to simulate if granting request is safe
        task.resourceType = resourceType;
        ArrayList<task> simTasks = new ArrayList<>();
        HashMap<Integer, Integer> simResources = new HashMap<>(task.resources);
        task curr = new task(t);
        simTasks.add(curr);
        for(task ta: tasks){
            if(!ta.abort && ta != t){
                simTasks.add(new task(ta));
            }
        }

        curr.currentResources.put(resourceType, curr.currentResources.getOrDefault(resourceType, 0) + request);
        simResources.put(resourceType, simResources.getOrDefault(resourceType, 0) - request);
        Collections.sort(simTasks);
        //Iterate through all replica tasks and simulate granting resources if available
        for(int i = 0; i < task.resourceTypes; i++){
            resourceType = i+1;
            for(task ta: simTasks){
                int maxAdd = ta.initialClaim.get(resourceType) - ta.currentResources.getOrDefault(resourceType, 0);
                int available = simResources.get(resourceType);
                if(maxAdd > available) {
                    //If deadlock, return false
                    return false;
                }
                simResources.put(resourceType, simResources.get(resourceType) + ta.currentResources.getOrDefault(resourceType, 0));
            }
        }
        //No deadlock, return true
        return true;
    }

    //Execute all banker requests
    public static void doBankerRequests(task t, ArrayList<task> tasks, ArrayList<task> blockedQ){
        HashMap<Integer, Integer> resources = task.resources;
        ArrayList<String[]> list = t.commands;
        String[] command = list.get(0);
        int resourceType = Integer.parseInt(command[3]);
        int request = Integer.parseInt(command[4]);
        //Check if request is grantable
        boolean legalRequest = requestCheck(t, request, resourceType);
        boolean safe = isSafe(tasks, t, request, resourceType);
        if(resources.get(resourceType) >= request && legalRequest && safe){
            //Fulfill the request
            t.currentResources.put(resourceType, t.currentResources.getOrDefault(resourceType, 0) + request);
            resources.put(resourceType, resources.get(resourceType)-request);
            list.remove(0);
            if(t.queued){
                t.queued = false;
                t.queuedMove = true;
            }
        }
        else if(!t.abort){
            //add to queue
            if(!t.queued) {
                blockedQ.add(t);
                t.queued = true;
            }
            else t.queuedMove = true;
        }

    }

    //Wrapper method for banker algorithm
    public static void bankerAlg(ArrayList<task> tasks){
        ArrayList<task> blockedQ = new ArrayList<>();
        
        while(!allTerminated(tasks)){
            if(hasInitiate(tasks)){
                doBankerInitiate(tasks);
            }
            else{
                bankerBlockedRequests(blockedQ, tasks);
                removeQ(blockedQ);
                bankerRequestOrRelease(tasks, blockedQ);
                updateWaiting(blockedQ);
            }
            //Remove tasks flagged as abort
            removeAbort(tasks);
            task.cycles++;
            //Add released resources to be available for next cycle
            addRelease();
            //Remove all tasks that are terminated
            terminated(tasks);
        }
    }


    //Wrapper method for FIFO implementation
    public static void fifo(ArrayList<task> tasks){
        ArrayList<task> blockedQ = new ArrayList<>();
        while(!allTerminated(tasks)){
            if(hasInitiate(tasks)){
                doInitiate(tasks);
            }
            //If Deadlocked, abort tasks until no longer deadlocked
            else if(isDeadlock(tasks)){
                while(isDeadlock(tasks)) abortLowest(tasks, blockedQ);
                updateWaiting(tasks);
            }
            else{
                blockedRequests(blockedQ);
                removeQ(blockedQ);
                requestOrRelease(tasks, blockedQ);
                updateWaiting(blockedQ);
            }
            task.cycles++;
            addRelease();
            terminated(tasks);
        }
    }

    //If task is not flagged to be queued, remove it from queue arraylist
    public static void removeQ(ArrayList<task> blockedQ){
        ArrayList<task> toRemove = new ArrayList<>();
        for(task t: blockedQ){
            if(!t.queued) toRemove.add(t);
        }
        blockedQ.removeAll(toRemove);
    }

    //Accumulate released resources to be added for the next cycle
    public static void addRelease(){
        for(int i = 0; i < task.resourceTypes; i++){
            task.resources.put(i+1, task.resources.get(i+1) + task.returnedResources.getOrDefault(i+1, 0));
            task.returnedResources.put(i+1,0);
        }
    }

    //Wrapper method for all queued tasks, attempt to execute requests if delay is 0
    public static void blockedRequests(ArrayList<task> blockedQ){
        for(task t: blockedQ){
            if(Integer.parseInt(t.commands.get(0)[2]) == 0)
            doRequests(t, blockedQ);
            else t.commands.get(0)[2] = Integer.toString(Integer.parseInt(t.commands.get(0)[2])-1);
        }
    }

    //Abort task with the lowest task number if in deadlock
    public static void abortLowest(ArrayList<task> tasks, ArrayList<task> blockedQ){
        int minTaskNumber = Integer.MAX_VALUE;
        task minTask = tasks.get(0);
        for(task t: tasks){
            if(minTaskNumber > t.taskNumber){
                minTaskNumber = Math.min(minTaskNumber, t.taskNumber);
                minTask = t;
            }

        }
        minTask.abort = true;
        for(int i = 0; i < task.resourceTypes; i++){
            int reclaim = minTask.currentResources.getOrDefault(i+1, 0);
            task.resources.put(i+1, task.resources.get(i+1) + reclaim);
        }
        tasks.remove(tasks.indexOf(minTask));
        if(blockedQ.contains(minTask)) blockedQ.remove(blockedQ.indexOf(minTask));
    }

    //Check if tasks are currently in a deadlock
    public static boolean isDeadlock(ArrayList<task> tasks){
        HashMap<Integer, Integer> resources = task.resources;
            for(task t: tasks){
                ArrayList<String[]> list = t.commands;
                String[] command = list.get(0);
                //If a task is not requesting resources, the state is not a deadlock
                if(!command[0].equals("request"))
                {
                    return false;
                }
                int resourceType = Integer.parseInt(command[3]);
                int request = Integer.parseInt(command[4]);
                //If a request is grantable, it is not a deadlock
                if(resources.get(resourceType) >= request){
                    return false;
                }
            }
            return true;
    }

    //Wrapper method to execute correct algorithm if command is request or release and handle delays
    public static void requestOrRelease(ArrayList<task> tasks, ArrayList<task> blockedQ){
        for(task t: tasks){
            if(t.queuedMove){
                t.queuedMove = false;
                continue;
            }
            ArrayList<String[]> list = t.commands;
            String[] command = list.get(0);
            if(command[0].equals("request")){
                if(Integer.parseInt(command[2]) == 0)
                doRequests(t, blockedQ);
                else command[2] = Integer.toString(Integer.parseInt(command[2]) - 1);
            }
            else if(command[0].equals("release")){
                if(Integer.parseInt(command[2]) == 0)
                doRelease(t);
                else command[2] = Integer.toString(Integer.parseInt(command[2]) - 1);
            }

        }
    }

    //If flagged terminated, set terminated cycles for the task and remove from task list
    public static void terminated(ArrayList<task> tasks){
        ArrayList<task> toRemove = new ArrayList<>();
        for(task t: tasks){
            if(t.commands.size() == 1 && Integer.parseInt(t.commands.get(0)[2]) == 0){
                toRemove.add(t);
                t.commands.remove(0);
                t.terminated = true;
                t.terminatedCycle = task.cycles;
            }
            else if(t.commands.size() == 1){
                int delay = Integer.parseInt(t.commands.get(0)[2]) - 1;
                t.commands.get(0)[2] = Integer.toString(delay);
            }
        }
        tasks.removeAll(toRemove);
    }

    //Add released resources to temp hashmap to be added for the next cycle;
    public static void doRelease(task t){
        ArrayList<String[]> list = t.commands;
        String[] command = list.get(0);
        int resourceType = Integer.parseInt(command[3]);
        int release = Integer.parseInt(command[4]);
        t.currentResources.put(resourceType, t.currentResources.get(resourceType) - release);
        task.returnedResources.put(resourceType, task.returnedResources.getOrDefault(resourceType, 0)+release);
        list.remove(0);
    }
    //FIFO requests execute code 
    public static void doRequests(task t, ArrayList<task> blockedQ){
        HashMap<Integer, Integer> resources = task.resources;
        ArrayList<String[]> list = t.commands;
        String[] command = list.get(0);
        int resourceType = Integer.parseInt(command[3]);
        int request = Integer.parseInt(command[4]);
        if(resources.get(resourceType) >= request){
            //Fulfill the request
            t.currentResources.put(resourceType, t.currentResources.getOrDefault(resourceType, 0) + request);
            resources.put(resourceType, resources.get(resourceType)-request);
            list.remove(0);
            if(t.queued){
                t.queued = false;
                t.queuedMove = true;
            }
        }
        else{
            if(!t.queued) {
                blockedQ.add(t);
                t.queued = true;
            }
            //add to queue
        }
    }
    //Remove commands for initiate 
    public static void doInitiate(ArrayList<task> tasks){
        for(task t: tasks){
            ArrayList<String[]> list = t.commands;
            list.remove(0);
        }
    }
    //If task has yet to initiate, return true
    public static boolean hasInitiate(ArrayList<task> tasks){
        for(task t: tasks){
            if((t.commands.get(0)[0].equals("initiate"))){
                return true;
            }
        }
        return false;
    }

    //Check if all tasks are terminated/aborted to end the algorithm
    public static boolean allTerminated(ArrayList<task> tasks){
        for(task t: tasks){
            if(!t.terminated) return false;
        }
        return true;
    }

    //Print method for debugging if termination cycle is correct
    public static void printTerminated(ArrayList<task> tasks){
        for(task t: tasks){
            System.out.println("Task number: " + t.taskNumber + " Finished at: " + t.terminatedCycle);
        }
    }

    //Print method for debugging if waiting is correct
    public static void printWaiting(ArrayList<task> tasks){
        for(task t: tasks){
            System.out.println("Task number: " + t.taskNumber + " Waiting at: " + t.waiting);
        }

    }

    //Setup arraylists to add all tasks from input
    public static void setUp(Scanner input, ArrayList<task> tasks){
        int numTasks = input.nextInt();
        int resourceTypes = input.nextInt();
        task.resourceTypes = resourceTypes;
        for(int i = 0; i < resourceTypes; i++){
            int totalResources = input.nextInt();
            task.resources.put(i+1, totalResources);
        }

        for(int i = 0; i < numTasks; i++){
            tasks.add(new task(i+1));
        }
        while(input.hasNext()){
            String[] line = new String[5];
            for(int i = 0; i < 5; i++){
                line[i] = input.next();
            }
            int taskNum = Integer.parseInt(line[1]);
            tasks.get(taskNum-1).commands.add(line);
        }

    }
}

class task implements Comparable<task>{
    int taskNumber;
    int terminatedCycle;
    int waiting;
    boolean terminated;
    boolean queued;
    boolean queuedMove;
    boolean abort;
    //List to hold all commands for specific task number
    //Commands: Initiate, request, release, terminate
    ArrayList<String[]> commands = new ArrayList<>();
    //Map to keep track of initial claim for each resource type
    HashMap<Integer,Integer> initialClaim = new HashMap<>();
    //Map to keep track of how many resources of each type a task currently has
    HashMap<Integer, Integer> currentResources = new HashMap<>();
    static int cycles = 0;
    static int resourceTypes;
    static int resourceType;
    //Map of total resources available in system
    static HashMap<Integer, Integer> resources = new HashMap<>();
    //Map of released resources to be added to total resources at the end of the cycle
    static HashMap<Integer, Integer> returnedResources = new HashMap<>(); 

    task(int taskNumber){
        this.taskNumber = taskNumber;
        this.waiting = 0;
    }

    //Constructor to create duplicates to check if current state is safe
    task(task t){
        this.taskNumber = t.taskNumber;
        this.initialClaim = new HashMap<>(t.initialClaim);
        this.currentResources = new HashMap<>(t.currentResources);
    }

    public String toString(){
        return Integer.toString(taskNumber);
    }
    
    public void printCommands(){
        for(int i = 0; i < commands.size(); i++){
            String line = "";
            for(int j = 0; j < commands.get(i).length; j++){
                line += commands.get(i)[j] + " ";
            }
            System.out.println(line);
        }
    }

    //To sort tasks by smallest amount needed to fulfill claim first
    public int compareTo(task o){
        return (this.initialClaim.get(task.resourceType) - this.currentResources.getOrDefault(task.resourceType, 0)) - 
        (o.initialClaim.get(task.resourceType) - o.currentResources.getOrDefault(task.resourceType, 0));
    }

    //Reset all tasks and static variables for next algorithm
    public void reset(){
        cycles = 0;
        resources = new HashMap<>();
        returnedResources = new HashMap<>();
        commands = new ArrayList<>();
        currentResources = new HashMap<>();
        initialClaim = new HashMap<>();
        terminated = false;
        queued = false;
        queuedMove = false;
        abort = false;
        waiting = 0;
    }

}