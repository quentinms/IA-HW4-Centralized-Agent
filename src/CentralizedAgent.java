
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import logist.agent.Agent;
import logist.behavior.CentralizedBehavior;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 */
@SuppressWarnings("unused")
public class CentralizedAgent implements CentralizedBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		// System.out.println("Agent " + agent.id() + " has tasks " + tasks);
		return centralizedPlan(vehicles, tasks);
	}

	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}

	private List<Plan> centralizedPlan(List<Vehicle> vehicles, TaskSet tasks) {

		Solution Aold = new Solution(tasks, vehicles);
		Aold.cost = Double.POSITIVE_INFINITY;
		
		Solution A = selectInitialSolution(vehicles, tasks);
		
		List<Solution> N = null; // TODO virer le = null quand tout le reste marchera

		int count = 0;
		while (count < 10000 && A.cost < Aold.cost) {
			
			System.out.println("Creating new solution");
			Aold = new Solution(A, "cloning");
			
			System.out.println("Choosing neighbours");
			N = chooseNeighbours(Aold, tasks, vehicles);
			
			// TODO Should Aold be in N?
			System.out.println("Choosing the local best");
			A = localChoice(N);
			System.out.println("A:"+A.debug);
			
			System.out.println("Next round");
			System.out.println();

			count++;
		}
		
		System.out.println(count);
		
		System.out.println(A.cost);
		
		return A.getPlan();
	

	}

	/**
	 * As an initial solution, we just take the vehicle with biggest capacity
	 * and assign all the tasks to it.
	 * TODO any clue why the problem isn't solvable if all tasks don't fit in the biggest vehicle?
	 * @param vehicles the list of vehicles
	 * @param tasks the liste of tasks
	 * @return an initial solution
	 */
	private Solution selectInitialSolution(List<Vehicle> vehicles, TaskSet tasks) {

		Vehicle biggestVehicle = null;
		int maxCapacity = 0;
		for (Vehicle vehicle : vehicles) {
			if (vehicle.capacity() > maxCapacity) {
				maxCapacity = vehicle.capacity();
				biggestVehicle = vehicle;
			}
		}

		Solution initialSolution = new Solution(tasks, vehicles);

//		Task previousTask = null;
//		int counter = 1;

		for (Task task : tasks) {

			/*if (previousTask == null) {
				//initialSolution.nextTaskVehicle[biggestVehicle, task);
				initialSolution.nextTaskVehicle[biggestVehicle.id()] = task;
			} else {
				initialSolution.nextTaskTask[previousTask.id] = task;
			}*/
			
			/*//initialSolution.vehicleTaskMap.put(task, biggestVehicle);
			initialSolution.vehicleTaskMap[task.id] = biggestVehicle;
			//initialSolution.time.put(task, counter);*/
			
			initialSolution.actionsList.get(biggestVehicle).add(new Action(task, "pickup"));
			initialSolution.actionsList.get(biggestVehicle).add(new Action(task, "delivery"));

			/*previousTask = task;
			counter++;*/

		}

		initialSolution.cost = initialSolution.computeCost();

		return initialSolution;
		
	}

	private List<Solution> chooseNeighbours(Solution Aold, TaskSet tasks, List<Vehicle> vehicles) {

		List<Solution> N = new ArrayList<Solution>();
		Vehicle vi = null;
		//System.out.println("141");
		
		while (vi == null || Aold.actionsList.get(vi).isEmpty()) {
			vi = vehicles.get((int) (Math.random() * vehicles.size()));
		}

		//System.out.println("146");

		// Applying the changing vehicle operator:
		for (Vehicle vj : vehicles) {
			if (!vj.equals(vi)) {
//				Task t = Aold.nextTaskVehicle[vi.id()];
				//System.out.println(151);
				//if (t.weight <= vj.capacity()) {
					List<Solution> A = changingVehicle(Aold, vi, vj);
					
					for(Solution solution: A){
						//System.out.println(solution);
						if (solution.verifyConstraints()) {
							N.add(solution);
						}
					}
				//}
			}
		}

		//System.out.println("160");

		// Applying the changing task order operator:
		// Task t = vi
		/*Task t = Aold.nextTaskVehicle[vi);
		int length = 0;

		do {
			t = Aold.nextTaskTask[t);
			length++;
		} while (t != null);

		System.out.println(172); System.out.println(length);

		if (length >= 2) {
			for (int tIndex1 = 1; tIndex1 < length; tIndex1++) {
				for (int tIndex2 = tIndex1 + 1; tIndex2 <= length; tIndex2++) {
					Solution A = changingTaskOrder(Aold, vi, tIndex1, tIndex2);
					if (A.verifyConstraints()) { //  pareil, un while non ?
						N.add(A);
					}
				}
			}
		}*/
		
		for (Action a1 : Aold.actionsList.get(vi)) {
			for (Action a2 : Aold.actionsList.get(vi)) {
				if(!a1.equals(a2)){
					Solution A = changingTaskOrder(Aold, vi, a1, a2);
						if (A.verifyConstraints()) {
							N.add(A);
					}
				}
			}
		}
		
		/*
		Task tPre1 = null;
		for (Task t1 = Aold.nextTaskVehicle[vi.id()]; t1 != null; t1 = Aold.nextTaskTask[t1.id]) {
			Task tPre2 = t1;
			for (Task t2 = Aold.nextTaskTask[t1.id]; t2 != null; t2 = Aold.nextTaskTask[t2.id]) {
				//System.out.println("Aold's actionsList: "+Aold.actionsList[vi.id()]);
				List<Solution> A = changingTaskOrder(Aold, vi, t1, t2, tPre1, tPre2);
				for(Solution solution: A){
					if (solution.verifyConstraints()) {
						N.add(solution);
					}
				}
				tPre2 = t2;
			}
			tPre1 = t1;
		}*/
		
		//System.out.println(189);
		return N;

	}

	private Solution localChoice(List<Solution> N) {

		Solution bestSolution = null;
		double leastCost = Double.POSITIVE_INFINITY;
		

		for (Solution solution : N) {
			
			if (solution.cost < leastCost) {
				leastCost = solution.cost;
				bestSolution = solution;
			}
		}
		
		System.out.println(bestSolution.cost);
		return bestSolution;
	}

	public List<Solution> changingVehicle(Solution A, Vehicle v1, Vehicle v2) {
		//System.out.println("A");
		List<Solution> solutions = new ArrayList<Solution>();
		
		Solution A1 = new Solution(A, "changingVehicle");
		Action action = A.actionsList.get(v1).get(0); // a pickup action
		Action deliveryAction = new Action(action.task, "delivery");
//		Task t = A.nextTaskVehicle[v1.id()];
		//System.out.println("B");
		
		//Remove actions corresponding to Task t from V1
//		A1.actionsList.get(v1).remove(new Action(t, "pickup"));
//		A1.actionsList.get(v1).remove(new Action(t, "delivery"));
		A1.actionsList.get(v1).remove(action);
		A1.actionsList.get(v1).remove(deliveryAction);
//		A1.actionsList.get(v1).remove(new Action(t, "delivery"));
		
//		System.out.println("Should link v1 to " + A1.nextTaskTask[t));
//		A1.nextTaskVehicle[v1.id()] = A1.nextTaskTask[t.id];
//		System.out.println("A1.nexttaskvehicle(v1) = " + A1.nextTaskVehicle[v1));
//		System.out.println("Should link t to " + A1.nextTaskVehicle[v2));
//		A1.nextTaskTask[t.id]= A1.nextTaskVehicle[v2.id()];
//		System.out.println("A1.nexttasktask(t) = " + A1.nextTaskTask[t));
//		System.out.println("Should link v2 to " + t);
//		A1.nextTaskVehicle[v2.id()] = t;
//		System.out.println("nexttaskvehicle(v2) = " + t);
//		System.out.println("Should link t to " + v2);
//		A1.vehicleTaskMap[t.id] = v2;
//		System.out.println("nexttaskmap(t) = " + v2);
		//System.out.println("C");
		
//		A1.actionsList.put(v2, new Action(t, "pickup"));
		A1.actionsList.get(v2).add(0, action);
//		A1.actionsList[v2.id()].add(0, new Action(t, "pickup"));
		/*updateTime(A1, v1);
		updateTime(A1, v2);*/
		
		
		//We can put it until the end of the array
		for (int i = 1; i < A1.actionsList.get(v2).size(); i++) {
			Solution A_tmp = new Solution(A1, A1.debug+"-taskfree-multi");
			A_tmp.actionsList.get(v2).add(i, deliveryAction);
			A_tmp.cost = A_tmp.computeCost();
			
			solutions.add(A_tmp);
		}
		/*
		for (int index = 1; index <= A1.actionsList[v2.id()].size(); index++){
			Solution A_tmp = new Solution(A1, A1.debug+"-multi");
			A_tmp.actionsList[v2.id()].add(index, new Action(t, "delivery"));
			A_tmp.cost = A_tmp.computeCost();
			
			solutions.add(A_tmp);
		}*/
		/*
		
		Solution A_tmp = new Solution(A1, A1.debug+"-multi");
		A_tmp.actionsList[v2.id()].add(1, new Action(t, "delivery"));
		A_tmp.cost = A_tmp.computeCost();
		
		solutions.add(A_tmp);
		*/
		
		//System.out.println("C");
		
		//System.out.println("D");
		
		//A1.cost = A1.computeCost();
		return solutions;
	}
	
	public Solution changingTaskOrder(Solution A, Vehicle vi, Action a1, Action a2) {//, Task tPre1, Task tPre2) {
		List<Solution> solutions = new ArrayList<Solution>();
//	public List<Solution> changingTaskOrder(Solution A, Vehicle vi, Task t1, Task t2) {//, Task tPre1, Task tPre2) {
//		List<Solution> solutions = new ArrayList<Solution>();
		
		Solution A1 = new Solution(A, "changingTaskOrder");
		//System.out.println("At ze buggining, A1.actionsList[vi.id()] is " + A1.actionsList[vi.id()]);
		
		/*Task tPost1 = A1.nextTaskTask[t1.id];
		Task tPost2 = A1.nextTaskTask[t2.id];
		
		if (tPost1.equals(t2)) {
			if(tPre1 != null){
				A1.nextTaskTask[tPre1.id] = t2;
			} else {
				A1.nextTaskVehicle[vi.id()] = t2;
			}
			A1.nextTaskTask[t2.id] = t1;
			A1.nextTaskTask[t1.id] = tPost2;
		} else {

			if(tPre1 != null){
				A1.nextTaskTask[tPre1.id] = t2;
			} else {
				A1.nextTaskVehicle[vi.id()] = t2;
			}
			A1.nextTaskTask[tPre2.id] =  t1;
			A1.nextTaskTask[t2.id] = tPost1;
			A1.nextTaskTask[t1.id] = tPost2;
		}*/
		
		//updateTime(A1, vi);
		
		
		
//		Action pickupT1 = new Action(t1, "pickup");
//		Action pickupT2 = new Action(t2, "pickup");
		//System.out.println("And btw T1 is " + t1 + "and T2 is " + t2);
//		int indexT1 = A1.actionsList[vi.id()].indexOf(a1);
		int indexT1 = A1.actionsList.get(vi).indexOf(a1);
		//System.out.println("Youy A1.actionsList[vi.id()] is " + A1.actionsList[vi.id()]);
//		int indexT2 = A1.actionsList[vi.id()].indexOf(a2);
		int indexT2 = A1.actionsList.get(vi).indexOf(a2);
		//System.out.println("Yay indexT1 is " + indexT1 + " and uuu indexT2 is " + indexT2);
		
		A1.actionsList.get(vi).remove(a1);
		A1.actionsList.get(vi).remove(a2);
		
		if(indexT1 < indexT2){
			A1.actionsList.get(vi).add(indexT1, a2);
			A1.actionsList.get(vi).add(indexT2, a1);
		} else {
			A1.actionsList.get(vi).add(indexT2, a1);
			A1.actionsList.get(vi).add(indexT1, a2);
		}
		
		/*
		A1.actionsList[vi.id()].remove(a1);
		A1.actionsList[vi.id()].remove(a2);
		A1.actionsList[vi.id()].add(indexT1, a1);
		A1.actionsList[vi.id()].add(indexT2, a2);
		*/
		/*
		Action deliveryT1 = new Action(t1, "delivery");
		Action deliveryT2 = new Action(t2, "delivery");
		//System.out.println("And btw T1 is " + t1 + "and T2 is " + t2);
		int indexDeliveryT1 = A1.actionsList[vi.id()].indexOf(deliveryT1);
		//System.out.println("Youy A1.actionsList[vi.id()] is " + A1.actionsList[vi.id()]);
		int indexDeliveryT2 = A1.actionsList[vi.id()].indexOf(deliveryT2);
		//System.out.println("Yay indexT1 is " + indexT1 + " and uuu indexT2 is " + indexT2);
		
		A1.actionsList[vi.id()].remove(deliveryT1);
		A1.actionsList[vi.id()].remove(deliveryT2);
		
		for (int i = indexT1+1; i <= A1.actionsList[vi.id()].size(); i++){
			for (int j = indexT2+1; j <= A1.actionsList[vi.id()].size(); j++){
				Solution A_tmp = new Solution(A1, A1.debug+"-multi");
	
				//Inserting messes up the indexes
				if(i < j){
					j = j + 1;
				}
				
				A_tmp.actionsList[vi.id()].add(i, deliveryT2);
				A_tmp.actionsList[vi.id()].add(j, deliveryT1);
				A_tmp.cost = A_tmp.computeCost();
				
				solutions.add(A_tmp);
				//System.out.println(solutions.size());
			}	
		}
		
		*/
		
		//System.out.println(264);
		
		
		A1.cost = A1.computeCost();
		
		
		//System.out.println(268);
		return A1;

	}

	/*public void updateTime(Solution A, Vehicle v) {
		Task ti = A.nextTaskVehicle[v);
		//System.out.println(275); System.out.println("*************" + A.debug);
		if (ti != null) {
			A.time.put(ti, 1);
			Task tj = null;
			do {
				tj = A.nextTaskTask[ti);
				if (tj != null) {
					A.time.put(tj, A.time.get(ti) + 1);
					ti = tj;
				}
				//System.out.println(285); System.out.println(tj);
			} while (tj != null);
			//System.out.println(287);
		}
		//System.out.println(289);
	}*/

}

