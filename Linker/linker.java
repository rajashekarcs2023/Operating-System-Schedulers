import java.util.*;
public class linker {
	static Scanner input = new Scanner(System.in);
	static ArrayList<String> output = new ArrayList<>();
	static int globalIndex = 0;
	static HashSet<String> multiply = new HashSet<>();
	static ArrayList<String> errors = new ArrayList<>();
	public static void main(String[] args) {
		
		int modules = input.nextInt();
		String[] link = new String[modules*3];
		for(int i = 0; i < modules*3; i += 3) {
			link[i] = addDefList();
			link[i+1] = addUseList();
			link[i+2] = addAddress();
		}
		
		HashMap<String, Integer> notUsed = new HashMap<>();
		
		input.close();
		
		popOutput(link);
		for(int i = 0; i < output.size(); i++) {
			errors.add("");
		}
		
		HashMap<String, Integer> symTable = new HashMap<>();
		int baseAddress = 0;
		for(int i = 0; i < link.length; i+= 3) {
			if(!link[i].equals("0")) {
				putSymbol(link[i], symTable, baseAddress, notUsed, i/3);
			}
			String[] toNum = link[i+2].split(" ");
			baseAddress += Integer.parseInt(toNum[0]);
			
		}
		
		baseAddress = 0;
		
		for(int i = 2; i < link.length; i += 3) {
			String[] addField = link[i].split(" ");
			link[i] = relocate(addField, baseAddress);
			baseAddress += Integer.parseInt(addField[0]);
			globalIndex = baseAddress;
			
		}
		
		globalIndex = 0;
		baseAddress = 0;
		
		for(int i = 1; i < link.length; i += 3) {
			String[] toNum = link[i].split(" ");
			int numUse = Integer.parseInt(toNum[0]);
			String[] useList = link[i].split(" ");
			HashSet<Integer> lastUse = new HashSet<>();
			for(int j = numUse, k = numUse *2; j > 0; j--, k -= 2) {
				String[] addField = link[i+1].split(" ");
				String sym = useList[k-1];
				int index = Integer.parseInt(useList[k]);
				if(lastUse.contains(index)) {
					errors.set(globalIndex + index, "Error: Multiple symbols used here; last one used");
					continue;
				} 
				lastUse.add(index);
				notUsed.remove(sym);
				
				link[i+1] = relocateSym(addField, sym, index, symTable);
			}
			toNum = link[i+1].split(" ");
			baseAddress += Integer.parseInt(toNum[0]);
			globalIndex = baseAddress;
		}
		
		System.out.println("Symbol Table");
		for(String s: symTable.keySet()) {
			System.out.print(s + " = " + symTable.get(s) + " ");
			if(multiply.contains(s)) System.out.print("Error: This variable is multiply defined; last value used.");
			System.out.println();
		}
		System.out.println();
		System.out.println("Memory Map");
		for(int i = 0; i < output.size(); i++) {
			if(i < 10)
			System.out.println(i + ":    " + output.get(i) + " " + errors.get(i));
			else System.out.println(i + ":   " + output.get(i) + " " + errors.get(i));
		}
		
		for(String s: notUsed.keySet()) System.out.println("Warning: Symbol '" + s + "' was defined in module " + notUsed.get(s) + " but never used.");
		
	}
	
	public static void popOutput(String[] link) {
		for(int i = 2; i < link.length; i += 3) {
			String[] addList = link[i].split(" ");
			for(int j = 2; j < addList.length; j += 2) {
				output.add(addList[j]);
			}
		}
	}
	
	public static void printArr(String[] arr) {
		for(String i : arr) System.out.println(i);
	}
	
