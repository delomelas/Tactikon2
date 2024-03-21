package uk.co.eidolon.tact2.AISimple;

import java.util.ArrayList;

import uk.co.eidolon.tact2.AISimple.PathFinder.Node;
import Tactikon.State.Position;

public class Task
{
	public int myUnitId;
	public int targetCityId;
	public int targetUnitId;
	public IAIEvaluator evaluator;
	public float score; // score should be independent of turns - ie, the benefit of doing this action
	public int turns; // number of turns until we can consider this action complete
	public boolean actioned = false;
	public boolean finished = false;
	public ArrayList<Position> route;
	public Position targetPosition;
}
