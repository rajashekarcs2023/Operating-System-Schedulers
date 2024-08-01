import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

public class paging {
    public static void main(String[] args) throws FileNotFoundException {
        File file = new File("random-numbers.txt");
        Scanner nums = new Scanner(file);
        String alg = args[5].toLowerCase();

        int[] input = setUp(args);
        if(alg.equals("fifo")) {
            fifo(nums, input, args);
        }
        else if(alg.equals("random")) {
            random(nums, input, args);
        }
        else {
            lru(nums, input, args);
        }
        
        nums.close();
    }

    public static void printResults(ArrayList<Process> toPrint, String[] args){
        System.out.println("The machine size is " + args[0]);
        System.out.println("The page size is " + args[1]);
        System.out.println("The process size is " + args[2]);
        System.out.println("The job mix is " + args[3]);
        System.out.println("The number of references per process is " + args[4]);
        System.out.println("The replacement algorithm is " + args[5]);
        System.out.println("The level of debugging output is " + args[6]);
        System.out.println();

        Process.runningSum = 0;
        Process.totalEvictions = 0;
        for(Process p: toPrint){
            if(p.evictions != 0){
            double residency = p.totalResidency/(double)p.evictions;
            System.out.println("Process " + p.id + " had " + p.faults + " faults and " + residency + " average residency");
            Process.runningSum += p.totalResidency;
            Process.totalEvictions += p.evictions;
            }
            else{
                System.out.println("Process " + p.id + " had " + p.faults + " faults");
                System.out.println("\tWith no evictions, the average residence is undefined.");
            }
        }
        System.out.println();
        if(Process.totalEvictions == 0){
            System.out.println("The total number of faults is " + Process.pageFaults);
            System.out.println("\tWith no evictions, the overall average residence is undefined.");
        }
        else{
        double avgResidency = Process.runningSum/(double)Process.totalEvictions;
        System.out.println("The total number of faults is " + Process.pageFaults + " and the overall average residency is " + avgResidency);
        }
    }

    public static void random(Scanner nums, int[] input, String[] args){
        ArrayList<Frame> frames = initializeFrames(input);
        Queue<Process> processQ = new LinkedList<>();
        int totalReferences = initializeQ(processQ, input);
        ArrayList<Page> pageTable = new ArrayList<>();
        initializePageTable(pageTable, input);

        ArrayList<Process> toPrint = new ArrayList<>(processQ);
        Process curr = processQ.poll();
        int count = 0;
        for(int i = 0; i < totalReferences; i++, count++){
            if(curr.remainingReferences == 0){
                curr = processQ.poll();
                count = 0;
            }
            if(count == 3){
                processQ.add(curr);
                curr = processQ.poll();
                count = 0;
            }
            //System.out.print("Time: " + (Page.time+1) + " Process: "+ curr.id+ " Got word: " + curr.w);
            randomContainsWord(curr, frames, pageTable, nums);
            curr.remainingReferences--;
            randomWord(nums, input, curr);

        }

        Collections.sort(toPrint);

        printResults(toPrint, args);
    }

