import java.io.*;
import java.util.*;
public class scheduling{
    public static void main(String[] args) throws IOException{
        File file;
        if(args[0].toLowerCase().equals("-verbose")){
            process.verbose = true;
            file = new File(args[1]);
        }
        else{
            file = new File(args[0]);
        }
        fcfs(file);
        process.resetProcess();
        //rr(file); 
        // process.resetProcess();
        // uniProgram(file);
        // process.resetProcess();
        // sjf(file);
    }
    public static void printVerboseRR(process[] processArray){
        if(process.verbose){String outPut = String.format("%-20s","Before Cycle\t" + process.cycles + ":");
        for(process p : processArray){
            String state = p.state;
            if(state.equals("Ready") || state.equals("Terminated")){
                outPut += String.format("%-15s", state + " " + 0);    
            }
            else
            outPut += String.format("%-15s", p.state + " " + Math.max(p.quantum, p.remainBlock));
        }
        System.out.println(outPut);}
        
    }
    public static void printVerbose(process[] processArray){
        if(process.verbose){ String outPut = String.format("%-20s","Before Cycle\t" + process.cycles + ":");
        for(process p : processArray){
            String state = p.state;
            if(state.equals("Ready") || state.equals("Terminated")){
                outPut += String.format("%-15s", state + " " + 0);    
            }
            else
            outPut += String.format("%-15s", p.state + " " + Math.max(p.remainCPUBurst, p.remainBlock));
        }
        System.out.println(outPut);}
    }
    // Round Robin ------------------------------------------------------------------------------------------
    public static void rr(File file) throws IOException{
        Scanner input = new Scanner(file);
        String original = "";
        int modules = input.nextInt();
        original += modules;
        process[] processArray = new process[modules];
        
        for(int i = 0; i < modules;i++){
            int A = input.nextInt();
            original += " " + A;
            int B = input.nextInt();
            original += " " + B;
            int C = input.nextInt();
            original += " " + C;
            int IO = input.nextInt();
            original += " " + IO + " ";
            processArray[i] = new process(A, B, C, IO, i);
        }
        input.close();
        Arrays.sort(processArray);
        
        for(int i = 0; i < processArray.length; i++){
            processArray[i].processNum = i;
        }

        String sorted = "" + modules;
        for(process p : processArray){
            sorted += " " + p.A;
            sorted += " " + p.B;
            sorted += " " + p.C;
            sorted += " " + p.IO + " ";
        }

        System.out.println("The original input was: "+ original);
        System.out.println("The (sorted) input is:  " + sorted + "\n");
        if(process.verbose) System.out.println("This detailed printout gives the state and remaining burst for each process\n");
        rrProcess(processArray);
        System.out.println("\nThe scheduling algorithm used was Round Robin\n");  
        printArray(processArray);
        printSummary(processArray);
    }
    public static void rrProcess(process[] processArray) throws IOException{
        File randomFile = new File("random-numbers.txt");
        Scanner randomGen = new Scanner(randomFile);
        Queue<process> readyQ = new LinkedList<>();
        PriorityQueue<process> isReady = new PriorityQueue<>((p1,p2) -> p1.compareTo(p2));
        printVerboseRR(processArray);
        for(process p: processArray){
            if(p.hasArrived()){
                p.state = "Ready";
                readyQ.add(p);
            }       
        }
        while(!allTerminated(processArray)){
            while(!readyQ.isEmpty()){
                process p = readyQ.poll();
                p.state = "Running";
                if(p.remainCPUBurst == 0){
                    int baseRand = randomGen.nextInt();
                    int CPUBurst = modCalc(baseRand, p.B);
                    p.remainCPUBurst = CPUBurst;
                }
                int quantum = 0;
                if(p.remainCPUBurst > 2){
                    p.quantum = 2;
                    quantum = 2;
                }
                else{
                    quantum = p.remainCPUBurst;
                    p.quantum = quantum;
                }
                while(quantum != 0 && p.remainCPU != 0){
                    countReady(processArray);
                    p.cycles++;
                    printVerboseRR(processArray);
                    p.remainCPU--;
                    quantum--;
                    p.quantum--;
                    p.remainCPUBurst--;
                    p.totalRunning++;
                    decrementBlockRR(processArray, isReady);
                    addArrivalRR(isReady, processArray);
                    if(p.remainCPU == 0){
                        p.state = "Terminated";
                        p.finishingTime = p.cycles;
                    }
                    if (p.remainCPU != 0 && p.remainCPUBurst > 0 && quantum == 0){
                        p.state = "Ready";
                        isReady.add(p);
                    }
                    while(!isReady.isEmpty()){
                        readyQ.add(isReady.poll());
                    }
                    
                }
            if(p.remainCPU != 0 && p.remainCPUBurst == 0){
                int baseRand = randomGen.nextInt();
                int IOBurst = modCalc(baseRand, p.IO);
                p.remainBlock = IOBurst;
                p.state = "Blocked";
            }
            while(!isReady.isEmpty()){
                readyQ.add(isReady.poll());
            }
            }
        emptyQueueRR(readyQ, processArray);
        }   
    }
    public static void emptyQueueRR(Queue<process> readyQ, process[] processArray){
        PriorityQueue<process> isReady = new PriorityQueue<>();
        while(isAllBlocked(processArray)){
            printVerboseRR(processArray);
            for(process pBlocked: processArray){
                if(pBlocked.remainBlock != 0){
                    pBlocked.remainBlock--;
                    pBlocked.IOTime++;
                    if(pBlocked.remainBlock == 0){
                        isReady.add(pBlocked);
                        pBlocked.state = "Ready";
                    }
                }
            }
            addArrivalRR(isReady, processArray);
            while(!isReady.isEmpty()){
                readyQ.add(isReady.poll());
            }
            process.totalBlocked++;
            process.cycles++;
        }
        
    }
    public static void addArrivalRR(PriorityQueue<process> isReady, process[] processArray){
        for(process p: processArray){
            if(p.A == p.cycles){
                p.state = "Ready";
                isReady.add(p);
            }
        }
    }
    public static void decrementBlockRR(process[] processArray, PriorityQueue<process> isReady){
        boolean addTotalBlocked = true;
        for(process pBlocked: processArray){
            if(pBlocked.remainBlock > 0){
                pBlocked.remainBlock--;
                pBlocked.IOTime++;
            if(addTotalBlocked){
                addTotalBlocked = false;
                process.totalBlocked++;
            }
                if(pBlocked.remainBlock == 0){
                    pBlocked.state = "Ready";
                    isReady.add(pBlocked);
                }
            }
        }
    }
    // Shortest Job First ------------------------------------------------------------------------------------------
    public static void sjf(File file) throws IOException{
        Scanner input = new Scanner(file);
        String original = "";
        int modules = input.nextInt();
        original += modules;
        process[] processArray = new process[modules];
        
        for(int i = 0; i < modules;i++){
            int A = input.nextInt();
            original += " " + A;
            int B = input.nextInt();
            original += " " + B;
            int C = input.nextInt();
            original += " " + C;
            int IO = input.nextInt();
            original += " " + IO + " ";
            processArray[i] = new process(A, B, C, IO, i);
        }
        input.close();
        Arrays.sort(processArray);
        
        for(int i = 0; i < processArray.length; i++){
            processArray[i].processNum = i;
        }

        String sorted = "" + modules;
        for(process p : processArray){
            sorted += " " + p.A;
            sorted += " " + p.B;
            sorted += " " + p.C;
            sorted += " " + p.IO + " ";
        }

        System.out.println("The original input was: "+ original);
        System.out.println("The (sorted) input is:  " + sorted + "\n"); 
        if(process.verbose) System.out.println("This detailed printout gives the state and remaining burst for each process\n");     
        sjfProcess(processArray);
        System.out.println("\nThe scheduling algorithm used was Shortest Job First\n");  
        printArray(processArray);
        printSummary(processArray);

    }
    public static void sjfProcess(process[] processArray) throws IOException{
        File randomFile = new File("random-numbers.txt");
        Scanner randomGen = new Scanner(randomFile);
        PriorityQueue<process> readyPQ = new PriorityQueue<>((p1,p2) -> 
            p1.remainCPU == p2.remainCPU ? p1.compareTo(p2) : p1.remainCPU - p2.remainCPU);
            
        printVerbose(processArray);
        for(process p: processArray){
            if(p.hasArrived()){
                p.state = "Ready";
                readyPQ.add(p);
            }       
        }
        while(!allTerminated(processArray)){
            while(!readyPQ.isEmpty()){
                process p = readyPQ.poll();
                p.state = "Running";
                int baseRand = randomGen.nextInt();
                int CPUBurst = modCalc(baseRand, p.B);
                p.remainCPUBurst = CPUBurst;
                while(CPUBurst != 0 && p.remainCPU != 0){
                    countReady(processArray);
                    p.cycles++;
                    p.remainCPU--;
                    CPUBurst--;
                    p.totalRunning++;
                    printVerbose(processArray);
                    p.remainCPUBurst--;
                    decrementBlock(readyPQ, processArray);
                    addArrival(readyPQ, processArray);
                    if(p.remainCPU == 0){
                        p.state = "Terminated";
                        p.finishingTime = p.cycles;
                    }
                }
            if(p.remainCPU != 0){
                baseRand = randomGen.nextInt();
                int IOBurst = modCalc(baseRand, p.IO);
                p.remainBlock = IOBurst;
                p.state = "Blocked";
            }
        }
        emptyQueue(readyPQ, processArray);
    } 

        printPQ(readyPQ);
    }
    public static void decrementBlock(PriorityQueue<process> readyPQ, process[] processArray){
        boolean addTotalBlocked = true;
        for(process pBlocked: processArray){
            if(pBlocked.remainBlock > 0){
                pBlocked.remainBlock--;
                pBlocked.IOTime++;
            if(addTotalBlocked){
                addTotalBlocked = false;
                process.totalBlocked++;
            }
                if(pBlocked.remainBlock == 0){
                    pBlocked.state = "Ready";
                    readyPQ.add(pBlocked);
                }
            }
        }
    }
    public static void emptyQueue(PriorityQueue<process> readyPQ, process[] processArray){
        while(isAllBlocked(processArray)){
            printVerbose(processArray);
            for(process pBlocked: processArray){
                if(pBlocked.remainBlock != 0){
                    pBlocked.remainBlock--;
                    pBlocked.IOTime++;
                    if(pBlocked.remainBlock == 0){
                        readyPQ.add(pBlocked);
                        pBlocked.state = "Ready";
                    }
                }
            }
            process.totalBlocked++;
            process.cycles++;
            addArrival(readyPQ, processArray);
        }
    }
    public static void addArrival(PriorityQueue<process> readyPQ, process[] processArray){
        for(process p: processArray){
            if(p.A == p.cycles){
                p.state = "Ready";
                readyPQ.add(p);
            }
        }
    }
    public static void printPQ(PriorityQueue<process> readyPQ){
        PriorityQueue<process> pq = new PriorityQueue<>(readyPQ);
        while(!pq.isEmpty()){
            process p = pq.poll();
            p.printData();
        }
    }
        // FIRST COME FIRST SERVE ------------------------------------------------------------------------------------------
    public static void fcfs(File file) throws IOException{
        Scanner input = new Scanner(file);
        String original = "";
        int modules = input.nextInt();
        original += modules;
        process[] processArray = new process[modules];
        
        for(int i = 0; i < modules;i++){
            int A = input.nextInt();
            original += " " + A;
            int B = input.nextInt();
            original += " " + B;
            int C = input.nextInt();
            original += " " + C;
            int IO = input.nextInt();
            original += " " + IO + " ";
            processArray[i] = new process(A, B, C, IO, i);
        }
        input.close();
        Arrays.sort(processArray);
        
        for(int i = 0; i < processArray.length; i++){
            processArray[i].processNum = i;
        }

        String sorted = "" + modules;
        for(process p : processArray){
            sorted += " " + p.A;
            sorted += " " + p.B;
            sorted += " " + p.C;
            sorted += " " + p.IO + " ";
        }

        System.out.println("The original input was: "+ original);
        System.out.println("The (sorted) input is:  " + sorted + "\n");   
        if(process.verbose) System.out.println("This detailed printout gives the state and remaining burst for each process\n");    
        fcfsProcess(processArray);
        System.out.println("\nThe scheduling algorithm used was First Come First Serve\n"); 
        printArray(processArray);
        printSummary(processArray);

    }
    public static void fcfsProcess(process[] processArray) throws IOException{
        File randomFile = new File("random-numbers.txt");
        Scanner randomGen = new Scanner(randomFile);
        Queue<process> readyQ = new LinkedList<>();
        printVerbose(processArray);
        for(process p: processArray){
            if(p.hasArrived()){
                p.state = "Ready";
                readyQ.add(p);
            }       
        }
        while(!allTerminated(processArray)){
            while(!readyQ.isEmpty()){
                process p = readyQ.poll();
                p.state = "Running";
                int baseRand = randomGen.nextInt();
                int CPUBurst = modCalc(baseRand, p.B);
                p.remainCPUBurst = CPUBurst;
                while(CPUBurst != 0 && p.remainCPU != 0){
                    countReady(processArray);
                    p.cycles++;
                    p.remainCPU--;
                    CPUBurst--;
                    p.totalRunning++;
                    printVerbose(processArray);
                    p.remainCPUBurst--;
                    decrementBlock(readyQ, processArray);
                    addArrival(readyQ, processArray);
                    if(p.remainCPU == 0){
                        p.state = "Terminated";
                        p.finishingTime = p.cycles;
                    }
                }
            if(p.remainCPU != 0){
                baseRand = randomGen.nextInt();
                int IOBurst = modCalc(baseRand, p.IO);
                p.remainBlock = IOBurst;
                p.state = "Blocked";
            }
        }
        emptyQueue(readyQ, processArray);
    }   
}
    public static void countReady(process[] processArray){
        for(process pReady: processArray){
            if(pReady.state.equals("Ready")){
                pReady.waitingTime++;
            }
        }
    } 
    public static void emptyQueue(Queue<process> readyQ, process[] processArray){
        while(isAllBlocked(processArray)){
            printVerbose(processArray);
            for(process pBlocked: processArray){
                if(pBlocked.remainBlock != 0){
                    pBlocked.remainBlock--;
                    pBlocked.IOTime++;
                    if(pBlocked.remainBlock == 0){
                        readyQ.add(pBlocked);
                        pBlocked.state = "Ready";
                    }
                }
            }
            process.totalBlocked++;
            process.cycles++;
            addArrival(readyQ, processArray);
        }
    }
    public static void decrementBlock(Queue<process> readyQ, process[] processArray){
        boolean addTotalBlocked = true;
        for(process pBlocked: processArray){
            if(pBlocked.remainBlock > 0){
                pBlocked.remainBlock--;
                pBlocked.IOTime++;
            if(addTotalBlocked){
                addTotalBlocked = false;
                process.totalBlocked++;
            }
                if(pBlocked.remainBlock == 0){
                    pBlocked.state = "Ready";
                    readyQ.add(pBlocked);
                }
            }
        }
    }
    public static void addArrival(Queue<process> readyQ, process[] processArray){
        for(process p: processArray){
            if(p.A == p.cycles){
                p.state = "Ready";
                readyQ.add(p);
            }
        }
    }
    public static boolean isAllBlocked(process[] processArray){
        if(allTerminated(processArray)) return false;
        for(process p: processArray){
            if(p.state.equals("Ready"))
                return false;
        }
        return true;
    }
    public static boolean allTerminated(process[] processArray){
        for(process p: processArray){
            if(p.remainCPU != 0)
                return false;
        }
        return true;
    }
    public static void printSummary(process[] processArray){
        double totalTurnAround = 0.0;
        double totalWaiting = 0.0;
        for(process p : processArray){
            totalTurnAround += (p.finishingTime - p.A);
            totalWaiting += p.waitingTime;
        }
        System.out.println("Summary Data:");
        System.out.println("\tFinishing Time: " + process.cycles);
        System.out.printf("\tCPU Utilization: %.6f\n", (process.totalRunning/(double)process.cycles));
        System.out.printf("\tI/O Utilization: %.6f\n", (process.totalBlocked/(double)process.cycles));
        System.out.printf("\tThroughput: %.6f processes per hundred cycles \n", 100.0/process.cycles * processArray.length);
        System.out.printf("\tAverage turnaround time: %.6f\n",(double)totalTurnAround/(double)processArray.length);
        System.out.printf("\tAverage waiting time: %.6f\n\n", totalWaiting/(double)processArray.length);
    }

