

//the list of imports
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.behavior.CentralizedBehavior;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
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
	
	private Object chooseNeighbours(){
		return null;
	}
	
	private List<Plan> localChoice(){
		return null;
	}
}
