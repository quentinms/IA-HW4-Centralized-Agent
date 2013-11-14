

//the list of imports
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
 * 
 */
@SuppressWarnings("unused")
public class CentralizedAgent implements CentralizedBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);

		Plan planVehicle1 = naivePlan(vehicles.get(0), tasks);

		List<Plan> plans = new ArrayList<Plan>();
		plans.add(planVehicle1);
		while (plans.size() < vehicles.size())
			plans.add(Plan.EMPTY);

		return plans;
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
	
	private List<Plan> centralizedPlan(List<Vehicle> vehicles, TaskSet tasks){
		List<Plan> A = selectInitialSolution(vehicles, tasks);
		List<Plan> A_old = null;
		Object N = null;
		Boolean ok = true;
		
		while(ok){
			A_old = new ArrayList<Plan>(A);
			N = chooseNeighbours();
			A = localChoice();
		}
		
		return A;
	}
	
	private List<Plan> selectInitialSolution(List<Vehicle> vehicles, TaskSet tasks){
		
		Vehicle biggestVehicle = null;
		int maxCapacity = 0;
		for(Vehicle vehicle: vehicles){
			if(vehicle.capacity()>maxCapacity){
				maxCapacity = vehicle.capacity();
				biggestVehicle = vehicle;
			}
		}
		
		return null;
	}
	
	private Object chooseNeighbours(List<Plan> Aold, TaskSet tasks, List<Vehicle> vehicles) {
		
		Vehicle vi = vehicles.get((int) (Math.random() * vehicles.size()));
		Vehicle vj = vehicles.get((int) (Math.random() * vehicles.size()));
		
		if (vi.equals(vj)) {
			// Change the order of any pair of two tasks to be delivered
			// random = (int) (Math.random() * vehicles.size());
			// Task task = vi.getCurrentTasks().get(random);
			// anotherRandom = (int) (Math.random() * vehicles.size());
			// vi.tasks.get(random) = vi.tasks.get(anotherRandom);
			// vi.tasks[anotherRandom] = task;
		} else {
			// Give the first task to be delivered by vi to vj
			// task = nextTask(vi);
			// vj.tasks.push(task);
			// vi.remove(task);
		}
		
		return null;
		
	}
	
	private List<Plan> localChoice(){
		return null;
	}
	
	/**
	 *TODO verify that it is correct
	 * http://i.imgur.com/xVyoSl.jpg
	 * @return True if the constraints are fulfilled, false otherwise.
	 */
	private Boolean verifyConstraints(){
		
		Vehicle vk = null;
		HashMap<Task, Task> nextTaskTask = null;
		HashMap<Vehicle, Task> nextTaskVehicle = null;
		HashMap<Task, Integer> time = null;
		HashMap<Task, Vehicle> vehicleTaskMap = null;
		
		ListIterator<Task> taskIter = null;
		
		
		//Constraint 1
		for(int index = 0; index < nextTaskTask.size(); index++){
			Task currentTask = nextTaskTask.get(index);
			Task nextTask = nextTaskTask.get(index+1);
			
			if(currentTask.equals(nextTask)){
				return false;
			}
			
		}
		
		//Constraint 2
		{
			Task tj = nextTaskVehicle.get(vk);
		
			if(time.get(tj)!=1){
				return false;
			}
		}
		
		//Constraint 3
		for(Task ti: nextTaskTask.keySet()){
			Task tj = nextTaskTask.get(ti);
			
			if(time.get(tj) != time.get(ti)+1){
				return false;
			}
			
		}
		
		//Constraint 4
		{
			Task tj = nextTaskVehicle.get(vk);
			if(!vk.equals(vehicleTaskMap.get(tj))){
				return false;
			}
		}
		
		//Constraint 5
		for(Task ti: nextTaskTask.keySet()){
			Task tj = nextTaskTask.get(ti);
			
			if(!vehicleTaskMap.get(tj).equals(vehicleTaskMap.get(ti))){
				return false;
			}
		}
		
		//Constraint 6
		//WTF
		
		//Constraint 7
		Task ti = null;
		int carriedWeight = 0;
		for (Task t: vk.getCurrentTasks()){
			carriedWeight += t.weight;
		}
		if(ti.weight + carriedWeight > vk.capacity()){
			return false;
		}
		
		
		return true;
	}
}