    public static boolean randomContainsWord(Process p, ArrayList<Frame> frames, ArrayList<Page> pageTable, Scanner nums){
        Page.time++;
        for(Frame f: frames){
            if(f.hasWord(p.w) && f.pid == p.id && f.currentProcess == p) {
                //System.out.println(" Hit in frame " + (f.id -1) + " For process " + p.id);
                addResidency(frames);
                return true;
            }
        }


        Page locate = pageTable.get(0);
        for(Page page: pageTable){
            if(page.hasWord(p.w)){
                locate = page; 
                break;
            }
        }

        //System.out.print(" Page Fault for process " + p.id + " word: " + p.w );
        p.faults++;
        Process.pageFaults++;
        if(Frame.vacantFrames != 0){
            addResidency(frames);
            for(int i = 0; i < frames.size(); i++){
                Frame newFrame = frames.get(i);
                if(newFrame.vacant){
                    newFrame.copyPage(locate);
                    newFrame.page.loadTime = Page.time;
                    newFrame.vacant = false;
                    Frame.vacantFrames--;
                    newFrame.pid = p.id;
                    newFrame.currentProcess = p;
                    //System.out.println(" Using free frame: " + (newFrame.id-1));
                    break;
                }
            }
        }

        else{
            int evictIndex = nums.nextInt() % frames.size();
            Frame evict = frames.get(frames.size()-1-evictIndex);
            Process.totalEvictions++;
            //Process.runningSum += evict.page.loaded.get(evict.pid);
            evict.currentProcess.evictions++;
            //System.out.println(" The evict id is " + evict.pid + " With amount " + evict.page.loaded.get(evict.pid));
            evict.currentProcess.totalResidency += (Page.time - evict.page.loadTime);
            evict.page.loaded.put(evict.pid, 0);
            //System.out.println(" Evicting page " + (evict.page.id) + " From Frame: " + (evict.id-1));

            ArrayList<Frame> toAddResidency = new ArrayList<>(frames);
            toAddResidency.remove(evictIndex);
            addResidency(toAddResidency);
            evict.pid = p.id;
            evict.copyPage(locate);
            evict.page.loadTime = Page.time;
            evict.currentProcess = p;
        }
        return false;
    }

    public static void lru(Scanner nums, int[] input, String[] args){
        ArrayList<Frame> frames = initializeFrames(input);
        Queue<Process> processQ = new LinkedList<>();
        int totalReferences = initializeQ(processQ, input);
        ArrayList<Page> pageTable = new ArrayList<>();
        initializePageTable(pageTable, input);

        ArrayList<Process> toPrint = new ArrayList<>(processQ);
        Process curr = processQ.poll();
        int count = 0;
        for(int i = 0; i < totalReferences; i++, count++){
            if(curr.remainingReferences == 0){
                curr = processQ.poll();
                count = 0;
            }
            if(count == 3){
                processQ.add(curr);
                curr = processQ.poll();
                count = 0;
            }
           // System.out.print("Time: " + (Page.time+1) + " Process: "+ curr.id+ " Got word: " + curr.w);
            lruContainsWord(curr, frames, pageTable);
            curr.remainingReferences--;
            randomWord(nums, input, curr);

        }

        Collections.sort(toPrint);

        printResults(toPrint, args);
    }

    public static boolean lruContainsWord(Process p, ArrayList<Frame> frames, ArrayList<Page> pageTable){
        //printFrameOrder(frames);
        Page.time++;
        Frame lru = new Frame();
        Boolean hasReference = false;
        for(Frame f: frames){
            if(f.hasWord(p.w) && f.pid == p.id && f.currentProcess == p) {
               // System.out.println(" Hit in frame " + (f.id -1) + " For process " + p.id);
                addResidency(frames);
                lru = f;
                hasReference = true;
                break;
            }
        }

        if(hasReference){
            frames.remove(lru);
            frames.add(lru);
            return true;
        }

        Page locate = pageTable.get(0);
        for(Page page: pageTable){
            if(page.hasWord(p.w)){
                locate = page; 
                break;
            }
        }

       // System.out.print(" Page Fault for process " + p.id + " word: " + p.w );
        p.faults++;
        Process.pageFaults++;
        if(Frame.vacantFrames != 0){
            addResidency(frames);
            for(int i = 0; i < frames.size(); i++){
                Frame newFrame = frames.get(i);
                if(newFrame.vacant){
                    newFrame.copyPage(locate);
                    newFrame.page.loadTime = Page.time;
                    newFrame.vacant = false;
                    Frame.vacantFrames--;
                    newFrame.pid = p.id;
                    newFrame.currentProcess = p;
                    //System.out.println(" Using free frame: " + (newFrame.id-1));
                    lru = frames.get(i);
                    frames.remove(i);
                    frames.add(lru);
                    break;
                }
            }
        }

        else{
            Frame evict = frames.get(0);
            Process.totalEvictions++;
            Process.runningSum += (Page.time - evict.page.loadTime);
            evict.currentProcess.evictions++;
            //System.out.println("The evict id is " + evict.pid + " With amount " + evict.page.loaded.get(evict.pid));
            evict.currentProcess.totalResidency += (Page.time - evict.page.loadTime);
            evict.page.loaded.put(evict.pid, 0);
            //System.out.println(" Evicting page " + (evict.page.id) + " From Frame: " + (evict.id-1));
            frames.remove(0);
            addResidency(frames);
            evict.pid = p.id;
            evict.copyPage(locate);
            evict.page.loadTime = Page.time;
            evict.currentProcess = p;
            frames.add(evict);
        }
        return false;
    }