    // UNIPROGRAMMING ------------------------------------------------------------------------------------------
    public static void uniProgram(File file) throws IOException{
        Scanner input = new Scanner(file);
        String original = "";
        int modules = input.nextInt();
        original += modules; 
        process[] processArray = new process[modules];
        for(int i = 0; i < modules;i++){
            int A = input.nextInt();
            original += " " + A;
            int B = input.nextInt();
            original += " " + B;
            int C = input.nextInt();
            original += " " + C;
            int IO = input.nextInt();
            original += " " + IO + " ";
            processArray[i] = new process(A, B, C, IO, i);
        }
        input.close();
        Arrays.sort(processArray);
        String sorted = " " + modules;
        for(int i = 0; i < processArray.length; i++){
            processArray[i].processNum = i;
            sorted += " " + processArray[i].A;
            sorted += " " + processArray[i].B;
            sorted += " " + processArray[i].C;
            sorted += " " + processArray[i].IO + " ";
        }
        int waitingTime = 0;
        sorted = String.format("%-25s", sorted);
        int cycles = 0;
        System.out.println("The original input was: "+ original);
        System.out.println("The (sorted) input is: " + sorted + "\n");
        if(process.verbose) System.out.println("This detailed printout gives the state and remaining burst for each process\n");
        printVerbose(processArray);
        for(process p : processArray){
            cycles = uniProcess(p, cycles, processArray);
            p.finishingTime = cycles;
            if(p.processNum == 0) p.waitingTime = 0;
            else p.waitingTime = waitingTime - p.A;
            
            waitingTime += cycles- waitingTime;
        }
        System.out.println("\nThe scheduling algorithm used was Uniprocessor\n"); 
        double totalTurnAround = 0.0;
        double totalWaiting = 0.0;
        for(process p : processArray){
            p.printData();
            totalTurnAround += (p.finishingTime - p.A);
            totalWaiting += p.waitingTime;
            System.out.println();
        }
        System.out.println("Summary Data:");
        System.out.println("\tFinishing Time: " + cycles);
        System.out.printf("\tCPU Utilization: %.6f\n", (process.totalRunning/(double)cycles));
        System.out.printf("\tI/O Utilization: %.6f\n", (process.totalBlocked/(double)cycles));
        System.out.printf("\tThroughput: %.6f processes per hundred cycles \n", 100.0/cycles * modules);
        System.out.printf("\tAverage turnaround time: %.6f\n",totalTurnAround/modules);
        System.out.printf("\tAverage waiting time: %.6f\n\n", totalWaiting/modules);
           
    }
    public static int uniProcess(process p, int cycles, process[] processArray) throws IOException{
        File randomFile = new File("random-numbers.txt");
        Scanner randomGen = new Scanner(randomFile);
        for(int i = 0; i < process.randCount; i++){
            randomGen.nextInt();
        }
        
        while(p.remainCPU != 0){
            int baseRand = randomGen.nextInt();
            process.randCount++;
            int CPUBurst = modCalc(baseRand, p.B);
            p.remainCPUBurst = CPUBurst;
            while(CPUBurst != 0 && p.remainCPU != 0){
                p.state = "Running";
                process.cycles++;
                printVerbose(processArray);
                p.remainCPU--;
                CPUBurst--;
                p.remainCPUBurst--;
                cycles++;
                p.totalRunning++;
            }
            if(p.remainCPU == 0) break;
            
            baseRand = randomGen.nextInt();
            process.randCount++;
            int IOBurst = modCalc(baseRand, p.IO);
            p.remainBlock = IOBurst;
            while(IOBurst != 0){
                p.state = "Blocked";
                p.IOTime++;
                IOBurst--;
                cycles++;
                p.totalBlocked++;
                process.cycles++;
                printVerbose(processArray);
                p.remainBlock--;
            }

        }
        p.state = "Terminated";
        randomGen.close();
        return cycles;
    }

