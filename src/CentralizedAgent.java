
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

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
		Boolean ok = true;

		int count = 0;
		//TODO sometimes, A is null. Not sure if it should happen
		//TODO "termination condition" something else than a counter?
		while (count < 1000 && A != null && Aold.cost > A.cost) {
			
			System.out.println("Creating new solution");
			Aold = new Solution(A, "cloning");
			
			System.out.println("Choosing neighbours");
			N = chooseNeighbours(Aold, tasks, vehicles);
			
			// TODO Should Aold be in N?
			System.out.println("Choosing the local best");
			A = localChoice(N);
			
			System.out.println("Next round");
			System.out.println();

			count++;
		}
		
		System.out.println(count);

		if(A != null){
			return A.getPlan();
		} else {
			return Aold.getPlan();
		}

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

		Task previousTask = null;
		int counter = 1;

		for (Task task : tasks) {

			if (previousTask == null) {
				initialSolution.nextTaskVehicle.put(biggestVehicle, task);
			} else {
				initialSolution.nextTaskTask.put(previousTask, task);
			}

			initialSolution.vehicleTaskMap.put(task, biggestVehicle);
			initialSolution.time.put(task, counter);

			previousTask = task;
			counter++;

		}

		initialSolution.cost = initialSolution.computeCost();

		return initialSolution;
		
	}

	private List<Solution> chooseNeighbours(Solution Aold, TaskSet tasks, List<Vehicle> vehicles) {

		List<Solution> N = new ArrayList<Solution>();
		Vehicle vi = null;
		System.out.println("141");
		
		while (vi == null || Aold.nextTaskVehicle.get(vi) == null) {
			vi = vehicles.get((int) (Math.random() * vehicles.size()));
		}

		System.out.println("146");

		// Applying the changing vehicle operator:
		for (Vehicle vj : vehicles) {
			if (!vj.equals(vi)) {
				Task t = Aold.nextTaskVehicle.get(vi);
				System.out.println(151);
				// TODO gerer le poids
				if (t.weight <= vj.capacity()) {
					//TODO: plutôt while(!A.verifyConstraints) {} non ?
					Solution A = changingVehicle(Aold, vi, vj);
					if (A.verifyConstraints()) {
						N.add(A);
					}
				}
			}
		}

		System.out.println("160");

		// Applying the changing task order operator:
		// TODO waaat?
		// Task t = vi
		/*Task t = Aold.nextTaskVehicle.get(vi);
		int length = 0;

		do {
			t = Aold.nextTaskTask.get(t);
			length++;
		} while (t != null);

		System.out.println(172); System.out.println(length);

		if (length >= 2) {
			for (int tIndex1 = 1; tIndex1 < length; tIndex1++) {
				for (int tIndex2 = tIndex1 + 1; tIndex2 <= length; tIndex2++) {
					Solution A = changingTaskOrder(Aold, vi, tIndex1, tIndex2);
					if (A.verifyConstraints()) { // TODO pareil, un while non ?
						N.add(A);
					}
				}
			}
		}*/
		
		Task tPre1 = null;
		for (Task t1 = Aold.nextTaskVehicle.get(vi); t1 != null; t1 = Aold.nextTaskTask.get(t1)) {
			Task tPre2 = t1;
			for (Task t2 = Aold.nextTaskTask.get(t1); t2 != null; t2 = Aold.nextTaskTask.get(t2)) {
				Solution A = changingTaskOrder(Aold, vi, t1, t2, tPre1, tPre2);
				if (A.verifyConstraints()) {
					N.add(A);
				}
				tPre2 = t2;
			}
			tPre1 = t1;
		}
		
		System.out.println(189);
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
		
		return bestSolution;
	}

	public Solution changingVehicle(Solution A, Vehicle v1, Vehicle v2) {
		System.out.println("A");
		Solution A1 = new Solution(A, "changingVehicle");
		Task t = A.nextTaskVehicle.get(v1);
		System.out.println("B");
		A1.nextTaskVehicle.put(v1, A1.nextTaskTask.get(t));
		A1.nextTaskTask.put(t, A1.nextTaskVehicle.get(v2));
		A1.nextTaskVehicle.put(v2, t);
		System.out.println("C");
		updateTime(A1, v1);
		updateTime(A1, v2);
		System.out.println("D");
		A1.vehicleTaskMap.put(t, v2);
		A1.cost = A1.computeCost();
		return A1;
	}
	
	//TODO There's a bug causing a loop in the tasks.
	//TODO There's a bug causing null to be in the hashmap
	//TODO Reecrire la fonction completement parceque leur code c'est vraiment de la merde.
	public Solution changingTaskOrder(Solution A, Vehicle vi, Task t1, Task t2, Task tPre1, Task tPre2) {
		
		Solution A1 = new Solution(A, "changingTaskOrder");
		
		Task tPost1 = A1.nextTaskTask.get(t1);
		Task tPost2 = A1.nextTaskTask.get(t2);
		
		if (tPost1.equals(t2)) {
			if(tPre1 != null){
				A1.nextTaskTask.put(tPre1, t2);
			} else {
				A1.nextTaskVehicle.put(vi, t2);
			}
			A1.nextTaskTask.put(t2, t1);
			A1.nextTaskTask.put(t1, tPost2);
		} else {

			if(tPre1 != null){
				A1.nextTaskTask.put(tPre1, t2);
			} else {
				A1.nextTaskVehicle.put(vi, t2);
			}
			A1.nextTaskTask.put(tPre2, t1);
			A1.nextTaskTask.put(t2, tPost1);
			A1.nextTaskTask.put(t1, tPost2);
		}
		
		
		/*System.out.println(230);
		// TODO wat?
		// Task tPre1 = vi
		Task tPre1 = null;

		Task t1 = A1.nextTaskVehicle.get(vi);
		System.out.println(236);
		for (int count = 1; count < taskIndex1; count++) {
			tPre1 = t1;
			t1 = A1.nextTaskTask.get(t1);
		}
		System.out.println(tPre1);//TODO tPre1 should not be null, I guess
		System.out.println(240);
		Task tPost1 = A1.nextTaskTask.get(t1);
		Task tPre2 = t1;

		Task t2 = A1.nextTaskTask.get(tPre2);

		for (int count = taskIndex1; count < taskIndex2; count++) {
			tPre2 = t2;
			t2 = A1.nextTaskTask.get(t2);
		}
		System.out.println(250);
		Task tPost2 = A1.nextTaskTask.get(t2);
		//TODO t2, tPre1, tPre2, etc. should not be null
		if (tPost1.equals(t2)) {
			A1.nextTaskTask.put(tPre1, t2);
			A1.nextTaskTask.put(t2, t1);
			A1.nextTaskTask.put(t1, tPost2);
		} else {

			A1.nextTaskTask.put(tPre1, t2);
			A1.nextTaskTask.put(tPre2, t1);
			A1.nextTaskTask.put(t2, tPost1);
			A1.nextTaskTask.put(t1, tPost2);
		}
		*/
		
		System.out.println(264);
		updateTime(A1, vi);
		
		A1.cost = A1.computeCost();
		System.out.println(268);
		return A1;

	}

	public void updateTime(Solution A, Vehicle v) {
		Task ti = A.nextTaskVehicle.get(v);
		System.out.println(275); System.out.println("*************" + A.debug);
		if (ti != null) {
			A.time.put(ti, 1);
			Task tj = null;
			do {
				tj = A.nextTaskTask.get(ti);
				if (tj != null) {
					A.time.put(tj, A.time.get(ti) + 1);
					ti = tj;
				}
				//System.out.println(285); System.out.println(tj);
			} while (tj != null);
			//System.out.println(287);
		}
		//System.out.println(289);
	}

}