    public static void printFrameOrder(ArrayList<Frame> frames){
        System.out.print("\tThe frame order is: ");
        for(Frame f: frames){
            System.out.print((f.id-1) + " ");
        }
        System.out.println();
    }

    public static void fifo(Scanner nums, int[] input, String[] args){
        ArrayList<Frame> frames = initializeFrames(input);
        Queue<Process> processQ = new LinkedList<>();
        int totalReferences = initializeQ(processQ, input);
        ArrayList<Page> pageTable = new ArrayList<>();
        initializePageTable(pageTable, input);

        ArrayList<Process> toPrint = new ArrayList<>(processQ);
        Process curr = processQ.poll();
        int count = 0;
        for(int i = 0; i < totalReferences; i++, count++){
            if(curr.remainingReferences == 0){
                curr = processQ.poll();
                count = 0;
            }
            if(count == 3){
                processQ.add(curr);
                curr = processQ.poll();
                count = 0;
            }
           // System.out.print("Time: " + Page.time + " Process: "+ curr.id+ " Got word: " + curr.w);
            fifoContainsWord(curr, frames, pageTable);
            curr.remainingReferences--;
            randomWord(nums, input, curr);

        }

        Collections.sort(toPrint);

        printResults(toPrint, args);
    }

    public static boolean fifoContainsWord(Process p, ArrayList<Frame> frames, ArrayList<Page> pageTable){
        Page.time++;
        for(Frame f: frames){
            if(f.hasWord(p.w) && f.pid == p.id && f.currentProcess == p) {
                //System.out.println(" Hit in frame " + (f.id -1) + " For process " + p.id);
                addResidency(frames);
                return true;
            }
        }


        Page locate = pageTable.get(0);
        for(Page page: pageTable){
            if(page.hasWord(p.w)){
                locate = page; 
                break;
            }
        }

       // System.out.print(" Page Fault for process " + p.id + " word: " + p.w );
        p.faults++;
        Process.pageFaults++;
        if(Frame.vacantFrames != 0){
            addResidency(frames);
            for(int i = 0; i < frames.size(); i++){
                Frame newFrame = frames.get(i);
                if(newFrame.vacant){
                    newFrame.copyPage(locate);
                    newFrame.page.loadTime = Page.time;
                    newFrame.vacant = false;
                    Frame.vacantFrames--;
                    newFrame.pid = p.id;
                    newFrame.currentProcess = p;
                    //System.out.println(" Using free frame: " + (newFrame.id-1));
                    break;
                }
            }
        }

        else{
            Frame evict = frames.get(0);
            Process.totalEvictions++;
            Process.runningSum += evict.page.loaded.get(evict.pid);
            evict.currentProcess.evictions++;
            //System.out.println("The evict id is " + evict.pid + " With amount " + evict.page.loaded.get(evict.pid));
            evict.currentProcess.totalResidency += (Page.time - evict.page.loadTime);
            evict.page.loaded.put(evict.pid, 0);
            //System.out.println(" Evicting page " + (evict.page.id) + " From Frame: " + (evict.id-1));
            frames.remove(0);
            addResidency(frames);
            evict.pid = p.id;
            evict.copyPage(locate);
            evict.page.loadTime = Page.time;
            evict.currentProcess = p;
            frames.add(evict);
        }
        return false;
    }

    public static void addResidency(ArrayList<Frame> frames){
        for(Frame f: frames){
            if(!f.vacant){
                f.page.addResidency(f.pid);
            }
        }
        
    }