    public static int modCalc(int baseRand, int mod){
        return 1 + (baseRand % mod);
    }

    public static void printArray(process[] processArray){
        for(process p: processArray)
            p.printData();
    }

}

class process implements Comparable<process>{
    int A;
    int B;
    int C;
    int IO;
    int IOTime;
    int waitingTime;
    int finishingTime;
    int processNum;
    int remainCPU;
    int remainBlock;
    int remainCPUBurst;
    int quantum;
    String state;
    static int cycles = 0;
    static int randCount = 0;
    static int totalRunning = 0;
    static int totalBlocked = 0;
    static boolean verbose;

    public process(int A, int B, int C, int IO, int processNum){
        this.A = A;
        this.B = B;
        this.C = C;
        this.remainCPU = C;
        this.IO = IO;
        this.IOTime = 0;
        this.waitingTime = 0;
        this.finishingTime = 0;
        this.processNum = processNum;
        this.remainBlock = 0;
        this.state = "Unstarted";
    }
    public void printData(){
        System.out.println("Process " + processNum + ":");
        System.out.println("\t(A,B,C,IO) = (" + A + "," + B + "," + C + "," + IO + ")");
        System.out.println("\tFinishing Time: " + finishingTime);
        System.out.println("\tTurnaround Time: " + (finishingTime - A));
        System.out.println("\tIO Time: " + IOTime);
        System.out.println("\tWaiting Time: " + waitingTime);
        System.out.println();
    }

    public int compareTo(process o){
        if(o.A < this.A){
            return 1;
        }
        else if(o.A > this.A){
            return -1;
        }
        return this.processNum - o.processNum ;
    }
    public boolean hasArrived(){
        return A <= cycles;
    }
    public static void resetProcess(){
        randCount = 0;
        totalRunning = 0;
        totalBlocked = 0;
        cycles = 0;
        System.out.println("------------------------------------------------------------------------------------------");
    }

}