class Solution {

	// TODO: privatiser/publiquiser toutes les variables une fois que le reste fonctionne.
	HashMap<Task, Task> nextTaskTask;
	HashMap<Vehicle, Task> nextTaskVehicle;
	HashMap<Task, Integer> time;
	HashMap<Task, Vehicle> vehicleTaskMap;
	Double cost;
	String debug;

	static TaskSet tasks;
	static List<Vehicle> vehicles;

	public Solution(TaskSet tasks, List<Vehicle> vehicles) {
		nextTaskTask = new HashMap<Task, Task>();
		nextTaskVehicle = new HashMap<Vehicle, Task>();
		time = new HashMap<Task, Integer>();
		vehicleTaskMap = new HashMap<Task, Vehicle>();
		Solution.tasks = tasks;
		Solution.vehicles = vehicles;
	}

	public Solution(Solution parentSolution, String debug) {
		nextTaskTask = new HashMap<Task, Task>(parentSolution.nextTaskTask);
		nextTaskVehicle = new HashMap<Vehicle, Task>(parentSolution.nextTaskVehicle);
		time = new HashMap<Task, Integer>(parentSolution.time);
		vehicleTaskMap = new HashMap<Task, Vehicle>(parentSolution.vehicleTaskMap);
		cost = computeCost();
		this.debug = debug;
	}