	public static void putSymbol(String definition, HashMap<String, Integer> symTable, int baseAddress, HashMap<String,Integer> notUsed, int module) {
		String [] defList= definition.split(" ");
		for(int j = 1; j < defList.length; j += 2) {
			if(!symTable.containsKey(defList[j])) {
				String symbol = defList[j];
				notUsed.put(symbol,module);
				int symValue = Integer.parseInt(defList[j+1]) + baseAddress;
				symTable.put(symbol, symValue);
			}
			else {
				String symbol = defList[j];
				multiply.add(symbol);
				notUsed.put(symbol, module);
				int symValue = Integer.parseInt(defList[j+1]) + baseAddress;
				symTable.put(symbol, symValue);
			}
		}
	}
	
	public static String relocate(String[] addField, int baseAddress) {
		String add = addField[0] + " ";
		int modSize = Integer.parseInt(addField[0]);
		int localIndex = 0;
		for(int i = 1; i < addField.length; i+=2) {
			add += addField[i] + " ";
			if(addField[i].equals("R")) {
				int field = Integer.parseInt(addField[i+1]);
				if(field - (field/1000)*1000  > modSize) {
					field -= field - (field/1000)*1000;
					errors.set(globalIndex + localIndex, "Error: Type R address exceeds module size: 0 (relative) used");
				} 
				field += baseAddress;
				output.set(globalIndex + localIndex, Integer.toString(field));
				add += Integer.toString(field) + " ";
			}
			else if(addField[i].equals("A")) {
				int field = Integer.parseInt(addField[i+1]);
				if(field - (field/1000)*1000  > 299) {
					field -= field - (field/1000)*1000;
					field += 299;
					errors.set(globalIndex + localIndex, "Error: A type address exceeds machine size; max legal value used");
				}
				output.set(globalIndex + localIndex, Integer.toString(field));
				add += Integer.toString(field) + " ";
			}
			else add += addField[i+1] + " ";
			localIndex++;
		}
		return add;
	}
	
	public static String relocateSym(String[] addField, String sym, int index, HashMap<String, Integer> symTable) {
		ArrayList<String> list = new ArrayList<>();
		int localIndex = 0;
		for(String s: addField) list.add(s);
		
		int currIndex = 0;
		
		for(int i = 1; i < addField.length; i+=2) {
			if(currIndex == index) {
				
				int field = Integer.parseInt(addField[i+1]);
				int nextIndex = field;
				field = field/1000 * 1000;
				nextIndex -= nextIndex/1000 *1000;
				if(!symTable.containsKey(sym)) {
					field += 111;
					errors.set(globalIndex + localIndex, "Error: The symbol '" + sym +"' does not exist. 111 will be used.");
				}
				else field += symTable.get(sym);
				list.set(i+1, Integer.toString(field));
				output.set(globalIndex + localIndex, Integer.toString(field));
				if(nextIndex == 777) break;
				else {
					index = nextIndex;
					i = -1;
					currIndex = -1;
					localIndex = -1;
				}
				
			}
			
			localIndex++;
			currIndex++;
		}
		String add = "";
		for(String s: list) add += s+ " ";
		return add;
	}
	public static String addDefList() {
		int cycles = input.nextInt();
		String defString = Integer.toString(cycles) + " ";
		for(int i = 0; i < cycles; i++) {
			defString += input.next() + " " + input.next() + " ";
		}
		return defString;
	}
	
	public static String addUseList() {
		int cycles = input.nextInt();
		String useString = Integer.toString(cycles) + " ";
		for(int i = 0; i < cycles; i++) {
			useString += input.next() + " " + input.next() + " ";
		}
		return useString;
	}
	
	public static String addAddress() {
		int cycles = input.nextInt();
		String addString = Integer.toString(cycles) + " ";
		for(int i = 0; i < cycles; i++) {
			String add1 = input.next();
			if(add1.length() > 1) {
				String temp1 = add1.substring(0,1);
				String temp2 = add1.substring(1);
				addString += temp1 + " " + temp2 + " ";
				continue;
			}
			addString += add1 + " " + input.next() + " ";
		}
		return addString;
	}
}