class Solution {

	// TODO: privatiser/publiquiser toutes les variables une fois que le reste fonctionne.
	//HashMap<Task, Task> nextTaskTask;
	//Task[] nextTaskTask;
	//HashMap<Vehicle, Task> nextTaskVehicle;
	//Task[] nextTaskVehicle;
	//HashMap<Task, Integer> time;
	//HashMap<Task, Vehicle> vehicleTaskMap;
	//Vehicle[] vehicleTaskMap;
	HashMap<Vehicle, List<Action>> actionsList;
//	ArrayList[] actionsList;

	Double cost;
	String debug;

	static TaskSet tasks;
	static List<Vehicle> vehicles;

	public Solution(TaskSet tasks, List<Vehicle> vehicles) {
		//nextTaskTask = new Task[tasks.size()];//new HashMap<Task, Task>();
		//nextTaskVehicle = new Task[vehicles.size()];//new HashMap<Vehicle, Task>();
		//time = new HashMap<Task, Integer>();
		//vehicleTaskMap = new Vehicle[tasks.size()];//new HashMap<Task, Vehicle>();
		//TODO
		actionsList = new HashMap<Vehicle, List<Action>>();
		for(Vehicle v: vehicles){
			actionsList.put(v, new ArrayList<Action>());
		}
		Solution.tasks = tasks;
		Solution.vehicles = vehicles;
	}