	public List<Plan> getPlan() {

		List<Plan> plans = new ArrayList<Plan>();

		for (Vehicle v : vehicles) {

			City current = v.homeCity();
			Plan plan = new Plan(current);

			Task t = nextTaskVehicle.get(v);

			if (t != null) {
				
				for (City city : current.pathTo(t.pickupCity))
					plan.appendMove(city);

				plan.appendPickup(t);

				// move: pickup location => delivery location
				for (City city : t.path()) {
					plan.appendMove(city);
				}

				plan.appendDelivery(t);

				// set current city
				current = t.deliveryCity;

				while (nextTaskTask.get(t) != null) {

					t = nextTaskTask.get(t);

					for (City city : current.pathTo(t.pickupCity)) {
						plan.appendMove(city);
					}

					plan.appendPickup(t);

					// move: pickup location => delivery location
					for (City city : t.path())
						plan.appendMove(city);

					plan.appendDelivery(t);

					// set current city
					current = t.deliveryCity;

				}

				plans.add(plan);
				
			} else {
				plans.add(Plan.EMPTY);
			}

		}

		return plans;
	}

	double computeCost() {
		
		double cost = 0.0;

		for (Task ti : tasks) {
			Task nextTask = nextTaskTask.get(ti);
			if (nextTask != null) {
				cost += (ti.deliveryCity.distanceTo(nextTask.pickupCity)
						+ nextTask.pickupCity.distanceTo(nextTask.deliveryCity))
						* vehicleTaskMap.get(ti).costPerKm();
			}
		}

		for (Vehicle vi : vehicles) {
			Task nextTask = nextTaskVehicle.get(vi);
			if (nextTask != null) {
				cost += (vi.homeCity().distanceTo(nextTask.pickupCity)
						+ nextTask.pickupCity.distanceTo(nextTask.deliveryCity))
						* vi.costPerKm();
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
		 * nextTask(t) ≠ t: the task delivered after
		 * some task t cannot be the same task.
		 */
		for (int i = 0; i < nextTaskTask.size(); i++) {
			Task currentTask = nextTaskTask.get(i);
			Task nextTask = nextTaskTask.get(i + 1);
			if (currentTask != null && currentTask.equals(nextTask)) {
				return false;
			}
		}

		/*
		 * Constraint 2
		 * nextTask(vk) = tj ⇒ time(tj) = 1: already explained
		 */
		for (Vehicle vk : vehicles) {
			Task tj = nextTaskVehicle.get(vk);
			if (tj != null && time.get(tj) != 1) {
				return false;
			}
		}

		/*
		 * Constraint 3
		 * nextTask(ti) = tj ⇒ time(tj) = time(ti) + 1:
		 * already explained
		 */
		for (Task ti : nextTaskTask.keySet()) {
			Task tj = nextTaskTask.get(ti);
			if (tj != null && time.get(tj) != time.get(ti) + 1) {
				return false;
			}
		}

		/*
		 * Constraint 4
		 * nextTask(vk) = tj ⇒ vehicle(tj) = vk: already explained
		 */
		for (Vehicle vk : vehicles) {
			Task tj = nextTaskVehicle.get(vk);
			if (!vk.equals(vehicleTaskMap.get(tj))) {
				return false;
			}
		}

		/*
		 * Constraint 5
		 * nextTask(ti) = tj ⇒ vehicle(tj) = vehicle(ti): already explained
		 */
		for (Task ti : nextTaskTask.keySet()) {
			Task tj = nextTaskTask.get(ti);
			if (!vehicleTaskMap.get(tj).equals(vehicleTaskMap.get(ti))) {
				return false;
			}
		}

		/*
		 * Constraint 6
		 * TODO: verify & approve intent.
		 * all tasks must be delivered: the set of values of the variables
		 * in the nextTask array must be equal to the set of tasks T plus
		 * NV times the value NULL
		 */
		
		// return false if taskCounter + nullCounter ≠ |nextTask| + NV
		int nullCounter = 0;
		int taskCounter = 0;
		
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
			Task currentTask = nextTaskTask.get(i);
			if (currentTask == null) {
				nullCounter++;
			} else if (!tasks.contains(currentTask)) {
				return false;
			} else {
				taskCounter++;
			}
		}
		
		if (nullCounter + taskCounter != nextTaskTask.size() + nextTaskVehicle.size() + vehicles.size()) {
			return false;
		}

		/*
		 * Constraint 7
		 * the capacity of a vehicle cannot be exceeded:
		 * if load(ti) > capacity(vk) then vehicle(ti) ≠ vk
		 */
		for (Vehicle vk : vehicles) {
			int carriedWeight = 0;
			for (Task ti = nextTaskVehicle.get(vk); ti != null; ti = nextTaskVehicle.get(ti)) {
				carriedWeight += ti.weight;
			}
			if (carriedWeight > vk.capacity()) { // TODO: && vehicleTaskMap.get(ti).equals(vk) ?
				return false;
			}
		}

		return true;
	}
}
