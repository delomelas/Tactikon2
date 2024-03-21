package uk.co.eidolon.tact2.AISimple;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import Tactikon.State.IUnit;
import Tactikon.State.TactikonState;

public class TaskList
{
	Map<Integer, Task> taskList = new TreeMap<Integer, Task>(); // map of unitId to task
	
	public Task GetTaskForUnitId(int unitId)
	{
		if (taskList.containsKey(unitId)) return taskList.get(unitId);
		return null;
	}
	
	public void AssignUnitTask(int unitId, Task task)
	{
		taskList.put(unitId, task);
	}
	
	public void ClearTask(int unitId)
	{
		taskList.remove(unitId);
	}
	
	public ArrayList<Task> GetCityTasks(Class type, int cityId)
	{
		ArrayList<Task> tasks = new ArrayList<Task>();
		for (Entry<Integer, Task> entry : taskList.entrySet())
		{
			Task task = entry.getValue();
			if (task.evaluator.getClass() == type && task.targetCityId == cityId) tasks.add(task);
		}
		
		return tasks;
	}
	
	public void ExpireTasks(TactikonState state)
	{
		ArrayList<Task> tasksToDelete = new ArrayList<Task>();
		for (Entry<Integer, Task> entry : taskList.entrySet())
		{
			Task task = entry.getValue();
			IUnit unit = state.GetUnit(task.myUnitId);
			if (unit == null) tasksToDelete.add(task);
			if (task.targetUnitId > 0)
			{
				IUnit targetUnit = state.GetUnit(task.targetUnitId);
				if (targetUnit == null) tasksToDelete.add(task);
			}
			
		}
		
		for (Task task : tasksToDelete)
		{
			taskList.remove(task.myUnitId);
		}
	}
	
	public ArrayList<Task> GetTasksTargettingUnit(int unitId)
	{
		ArrayList<Task> tasks = new ArrayList<Task>();
		for (Entry<Integer, Task> entry : taskList.entrySet())
		{
			Task task = entry.getValue();
			if (task.targetUnitId == unitId) tasks.add(task);
		}
		
		return tasks;
	}
	
	public void StartTurn()
	{
		ArrayList<Task> tasksToDelete = new ArrayList<Task>();
		for (Entry<Integer, Task> entry : taskList.entrySet())
		{
			Task task = entry.getValue();
			task.actioned = false;
			
			if (entry.getValue().evaluator instanceof DefendCity) tasksToDelete.add(entry.getValue());
		}
		for (Task task : tasksToDelete)
		{
			taskList.remove(task.myUnitId);
		}
	}
}