	public Solution(Solution parentSolution, String debug) {
		//nextTaskTask = parentSolution.nextTaskTask.clone();//new HashMap<Task, Task>(parentSolution.nextTaskTask);
		//nextTaskVehicle = parentSolution.nextTaskVehicle.clone();//new HashMap<Vehicle, Task>(parentSolution.nextTaskVehicle);
		//time = new HashMap<Task, Integer>(parentSolution.time);
		//vehicleTaskMap = parentSolution.vehicleTaskMap.clone() ; //new HashMap<Task, Vehicle>(parentSolution.vehicleTaskMap);
		actionsList = new HashMap<Vehicle, List<Action>>();
		for(Vehicle v: vehicles){
			actionsList.put(v, new ArrayList<Action>(parentSolution.actionsList.get(v)));
		}
		cost = computeCost();
		this.debug = debug;
	}

	
	//TODO Rework the generated plan in order to handle multiple tasks at once.
	public List<Plan> getPlan() {

		List<Plan> plans = new ArrayList<Plan>();
		
		for(Vehicle v: vehicles){
			List<Action> actions = actionsList.get(v);
			City current = v.homeCity();
			Plan plan = new Plan(current);
			
			for (Action action: actions){
				
				for (City city : current.pathTo(action.city)) {
					plan.appendMove(city);
				}
				
				if(action.actionType.equals("pickup")){
					plan.appendPickup(action.task);
				} else {
					plan.appendDelivery(action.task);
				}
				
				current = action.city;
				
				
			}
			
			plans.add(plan);
			System.out.println("Vehicle "+(v.id()+1)+"'s cost is "+(plan.totalDistance()*v.costPerKm()));
			System.out.println("Vehicle "+(v.id()+1)+"'s plan is \n"+plan.toString());
		}

		return plans;
	}

	
	//TODO Rework the cost function in order to handle multiple tasks at once.
	double computeCost() {
		
		double cost = 0.0;
		
		for (Vehicle v: vehicles){
			City currentCity = v.homeCity();
			
			/*for(Task t = nextTaskVehicle[v); t != null; t = nextTaskTask.get(t)){
				cost += (currentCity.distanceTo(t.pickupCity)+t.pickupCity.distanceTo(t.deliveryCity))*v.costPerKm();
				currentCity = t.deliveryCity;
			}*/
			
			for (Action action: actionsList.get(v)) {
				cost+=currentCity.distanceTo(action.city)*v.costPerKm();
				currentCity = action.city;
			}
			
		}
		
		return cost;
		
	}

