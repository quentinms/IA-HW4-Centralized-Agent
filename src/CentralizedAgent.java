
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

		Solution Aold = new Solution(vehicles);
		Aold.cost = Double.POSITIVE_INFINITY;
		
		Solution A = selectInitialSolution(vehicles, tasks);
		
		List<Solution> N = null;

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
		
		System.out.println(A.cost);
		
		return A.getPlan();
	

	}

	/**
	 * As an initial solution, we just take the vehicle with biggest capacity
	 * and assign all the tasks to it.
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

		Solution initialSolution = new Solution(vehicles);

		for (Task task : tasks) {
			initialSolution.actionsList.get(biggestVehicle).add(new Action(task, "pickup"));
			initialSolution.actionsList.get(biggestVehicle).add(new Action(task, "delivery"));
		}

		initialSolution.cost = initialSolution.computeCost();

		return initialSolution;
		
	}

	private List<Solution> chooseNeighbours(Solution Aold, TaskSet tasks, List<Vehicle> vehicles) {

		List<Solution> N = new ArrayList<Solution>();
		Vehicle vi = null;
		
		while (vi == null || Aold.actionsList.get(vi).isEmpty()) {
			vi = vehicles.get((int) (Math.random() * vehicles.size()));
		}

		// Applying the changing vehicle operator:
		for (Vehicle vj : vehicles) {
			if (!vj.equals(vi)) {
				List<Solution> A = changingVehicle(Aold, vi, vj);
				for (Solution solution: A) {
					if (solution.verifyConstraints()) {
						N.add(solution);
					}
				}
			}
		}
		
		for (Action a1 : Aold.actionsList.get(vi)) {
			for (Action a2 : Aold.actionsList.get(vi)) {
				if (!a1.equals(a2)) {
					Solution A = changingTaskOrder(Aold, vi, a1, a2);
					if (A.verifyConstraints()) {
						N.add(A);
					}
				}
			}
		}

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
		List<Solution> solutions = new ArrayList<Solution>();
		
		Solution A1 = new Solution(A, "changingVehicle");
		Action pickupAction = A.actionsList.get(v1).get(0); // a pickup action
		Action deliveryAction = new Action(pickupAction.task, "delivery");
		A1.actionsList.get(v1).remove(pickupAction);
		A1.actionsList.get(v1).remove(deliveryAction);
		A1.actionsList.get(v2).add(0, pickupAction);
		
		//We can put it until the end of the array
		for (int i = 1; i < A1.actionsList.get(v2).size(); i++) {
			Solution A_tmp = new Solution(A1, A1.debug+"-taskfree-multi");
			A_tmp.actionsList.get(v2).add(i, deliveryAction);
			A_tmp.cost = A_tmp.computeCost();
			
			solutions.add(A_tmp);
		}

		return solutions;
	}
	
	public Solution changingTaskOrder(Solution A, Vehicle vi, Action a1, Action a2) {
		
		Solution A1 = new Solution(A, "changingTaskOrder");
		int indexT1 = A1.actionsList.get(vi).indexOf(a1);
		int indexT2 = A1.actionsList.get(vi).indexOf(a2);
		
		A1.actionsList.get(vi).remove(a1);
		A1.actionsList.get(vi).remove(a2);
		
		if(indexT1 < indexT2){
			A1.actionsList.get(vi).add(indexT1, a2);
			A1.actionsList.get(vi).add(indexT2, a1);
		} else {
			A1.actionsList.get(vi).add(indexT2, a1);
			A1.actionsList.get(vi).add(indexT1, a2);
		}
		
		A1.cost = A1.computeCost();
		return A1;

	}

}

class Solution {

	protected HashMap<Vehicle, List<Action>> actionsList;

	protected Double cost;
	protected String debug;

	private static List<Vehicle> vehicles;

	public Solution(List<Vehicle> vehicles) {
		actionsList = new HashMap<Vehicle, List<Action>>();
		for (Vehicle vehicle : vehicles) {
			actionsList.put(vehicle, new ArrayList<Action>());
		}
		Solution.vehicles = vehicles;
	}

	public Solution(Solution parentSolution, String debug) {
		actionsList = new HashMap<Vehicle, List<Action>>();
		for (Vehicle vehicle : vehicles) {
			actionsList.put(vehicle, new ArrayList<Action>(parentSolution.actionsList.get(vehicle)));
		}
		cost = computeCost();
		this.debug = debug;
	}

	
	public List<Plan> getPlan() {

		List<Plan> plans = new ArrayList<Plan>();
		
		for (Vehicle vehicle : vehicles) {
			
			List<Action> actions = actionsList.get(vehicle);
			City current = vehicle.homeCity();
			Plan plan = new Plan(current);
			
			for (Action action: actions){
				
				for (City city : current.pathTo(action.city)) {
					plan.appendMove(city);
				}
				
				if (action.actionType.equals("pickup")) {
					plan.appendPickup(action.task);
				} else if (action.actionType.equals("delivery")) {
					plan.appendDelivery(action.task);
				} else {
					System.err.println("Error in getPlan(): some action is neither pickup nor delivery.");
				}
				
				current = action.city;
				
			}
			
			plans.add(plan);
			
			System.out.println("Vehicle " + (vehicle.id() + 1) + "'s cost is " + (plan.totalDistance() * vehicle.costPerKm()));
			System.out.println("Vehicle " + (vehicle.id() + 1) + "'s plan is \n" + plan.toString());
			
		}

		return plans;
	}

	
	double computeCost() {
		double cost = 0.0;
		
		for (Vehicle vehicle : vehicles) {
			City currentCity = vehicle.homeCity();
			for (Action action : actionsList.get(vehicle)) {
				cost += currentCity.distanceTo(action.city) * vehicle.costPerKm();
				currentCity = action.city;
			}
		}
		
		return cost;
	}

	/**
	 * Verify constraints.
	 * @return true if the constraints are fulfilled, false otherwise.
	 */
	 Boolean verifyConstraints() {

		/*
		 * Constraint 7
		 * the capacity of a vehicle cannot be exceeded:
		 * if load(ti) > capacity(vk) ⇒ vehicle(ti) ≠ vk
		 */		
		for (Vehicle vehicle : vehicles) {
			
			int carriedWeight = 0;
			
			for (Action action: actionsList.get(vehicle)) {
				
				if (action.actionType.equals("pickup")) {
					carriedWeight += action.task.weight;
				} else {
					carriedWeight -= action.task.weight;
				}
	
				if (carriedWeight > vehicle.capacity()) {
					System.out.println("[Info] Constraint 7 not met.");
					return false;
				}
				
			}
			
		}
		
		
		/*
		 * Constraint 8
		 * Pickups actions of a task must be before corresponding deliveries
		 */
		for (Vehicle vehicle : vehicles) {
			
			ArrayList<Task> stack = new ArrayList<Task>();
			
			for (Object obj : actionsList.get(vehicle)) {
				
				Action action = (Action) obj;
				
				if (action.actionType.equals("pickup")) {
					stack.add(action.task);
				} else {
					if (!stack.remove(action.task)) {
						System.out.println("[Info] Constraint 8 " + debug + " - " + action.task + " not met.");
						return false;
					}
				}
				
			}
			
			if (!stack.isEmpty()) return false;
			
		}

		return true;
	}
	 
	@Override
	public String toString() {
		return debug + " : " + cost + " | ";
	}
	 
}

class Action {
	
	protected Task task;
	protected String actionType;
	protected City city;
	
	public Action(Task task, String type) {
		this.task = task;
		actionType = type;
		if (actionType.equals("pickup")) {
			city = this.task.pickupCity;
		} else {
			city = this.task.deliveryCity;
		}
	}
	
	@Override
	public String toString() {
		return actionType + " Task" + task.id + " in " + city;
	}
	
	public boolean equals(Object obj) {
		Action action = (Action) obj;
		return this.task.equals(action.task) && this.actionType.equals(action.actionType);
	}
}