    public static void initializePageTable(ArrayList<Page> pageTable, int[] input){
        for(int i = 0; i < input[2]; i++){
            int low = i;
            i += input[1]-1;
            int high = i;
            pageTable.add(new Page(low, high));
        }
    }

    public static ArrayList<Frame> initializeFrames(int[] input){
        ArrayList<Frame> frames = new ArrayList<>();
        int size = Frame.size;
        for(int i = 0; i < size; i++){
            frames.add(new Frame());
        }
        return frames;
    }

    public static int initializeQ(Queue<Process> processQ, int[] input){
        int s = input[2];
        int references = input[4];
        int totalReferences = 0;
        if(input[3] == 1){
            totalReferences = input[4];
            processQ.add(new Process(s,references));
        }
        else{
            totalReferences = 4*input[4];
            for(int i = 0; i < 4; i++){
                processQ.add(new Process(s,references));
            }
        }
        return totalReferences;
    }

    public static void randomWord(Scanner nums, int[] input, Process p){
        int j = input[3];
        double a = 0, b = 0, c = 0;
        if(j == 1){
            a = 1;
            b = 0;
            c = 0;
        }
        else if(j == 2){
            a = 1;
            b = 0;
            c = 0;

        }
        else if(j == 3){
            a = 0;
            b = 0;
            c = 0;

        }
        else if(j == 4){
            if(p.id == 1){
                a = .75;
                b = .25;
                c = 0;
            }
            else if(p.id == 2){
                a = .75;
                b = 0;
                c = .25;
            }
            else if(p.id == 3){
                a = .75;
                b = .125;
                c = .125;
            }
            else if(p.id == 4){
                a = .5;
                b = .125;
                c = .125;
            }
        }

        int s = input[2];
        int r = nums.nextInt();
        //System.out.println("Random number: " + r + " was used");
        double y = r/(Integer.MAX_VALUE + 1d);


        if( y < a){
            p.w = (p.w+1+s)%s;
        }
        else if(y < a+b){
            p.w = (p.w-5+s)%s;
        }
        else if(y < a+b+c){
            p.w = (p.w+4+s)%s;
        }
        else{
            p.w = nums.nextInt() % s;
        }
    }

    //Parse input as int
    public static int[] setUp(String[] args){
        int[] input = new int[5];
       
        for(int i = 0; i < 5; i++){
            input[i] = Integer.parseInt(args[i]);
        }
        Frame.size = input[0]/input[1];
        Frame.vacantFrames = Frame.size;
        return input;
    }

}

class Process implements Comparable<Process>{
    int w;
    int id;
    int remainingReferences;
    int faults;
    int totalResidency;
    int evictions;
    static int num = 1;
    static int pageFaults = 0;
    static int totalEvictions = 0;
    static int runningSum = 0;
    static int residencyTime = 0;
    
    
    Process(int s, int references){
        this.id = num;
        remainingReferences = references;
        w = 111 * id % s;
        num++;
        faults = 0;
        totalResidency = 0;
        evictions = 0;
    }


	@Override
	public int compareTo(Process o) {
		return this.id - o.id;
	}

}

class Frame{
    int id;
    int pid;
    Page page;
    boolean vacant;
    Process currentProcess;
    static int size;
    static int vacantFrames;

    Frame(){
        this.id = size;
        vacant = true;
        size--;
    }

    public void copyPage(Page p){
        this.page = new Page(p);
    }

    public boolean hasWord(int w){
        if(page == null) return false;
        return page.hasWord(w);
    }
}

class Page{
    int id;
    int pid;
    int low;
    int high;
    int loadTime;
    HashMap<Integer, Integer> loaded = new HashMap<>();
    static int time = 0;
    static int pages = 0;

    Page(int low, int high){
        this.id = pages;
        this.low = low;
        this.high = high;
        pages++;
    }

    Page(Page p){
        this.low = p.low;
        this.high = p.high;
    }

    public boolean hasWord(int w){
        return w <= high && w >= low;
    }

    public void addResidency(int pid){
        loaded.put(pid, (loaded.getOrDefault(pid, 0) + 1));
    }


}