	/**
	 * TODO verify that it is correct http://i.imgur.com/xVyoSl.jpg
	 * Rivo: looks okay to me :)
	 * @return true if the constraints are fulfilled, false otherwise.
	 */
	 Boolean verifyConstraints() {

		/*
		 * Constraint 1
		 * nextT ask(t) ̸= t: the task delivered after some task t cannot be the same task;
		 
		for (Task currentTask: tasks) {
			Task nextTask = nextTaskTask[currentTask.id];
			if (currentTask != null && currentTask.equals(nextTask)) {
				System.out.println("Constraint1");
				return false;
			}
		}*/

		/*
		 * Constraint 2
		 * nextTask(vk) = tj ⇒ time(tj) = 1: already explained
		 
		for (Vehicle vk : vehicles) {
			Task tj = nextTaskVehicle[vk);
			if (tj != null && time.get(tj) != 1) {
				System.out.println("Constraint2");
				return false;
			}
		}

		/*
		 * Constraint 3
		 * nextTask(ti) = tj ��� time(tj) = time(ti) + 1:
		 * already explained
		 
		for (Task ti : nextTaskTask.keySet()) {
			Task tj = nextTaskTask.get(ti);
			if (tj != null && time.get(tj) != time.get(ti) + 1) {
				System.out.println("Constraint3");
				return false;
			}
		}

		/*
		 * Constraint 4
		 * nextTask(vk) = tj ��� vehicle(tj) = vk: already explained
		 
		for (Vehicle vk : vehicles) {
			Task tj = nextTaskVehicle[vk.id()];
			if (tj!= null && !vk.equals(vehicleTaskMap[tj.id])) {
				System.out.println("Constraint4 "+this.debug);
				return false;
			}
		}*/

		/*
		 * Constraint 5
		 * nextTask(ti) = tj ⇒ vehicle(tj) = vehicle(ti)
		 *
		for (Task ti : tasks) {
			Task tj = nextTaskTask[ti.id];
			if (tj != null && !vehicleTaskMap[tj.id].equals(vehicleTaskMap[ti.id])) {
				System.out.println("Constraint5");
				return false;
			}
		}*/

		/*
		 * Constraint 6
		 * TODO: verify & approve intent.
		 * all tasks must be delivered: the set of values of the variables
		 * in the nextTask array must be equal to the set of tasks T plus
		 * NV times the value NULL
		 
		
		// return false if taskCounter + nullCounter ��� |nextTask| + NV
		int nullCounter = 0;
		
		TaskSet verifTasks = TaskSet.copyOf(tasks);
		
		for (Vehicle v : vehicles) {
			for (Task t = nextTaskVehicle[v.id()]; t != null; t = nextTaskTask[t.id]) {
				Boolean removed = verifTasks.remove(t);
				if (!removed) {
					//We cannot remove this task, either we already removed the task, or it should not exist
					return false;
				}
			}
			nullCounter++;
		}*/
		
		
		/*
		for (int i = 0; i < nextTaskTask.size(); i++) {
			Task currentTask = nextTaskTask.get(i);
			if (currentTask == null) {
				nullCounter++;
			} else if (!tasks.contains(currentTask)) {
				return false;
			} else {
				taskCounter++;
			}
		}
		
		
		for (int i = 0; i < nextTaskVehicle.size(); i++) {
			Task currentTask = nextTaskVehicle[i);
			if (currentTask == null) {
				nullCounter++;
			} else if (!tasks.contains(currentTask)) { //Will never happen
				return false;
			} else {
				taskCounter++;
			}
		}
		
		if (!verifTasks.isEmpty() || nullCounter != vehicles.size()) {
			System.out.println("Constraint6 "+verifTasks.size()+"/"+tasks.size()+" - "+nullCounter+"/"+vehicles.size());
			return false;
		}*/

		/*
		 * Constraint 7 //TODO gérer le poids
		 * the capacity of a vehicle cannot be exceeded:
		 * the capacity of a vehicle cannot be exceeded: if load(ti) > capacity(vk) ⇒ vehicle(ti)  ̸= vk
		 *
		for (Vehicle vk : vehicles) {
			int carriedWeight = 0;
			for (Task ti = nextTaskVehicle[vk); ti != null; ti = nextTaskTask.get(ti)) {
				carriedWeight += ti.weight;
			}
			if (carriedWeight > vk.capacity()) { // TODO: && vehicleTaskMap.get(ti).equals(vk) ?
				System.out.println("Constraint7 "+carriedWeight+"/"+vk.capacity());
				return false;
			}
		}*/
		
		for(Vehicle v: vehicles){
			int carriedWeight = 0;
			for(Action action: actionsList.get(v)) {
				if(action.actionType.equals("pickup")){
					carriedWeight += action.task.weight;
				} else {
					carriedWeight -= action.task.weight;
				}
	
				if(carriedWeight > v.capacity()){
					System.out.println("Constraint 7");
					return false;
				}
			}
		}
		
		
		/*Constraint 8
		 * Verify that pickups are before deliveries
		 * */
		
		for(Vehicle v: vehicles){
			ArrayList<Task> stack = new ArrayList<Task>();
			
			for(Object act: actionsList.get(v)){
				Action action = (Action)act;
				if(action.actionType.equals("pickup")){
					stack.add(action.task);
				} else {
					if(!stack.remove(action.task)){
						System.out.println("Constraint 8 "+debug+" - "+action.task);
						return false;
					}
				}
			}
			
			//TODO Verify all task are delivered
			
		}

		return true;
	}
	 
	 @Override
	public String toString() {
		return debug+" : "+cost+" | ";
	}
	 
}

class Action{
	Task task;
	String actionType;
	City city;
	
	public Action(Task t, String type){
		task = t;
		actionType = type;
		if(actionType.equals("pickup")){
			city = task.pickupCity;
		} else {
			city = task.deliveryCity;
		}
	}
	
	@Override
	public String toString() {
		return actionType+" Task"+task.id+" in "+city;
	}
	
	public boolean equals(Object obj) {
		Action action = (Action) obj;
		return this.task.equals(action.task) && this.actionType.equals(action.actionType);
	}
}
